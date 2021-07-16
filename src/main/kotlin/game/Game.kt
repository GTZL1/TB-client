package game

import Constants
import androidx.compose.runtime.*
import game.cards.plays.*
import game.cards.types.BaseCardType
import game.cards.types.CardType
import game.cards.types.HeroCardType
import game.player.Player
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.*
import network.*
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.abs

interface TurnCallback {
    fun onChangeTurn()
}

class Game(
    private val date: LocalDateTime,
    private val webSocketHandler: WebSocketHandler,
    private val httpClient: HttpClient,
    private val idSession: MutableState<Int>,
    private val cardTypes: List<CardType>,
    val player: Player,
    val opponent: Player,
    private val onEnding: (String, Boolean) -> Unit
) {
    private val turnCallback = mutableListOf<TurnCallback>()

    private var oldCard: PlayCard? = null
    private lateinit var oldClicked: MutableState<Boolean>
    private var powerAuthorization = mutableStateOf(false)

    internal var playerTurn = false

    val handCards = mutableStateListOf<PlayCard>()
    val playerRowCards = mutableStateListOf<PlayCard>()
    val playerBaseCards = mutableStateListOf<PlayCard>()
    val opponentBaseCards = mutableStateListOf<PlayCard>()
    val centerRowCards = mutableStateListOf<PlayCard>()
    val opponentRowCards = mutableStateListOf<PlayCard>()
    val discardCards = mutableStateListOf<PlayCard>()

    val cardsAlreadyActed = mutableListOf<Int>()

    internal var cardsMovedFromHand = mutableStateOf(0)

    internal var delay = mutableStateOf(Constants.MOVEMENT_DELAY)
    lateinit var delayJob: Job

    private fun initialization() {
        player.playDeck.drawHand().forEach { pc: PlayCard ->
            handCards.add(pc.cardType.generatePlayCard(pc.owner, pc.id))
            handCards.last().changePosition(Position.HAND)
        }
        var startId=0
        player.playDeck.getBaseCards().forEach { pc: PlayCard ->
            val newCard = pc.cardType.generatePlayCard(pc.owner, pc.id)
            playerBaseCards.add(newCard)
            playerBaseCards.last().changePosition(Position.PLAYER)
            newCard.changeId(player.nextId())
            notifyNewId(player.pseudo, startId++, newCard.id)
        }
        opponent.playDeck.getBaseCards().forEach { pc: PlayCard ->
            opponentBaseCards.add(pc.cardType.generatePlayCard(pc.owner, pc.id))
            opponentBaseCards.last().changePosition(Position.OPPONENT)
        }
    }

    fun cardToPlayerRow(card: PlayCard, isSpy: Boolean = false) {
        playerRowCards.add(card)
        if (!isSpy && handCards.remove(card)) {
            cardsMovedFromHand.value += 1
            tryIncinerationPower(card, opponent.pseudo)
        }
        centerRowCards.remove(card)
        card.changePosition(Position.PLAYER)
        cardsAlreadyActed.add(card.id)
    }

    fun cardToPlayerRow(card: PlayCard, isSpy: Boolean = false, position: Position, fromDeck: Boolean = false) {
        cardToPlayerRow(card, isSpy)
        notifyMovement(card, position, fromDeck)
    }

    fun cardToCenterRow(card: PlayCard) {
        centerRowCards.add(card)
        playerRowCards.remove(card)
        if (handCards.remove(card)) {
            cardsMovedFromHand.value += 1
        }
        opponentRowCards.remove(card)
        card.changePosition(Position.CENTER)
        cardsAlreadyActed.add(card.id)
    }

    fun cardToCenterRow(card: PlayCard, position: Position, fromDeck: Boolean = false) {
        cardToCenterRow(card)
        notifyMovement(card, position, fromDeck)
    }

    fun cardToOpponentRow(card: PlayCard) {
        opponentRowCards.add(card)
        centerRowCards.remove(card)
        if (handCards.remove(card)) {
            cardsMovedFromHand.value += 1
        }
        card.changePosition(Position.OPPONENT)
    }

    fun cardToOpponentRow(card: PlayCard, position: Position, fromDeck: Boolean = false) {
        cardToOpponentRow(card)
        notifyMovement(card, position, fromDeck)
    }

    private fun cardToDiscard(card: PlayCard) {
        discardCards.add(card)
        playerRowCards.remove(card)
        centerRowCards.remove(card)
        opponentRowCards.remove(card)
        playerBaseCards.remove(card)
        if(opponentBaseCards.remove(card)){
            //draw 2 cards when an opponent base is destroyed
            drawCards(Constants.NEW_CARDS_BASE_DESTROYED)
        }

        if(card.owner==player.pseudo) cardsAlreadyActed.remove(card.id) //necessary to the auto turn change
        checkEnding()
    }

    internal fun playSpyCard(card: SpyPlayCard) {
        card.changeOwner(opponent.pseudo)
        card.changeId(opponent.nextId())

        drawCards(Constants.NEW_CARDS_SPY)

        cardToOpponentRow(card = card,
            position = Position.SPY,
            fromDeck = true)
    }

    fun cardCanAct(card: PlayCard): Boolean {
        return !cardsAlreadyActed.contains(card.id)
    }

    fun registerToTurnChange(callback: TurnCallback) {
        turnCallback.add(callback)
    }

    fun unregisterToTurnChange(callback: TurnCallback) {
        turnCallback.remove(callback)
    }

    private fun notifyMovement(card: PlayCard, position: Position, fromDeck: Boolean = false) {
        webSocketHandler.sendMessage(
            JSONObject(
                CardMovement(
                    owner = card.owner,
                    id = card.id,
                    cardTypeName = card.cardType.name,
                    position = position,
                    fromDeck = fromDeck
                )
            )
        )
        checkChangeTurn()
    }

    private fun notifyAttack(attackerCard: PlayCard, targetCard: PlayCard, specialPower: Boolean = false) {
        webSocketHandler.sendMessage(
            JSONObject(
                CardAttack(
                    attackerOwner = attackerCard.owner,
                    attackerId = attackerCard.id,
                    targetOwner = targetCard.owner,
                    targetId = targetCard.id,
                    specialPower = specialPower
                )
            )
        )
        checkChangeTurn()
    }

    internal fun notifyNewId(owner: String, oldId: Int, newId: Int) {
        webSocketHandler.sendMessage(
            JSONObject(
                CardIdChange(
                    owner = owner,
                    oldId = oldId,
                    newId = newId
                )
            )
        )
    }

    internal fun changeTurn() {
        if (playerTurn) {
            webSocketHandler.sendMessage(
                JSONObject(SimpleMessage(Constants.CHANGE_TURN))
            )
            playerTurn = !playerTurn
            cardsAlreadyActed.clear()
            cardsMovedFromHand.value = 0
            oldCard=null

            try {
                //reset hero powers
                listOf(filterCardsOwner(player.pseudo), filterCardsOwner(opponent.pseudo))
                    .flatten().filter { playCard: PlayCard ->
                    playCard.cardType::class == HeroCardType::class
                }.forEach { playCard: PlayCard -> (playCard as HeroPlayCard).heroCardType.power.reset() }
            } catch (t: Throwable){
            }

            if (this::delayJob.isInitialized) {
                delayJob.cancel()
            }

            turnCallback.forEach { it.onChangeTurn() }
        }
    }

    suspend fun determineFirst() {
        val num = UUID.randomUUID().toString()
        webSocketHandler.sendMessage(JSONObject(SimpleMessage(num)))
        val msg = webSocketHandler.receiveOne()
        playerTurn = (num < msg.getString("type"))

        initialization()

        checkTimerTurn()
    }

    private fun checkChangeTurn() {
        delay.value = Constants.MOVEMENT_DELAY
        if (playerTurn && ((cardsMovedFromHand.value == (player.playDeck.getBaseCards().size)) || handCards.isEmpty())
            && (cardsAlreadyActed.size == (filterCardsOwner(player.pseudo).size - playerBaseCards.size))
        ) {
            changeTurn()
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun checkTimerTurn() {
        delayJob = GlobalScope.launch {
            delay.value = Constants.MOVEMENT_DELAY
            while (delay.value > 0 && playerTurn) {
                delay(1000)
                delay.value -= 1000
            }

            if (playerTurn) {
                changeTurn()
            }
        }
    }

    internal suspend fun receiveMessages() {
        for (msg in webSocketHandler.msgReceived) {
            when (msg.getString("type")) {
                Constants.CARD_MOVEMENT -> {
                    applyMovement(
                        owner = msg.getString("owner"),
                        id = msg.getInt("id"),
                        cardTypeName = msg.getString("cardTypeName"),
                        position = Position.valueOf(msg.getString("position")),
                        fromDeck = msg.getBoolean("fromDeck")
                    )
                }
                Constants.CHANGE_TURN -> {
                    playerTurn = !playerTurn
                    cardsAlreadyActed.clear()
                    cardsMovedFromHand.value = 0
                    checkChangeTurn()
                    checkTimerTurn()
                    turnCallback.forEach { it.onChangeTurn() }
                }
                Constants.CARD_ATTACK -> {
                    applyAttack(
                        attackerOwner = msg.getString("attackerOwner"),
                        attackerId = msg.getInt("attackerId"),
                        targetOwner = msg.getString("targetOwner"),
                        targetId = msg.getInt("targetId"),
                        specialPower = msg.getBoolean("specialPower")
                    )
                }
                Constants.NEW_ID_MESSAGE -> {
                    if(opponent.pseudo == msg.getString("owner")){
                        filterCardsOwner(opponent.pseudo).first { playCard ->
                            playCard.id == msg.getInt("oldId") }.changeId(msg.getInt("newId"))
                    }
                }
            }
        }
    }

    private fun applyMovement(owner: String, id: Int, cardTypeName: String, position: Position, fromDeck: Boolean) {
        val card = if (fromDeck) {
            cardTypes.first { cardType -> cardType.name == cardTypeName }.generatePlayCard(owner, id)
        } else {
            filterCardsOwner(owner).first { playCard -> playCard.id == id }
        }
        //a fromDeck card is always owned by opponent
        if(fromDeck) tryIncinerationPower(card, player.pseudo)

        when (position) {
            Position.PLAYER -> {
                cardToOpponentRow(card)
            }
            Position.CENTER -> {
                cardToCenterRow(card)
            }
            Position.OPPONENT -> {
                cardToPlayerRow(card)
            }
            Position.SPY -> {
                cardToPlayerRow(card)
            }
            else -> {
            }
        }
    }

    private fun applyAttack(
        attackerOwner: String,
        attackerId: Int,
        targetOwner: String,
        targetId: Int,
        specialPower: Boolean = false
    ) {
        val attacker =
            filterCardsOwner(attackerOwner).first { playCard -> playCard.id == attackerId }
        val target = filterCardsOwner(targetOwner).first { playCard -> playCard.id == targetId }
        if(attacker != target){
            try {
                //boolean for distance strike and whip strike powers
                if(specialPower) (attacker as HeroPlayCard).heroCardType.power.powerAuthorization()
                //each hero has only one overload really implemented
                (attacker as HeroPlayCard).attack(target, this)

            } catch (t: Throwable){
                (attacker as UnitPlayCard).attack(target)
            }
        } else {
            //healing power
            (attacker as HeroPlayCard).attack(attacker, this)
        }

        if (attacker.getHealth() <= 0) {
            cardToDiscard(attacker)
        }
        if (target.getHealth() <= 0) {
            cardToDiscard(target)
        }
    }

    internal fun handleClick(clicked: MutableState<Boolean>, card: PlayCard, specialPower: Boolean = false) {
        if(specialPower) powerAuthorization.value=true
        clicked.value = true
        if ((oldCard!=null) && (card != oldCard)) {
            oldClicked.value = false
            //ignore if first card belongs to opponent of if player click on 2 of his cards
            if (card.owner == oldCard!!.owner ||
                    oldCard!!.owner != player.pseudo) {
                oldCard = card
                oldClicked = clicked
            } else if ((oldCard!!.owner == player.pseudo)
            ) {
                clicked.value = false
                //attacker is oldCard
                if (canAttack(oldCard!!, card) || (oldCard!!.overrideDistanceAttack() && cardCanAct(oldCard!!))) {
                    try {
                        cardsAlreadyActed.add(oldCard!!.id)
                        applyAttack(
                            attackerOwner = oldCard!!.owner,
                            attackerId = oldCard!!.id,
                            targetOwner = card.owner,
                            targetId = card.id,
                            specialPower = powerAuthorization.value
                        )
                        notifyAttack(oldCard!!, card, powerAuthorization.value)
                        powerAuthorization.value = false
                    } catch (t: Throwable) {
                        println(t.message)
                    }
                }
            }
        } else if ((oldCard!=null) && (card == oldCard)
            && cardCanAct(card = oldCard!!)) {
                oldClicked.value=false
            clicked.value=false
            try {
                //healing power of heroes
                (oldCard as HeroPlayCard).heroCardType.power.action(owner = oldCard as HeroPlayCard,
                                                                    target = oldCard as HeroPlayCard,
                                                                    game = this)
                notifyAttack(card, card)
            } catch (t :Throwable) {}
        } else {
            oldCard = card
            oldClicked = clicked
        }
    }

    internal fun actionableCards(owner: String): List<String> {
        val cards= mutableListOf<String>()
        filterCardsOwner(owner).filter { playCard -> cardCanAct(playCard)
                && playCard.cardType::class != BaseCardType::class}
            .forEach { playCard -> cards.add(playCard.cardType.name) }
        return cards
    }

    private fun canAttack(attackerCard: PlayCard, targetCard: PlayCard): Boolean {
        return cardCanAct(attackerCard)
                && abs(attackerCard.getPosition().ordinal - targetCard.getPosition().ordinal) <= 1
    }

    private fun filterCardsOwner(owner: String): List<PlayCard> {
        return listOf(centerRowCards, playerRowCards, opponentRowCards, playerBaseCards, opponentBaseCards).flatten()
            .filter { playCard: PlayCard ->
                playCard.owner == owner
            }.toList()
    }

    private fun drawCards(nbCards: Int) {
        player.playDeck.drawMultipleCards(nbCards).forEach { pc: PlayCard ->
            handCards.add(pc.cardType.generatePlayCard(pc.owner, pc.id))
            handCards.last().changePosition(Position.HAND)
        }
    }

    private fun checkEnding(defeat: Boolean = false) {
        if (playerBaseCards.isEmpty() ||
            filterCardsOwner(opponent.pseudo).filter { playCard: PlayCard ->
                playCard.cardType::class == BaseCardType::class
            }.isEmpty() ||
            defeat) {
            val victory= if(defeat) false else (!playerBaseCards.isEmpty())
            try {
                runBlocking{
                    httpClient.request<String> {
                        url(System.getenv("TB_SERVER_URL") + ":" + System.getenv("TB_SERVER_PORT") + "/game")
                        headers {
                            append("Content-Type", "application/json")
                        }
                        body = JSONObject(
                            GameIssue(
                                idSession = idSession.value,
                                date = date.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                                winner = if (victory) player.pseudo else opponent.pseudo,
                                looser = if (victory) opponent.pseudo else player.pseudo
                            )
                        )
                        method = HttpMethod.Post
                    }
                }
            } catch (exception: ClientRequestException) {
                println(exception.message)
            }
            onEnding(opponent.pseudo, victory)
        }
    }

    fun sendDefeat(){
        checkEnding(true)
    }

    private fun tryIncinerationPower(card: PlayCard, targetOwner: String) {
        //incineration hero power
        try {
            (card as HeroPlayCard).heroCardType.power.action(
                cards = filterCardsOwner(targetOwner).filter { playCard: PlayCard ->
                    playCard.cardType::class != BaseCardType::class &&
                    playCard.cardType::class != HeroCardType::class
                },
                onAction = {playCard: PlayCard -> cardToDiscard(playCard) }
            )
        } catch (t: Throwable) {}
    }
}

enum class Position {
    DECK, HAND, PLAYER, CENTER, OPPONENT, DISCARD, SPY
}

@Composable
fun notifyChangeTurn(game: Game): Boolean {
    var state by remember { mutableStateOf(game.playerTurn) }
    DisposableEffect(game) {
        val callback = object : TurnCallback {
            override fun onChangeTurn() {
                state = game.playerTurn
            }
        }
        game.registerToTurnChange(callback)
        onDispose { game.unregisterToTurnChange(callback) }
    }
    return state
}

data class GameIssue(
    val idSession: Int,
    val date : String,
    val winner: String,
    val looser: String
)