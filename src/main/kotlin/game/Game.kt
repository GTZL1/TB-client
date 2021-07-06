package game

import Constants
import androidx.compose.runtime.*
import game.cards.plays.HeroPlayCard
import game.cards.plays.PlayCard
import game.cards.plays.UnitPlayCard
import game.cards.types.BaseCardType
import game.cards.types.HeroCardType
import game.player.Player
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.*
import network.CardAttack
import network.CardMovement
import network.SimpleMessage
import network.WebSocketHandler
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
    val player: Player,
    val opponent: Player,
    private val onEnding: (String, Boolean) -> Unit
) {
    private val turnCallback = mutableListOf<TurnCallback>()

    private var oldCard: PlayCard? = null
    private lateinit var oldClicked: MutableState<Boolean>

    internal var playerTurn = false

    val handCards = mutableStateListOf<PlayCard>()
    val playerRowCards = mutableStateListOf<PlayCard>()
    val baseCards = mutableStateListOf<PlayCard>()
    val centerRowCards = mutableStateListOf<PlayCard>()
    val opponentRowCards = mutableStateListOf<PlayCard>()
    val discardCards = mutableStateListOf<PlayCard>()

    private val cardsAlreadyActed = mutableListOf<Int>()

    internal var cardsMovedFromHand = mutableStateOf(0)

    internal var delay = mutableStateOf(Constants.MOVEMENT_DELAY)
    lateinit var delayJob: Job

    init {
        player.playDeck.drawHand().forEach { pc: PlayCard ->
            handCards.add(pc.cardType.generatePlayCard(pc.owner, pc.id))
            handCards.last().changePosition(Position.HAND)
        }
        player.playDeck.getBaseCards().forEach { pc: PlayCard ->
            baseCards.add(pc.cardType.generatePlayCard(pc.owner, pc.id))
            baseCards.last().changePosition(Position.PLAYER)
        }
        opponent.playDeck.getBaseCards().forEach { pc: PlayCard ->
            opponentRowCards.add(pc.cardType.generatePlayCard(pc.owner, pc.id))
            opponentRowCards.last().changePosition(Position.OPPONENT)
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

    fun cardToDiscard(card: PlayCard) {
        discardCards.add(card)
        playerRowCards.remove(card)
        centerRowCards.remove(card)
        opponentRowCards.remove(card)
        baseCards.remove(card)

        cardsAlreadyActed.remove(card.id) //necessary to the auto turn change
        checkEnding()
    }

    fun cardToOpponentRow(card: PlayCard) {
        opponentRowCards.add(card)
        centerRowCards.remove(card)
        if (handCards.remove(card)) {
            cardsMovedFromHand.value += 1
        }
        card.changePosition(Position.OPPONENT)
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

    internal fun notifyMovement(card: PlayCard, position: Position, fromDeck: Boolean = false) {
        webSocketHandler.sendMessage(
            JSONObject(
                CardMovement(
                    owner = card.owner,
                    id = card.id,
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
                //double strike heroes
                filterCardsOwner(player.pseudo).filter { playCard: PlayCard ->
                    playCard.cardType::class == HeroCardType::class
                }.forEach { playCard: PlayCard -> (playCard as HeroPlayCard).heroCardType.power.reset() }
            } catch (t: Throwable){
            }

            if (this::delayJob.isInitialized) {
                runBlocking { delayJob.cancel() }
            }

            turnCallback.forEach { it.onChangeTurn() }
        }
    }

    suspend fun determineFirst() {
        val num = UUID.randomUUID().toString()
        webSocketHandler.sendMessage(JSONObject(SimpleMessage(num)))
        val msg = webSocketHandler.receiveOne()
        playerTurn = (num < msg.getString("type"))

        checkTimerTurn()
    }

    private fun checkChangeTurn() {
        delay.value = Constants.MOVEMENT_DELAY
        if (playerTurn && ((cardsMovedFromHand.value == (player.playDeck.getBaseCards().size)) || handCards.isEmpty())
            && (cardsAlreadyActed.size == (filterCardsOwner(player.pseudo).size - baseCards.size))
        ) {
            changeTurn()
        }
    }

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
                        msg.getString("owner"), msg.getInt("id"),
                        Position.valueOf(msg.getString("position")),
                        msg.getBoolean("fromDeck")
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
            }
        }
    }

    private fun applyMovement(owner: String, id: Int, position: Position, fromDeck: Boolean) {
        val card = if (fromDeck) {
            (opponent.playDeck.getCards()
                .first { pc: PlayCard -> pc.id == id }).cardType.generatePlayCard(owner, id)
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
                //heroes
                if(specialPower) (attacker as HeroPlayCard).heroCardType.power.powerAuthorization()
                (attacker as HeroPlayCard).attack(target) { cardsAlreadyActed.remove(attackerId) }
            } catch (t: Throwable){
                (attacker as UnitPlayCard).attack(target)
            }
        } else {
            //healing power
            (attacker as HeroPlayCard).attack(attacker)
        }

        if (attacker.getHealth() <= 0) {
            cardToDiscard(attacker)
        }
        if (target.getHealth() <= 0) {
            cardToDiscard(target)
        }
    }

    internal fun handleClick(clicked: MutableState<Boolean>, card: PlayCard, specialPower: Boolean = false) {
        clicked.value = true
        if ((oldCard!=null) && (card != oldCard)) {
            oldClicked.value = false
            if (card.owner == oldCard!!.owner ||
                    oldCard!!.owner != player.pseudo) {
                oldCard = card
                oldClicked = clicked
            } else if ((oldCard!!.owner == player.pseudo)
            ) {
                clicked.value = false
                //attacker is oldCard
                if (canAttack(oldCard!!, card)) {
                    try {
                        cardsAlreadyActed.add(oldCard!!.id)
                        applyAttack(
                            attackerOwner = oldCard!!.owner, attackerId = oldCard!!.id,
                            targetOwner = card.owner, targetId = card.id
                        )
                        notifyAttack(oldCard!!, card, specialPower)
                    } catch (t: Throwable) {
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
                                                                    onAction = {cardsAlreadyActed.add(oldCard!!.id)})
                notifyAttack(card, card)
            } catch (t :Throwable) {}
        } else {
            oldCard = card
            oldClicked = clicked
        }
    }

    private fun canAttack(attackerCard: PlayCard, targetCard: PlayCard): Boolean {
        return cardCanAct(attackerCard)
                && abs(attackerCard.getPosition().ordinal - targetCard.getPosition().ordinal) <= 1
    }

    private fun filterCardsOwner(owner: String): List<PlayCard> {
        return listOf(centerRowCards, playerRowCards, opponentRowCards, baseCards).flatten()
            .filter { playCard: PlayCard ->
                playCard.owner == owner
            }.toList()
    }

    private fun checkEnding() {
        if (baseCards.isEmpty() ||
            filterCardsOwner(opponent.pseudo).filter { playCard: PlayCard ->
                playCard.cardType::class == BaseCardType::class
            }.isEmpty()
        ) {
            val victory= !baseCards.isEmpty()
            try {
                runBlocking {
                    httpClient.request<String> {
                        url(System.getenv("TB_SERVER_URL")+":"+System.getenv("TB_SERVER_PORT")+"/game")
                        headers {
                            append("Content-Type", "application/json")
                        }
                        body = JSONObject(GameIssue(
                            idSession = idSession.value,
                            date = date.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                            winner = if(victory) player.pseudo else opponent.pseudo,
                            looser = if(victory) opponent.pseudo else player.pseudo
                        ))
                        method = HttpMethod.Post
                    }
                }
            } catch (exception: ClientRequestException) {
            }
            onEnding(opponent.pseudo, victory)
        }
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