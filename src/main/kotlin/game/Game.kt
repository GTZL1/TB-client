package game

import Constants
import androidx.compose.runtime.*
import game.cards.plays.*
import game.cards.types.BaseCardType
import game.cards.types.CardType
import game.cards.types.HeroCardType
import game.player.Player
import game.player.PlayerIA
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
import kotlin.random.Random

/**
 * Used to notify turn change to the board
 */
interface TurnCallback {
    fun onChangeTurn()
}

/**
 * Store game state and execute actions to change it
 */
class Game(
    private val date: LocalDateTime,
    private val webSocketHandler: WebSocketHandler,
    private val httpClient: HttpClient,
    private val serverUrl: MutableState<String>,
    private val serverPort: String = Constants.SERVER_PORT,
    private val idSession: MutableState<Int>,
    private val cardTypes: List<CardType>,
    val player: Player,
    val opponent: Player,
    private val playIA: Boolean = false,
    private val onEnding: (String, Boolean) -> Unit
) {
    private val turnCallback = mutableListOf<TurnCallback>()

    private var oldCard: PlayCard? = null
    private lateinit var oldClicked: MutableState<Boolean>
    private var powerAuthorization = mutableStateOf(false)

    internal var playerTurn = false
    private var opponentEnded = false

    val handCards = mutableStateListOf<PlayCard>()
    val playerRowCards = mutableStateListOf<PlayCard>()
    val playerBaseCards = mutableStateListOf<PlayCard>()
    val opponentBaseCards = mutableStateListOf<PlayCard>()
    val centerRowCards = mutableStateListOf<PlayCard>()
    val opponentRowCards = mutableStateListOf<PlayCard>()
    val discardCards = mutableStateListOf<PlayCard>()

    val cardsAlreadyActed = mutableListOf<Int>()

    internal var cardsMovedFromHand = mutableStateOf(0)

    private val playerRowCapacity =
        player.playDeck.getBaseCards().size * Constants.PLAYER_ROW_CAPACITY

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
            if(!playIA){
                notifyNewId(player.pseudo, startId++, newCard.id)
            }
        }
        opponent.playDeck.getBaseCards().forEach { pc: PlayCard ->
            opponentBaseCards.add(pc.cardType.generatePlayCard(pc.owner, pc.id))
            opponentBaseCards.last().changePosition(Position.OPPONENT)
        }
    }

    /**
     * Move a card to player row
     * @card [PlayCard] to move
     * @isSpy if card is a [SpyPlayCard]
     */
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

    /**
     * Move a card to player row and notifies opponent using websocket
     * @card [PlayCard] to move
     * @isSpy if card is a [SpyPlayCard]
     * @position new [Position] of the card
     * @fromDeck if the card comes from the deck or not
     */
    fun cardToPlayerRow(card: PlayCard, isSpy: Boolean = false, position: Position, fromDeck: Boolean = false) {
        cardToPlayerRow(card, isSpy)
        if(!playIA) {
            notifyMovement(card, position, fromDeck)
        }
        checkChangeTurn()
    }

    /**
     * Move a card to center row
     * @card [PlayCard] to move
     */
    fun cardToCenterRow(card: PlayCard) {
        centerRowCards.add(card)
        playerRowCards.remove(card)
        if (handCards.remove(card) || ((card.owner == opponent.pseudo) && card.getPosition()==Position.HAND)) {
            cardsMovedFromHand.value += 1
        }
        opponentRowCards.remove(card)
        card.changePosition(Position.CENTER)
        cardsAlreadyActed.add(card.id)
    }

    /**
     * Move a card to center row and notifies opponent using websocket
     * @card [PlayCard] to move
     * @position new [Position] of the card
     * @fromDeck if the card comes from the deck or not
     */
    fun cardToCenterRow(card: PlayCard, position: Position, fromDeck: Boolean = false) {
        cardToCenterRow(card)
        if(!playIA) { notifyMovement(card, position, fromDeck) }
        checkChangeTurn()
    }

    /**
     * Move a card to opponent row
     * @card [PlayCard] to move
     */
    fun cardToOpponentRow(card: PlayCard) {
        opponentRowCards.add(card)
        centerRowCards.remove(card)
        //second condition used only when fighting IA
        if (handCards.remove(card) || ((card.owner == opponent.pseudo) && card.getPosition()==Position.HAND)) {
            cardsMovedFromHand.value += 1
        }
        card.changePosition(Position.OPPONENT)
        if(playIA) {
            cardsAlreadyActed.add(card.id)
        }
    }

    /**
     * Move a card to center row and notifies opponent using websocket
     * @card [PlayCard] to move
     * @position new [Position] of the card
     * @fromDeck if the card comes from the deck or not
     */
    private fun cardToOpponentRow(card: PlayCard, position: Position, fromDeck: Boolean = false) {
        cardToOpponentRow(card)
        if(!playIA) notifyMovement(card, position, fromDeck)
        checkChangeTurn()
    }

    /**
     * Move a card to discard pile
     * @card [PlayCard] to move
     */
    private fun cardToDiscard(card: PlayCard) {
        discardCards.add(card)
        playerRowCards.remove(card)
        centerRowCards.remove(card)
        opponentRowCards.remove(card)
        if(opponentBaseCards.remove(card)){
            //draw 2 cards when an opponent base is destroyed
            drawCards(Constants.NEW_CARDS_BASE_DESTROYED)
        } else if(playerBaseCards.remove(card) && playIA) {
            (opponent as PlayerIA).drawCards(Constants.NEW_CARDS_BASE_DESTROYED)
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

    /**
     * Notify opponent of a movement
     * @card [PlayCard] to move
     * @position [Position] to move card at
     * @fromDeck if the card is played from deck or not
     */
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
    }

    /**
     * Notify opponent of an attack
     * @attackerCard [PlayCard] attacker
     * @target [PlayCard] targeted
     * @specialPower if @attackerCard uses her special power or not
     */
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
    }

    /**
     * Notify opponent of a card id change
     * @owner owner of the card
     * @oldId old id of the card (used to select it)
     * @newId new id to set to the card
     */
    private fun notifyNewId(owner: String, oldId: Int, newId: Int) {
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

    /**
     * Notify opponent that player has no card remaining in his hand or on the board
     */
    private fun notifyEndGame() {
        webSocketHandler.sendMessage(
            JSONObject(SimpleMessage(Constants.ENDGAME))
        )
    }

    internal fun endPlayerTurn() {
        if (playerTurn) {
            if(!playIA) {
                webSocketHandler.sendMessage(
                    JSONObject(SimpleMessage(Constants.CHANGE_TURN))
                )
            }
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
                println(t.message)
            }

            if (this::delayJob.isInitialized) {
                delayJob.cancel()
            }
            turnCallback.forEach { it.onChangeTurn() }
        }
    }

    internal fun startPlayerTurn() {
        playerTurn = !playerTurn
        cardsAlreadyActed.clear()
        cardsMovedFromHand.value = 0
        checkChangeTurn()
        checkTimerTurn()

        turnCallback.forEach { it.onChangeTurn() }
    }

    suspend fun determineFirst() {
        playerTurn = if(!playIA){
            val num = UUID.randomUUID().toString()
            webSocketHandler.sendMessage(JSONObject(SimpleMessage(num)))
            val msg = webSocketHandler.receiveOne()
            (num < msg.getString("type"))
        } else {
            Random.nextBoolean()
        }

        initialization()

        checkTimerTurn()
    }

    private fun checkChangeTurn() {
        delay.value = Constants.MOVEMENT_DELAY
        if (playerTurn && ((cardsMovedFromHand.value == (player.playDeck.getBaseCards().size)) || handCards.isEmpty())
            && (cardsAlreadyActed.size == (filterCardsOwner(player.pseudo).size - playerBaseCards.size))
        ) {
            endPlayerTurn()
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
                endPlayerTurn()
            }
        }
    }

    /**
     * Receive messages from opponent and perform right actions
     */
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
                    startPlayerTurn()
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
                        opponent.playDeck.currentCardId=msg.getInt("newId")
                    }
                }
                Constants.ENDGAME -> {
                    opponentEnded = true
                }
            }
        }
    }

    /**
     * Apply an opponent's card movement on the board
     * @owner owner of the [PlayCard] to move
     * @id id of the [PlayCard] to move
     * @cardTypeName card type name of card to create (used only if @fromDeck==true)
     * @position [Position] to move card at
     * @fromDeck if card is played from deck. If true, a new PlayCard is created, if not the card is selected then moved
     */
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

    /**
     * Apply an attack between. Used for local and opponent's attacks
     * @attackerOwner owner of the attacker [PlayCard]
     * @attackerId id of the attacker [PlayCard]
     * @targetOwner owner of the targeted [PlayCard]
     * @targetId id of the targeted [PlayCard]
     * @specialPower if attacker card uses her special power
     */
    internal fun applyAttack(
        attackerOwner: String,
        attackerId: Int,
        targetOwner: String,
        targetId: Int,
        specialPower: Boolean = false
    ) {
        cardsAlreadyActed.add(attackerId)
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

    /**
     * Examine card clicked by player and apply attack if possible
     * @clicked mutable boolean useful to the method
     * @card most recently clicked [PlayCard]
     * @specialPower if a card can use her special power while attacking
     */
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
                if (canAttack(oldCard!!, card) || (powerAuthorization.value && oldCard!!.overrideDistanceAttack() && cardCanAct(oldCard!!))) {
                    try {
                        applyAttack(
                            attackerOwner = oldCard!!.owner,
                            attackerId = oldCard!!.id,
                            targetOwner = card.owner,
                            targetId = card.id,
                            specialPower = powerAuthorization.value
                        )
                        if(!playIA) { notifyAttack(oldCard!!, card, powerAuthorization.value) }
                        powerAuthorization.value = false
                        checkChangeTurn()
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

    internal fun cardBelongsToOwner(card: PlayCard, owner: String = player.pseudo): Boolean {
        return card.owner == owner
    }

    internal fun movableToCenterRow(): Boolean {
        return centerRowCards.size < Constants.CENTER_ROW_CAPACITY
    }

    internal fun movableToPlayerRow(): Boolean {
        return playerRowCards.size < playerRowCapacity
    }

    private fun movableToOpponentRow(): Boolean {
        return opponentRowCards.size < opponent.playDeck.getBaseCards().size * Constants.PLAYER_ROW_CAPACITY
    }

    internal fun movableFromHand(destinationRow: Position): Boolean {
        return when(destinationRow){
            Position.PLAYER -> (cardsMovedFromHand.value < player.playDeck.getBaseCards().size)
                    && movableToPlayerRow()
            Position.CENTER -> (cardsMovedFromHand.value < player.playDeck.getBaseCards().size)
                    && movableToCenterRow()
            Position.OPPONENT -> (cardsMovedFromHand.value < opponent.playDeck.getBaseCards().size)
                    && movableToOpponentRow()
            else -> false
        }
    }

    private fun canAttack(attackerCard: PlayCard, targetCard: PlayCard): Boolean {
        return cardCanAct(attackerCard)
                && abs(attackerCard.getPosition().ordinal - targetCard.getPosition().ordinal) <= 1
    }

    private fun filterCardsOwner(owner: String): List<PlayCard> {
        return listOf(centerRowCards, playerRowCards, opponentRowCards, playerBaseCards, opponentBaseCards).flatten()
            .filter { playCard: PlayCard ->
                cardBelongsToOwner(playCard, owner)
            }.toList()
    }

    private fun drawCards(nbCards: Int) {
        player.playDeck.drawMultipleCards(nbCards).forEach { pc: PlayCard ->
            handCards.add(pc.cardType.generatePlayCard(pc.owner, pc.id))
            handCards.last().changePosition(Position.HAND)
        }
    }

    /**
     * Check if game is ended and if true, send the result
     * @defeat if player has lost anyway (true only if window is closed during game)
     */
    private fun checkEnding(defeat: Boolean = false) {
        if (playerBaseCards.isEmpty() ||
            opponentBaseCards.isEmpty() ||
            (handCards.isEmpty() && opponentEnded)
            || defeat) {
            val victory= if(defeat) {
                            false
                        } else if (handCards.isEmpty() && opponentEnded){
                            playerBaseCards.sumOf { selector -> selector.getHealth() } > opponentBaseCards.sumOf { selector -> selector.getHealth() }
                        }
                        else {
                            (!playerBaseCards.isEmpty())
                        }
            stopGame()
            try {
                runBlocking{
                    httpClient.request<String> {
                        url("http://"+serverUrl.value + ":" + serverPort + "/game")
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
        } else if (playerRowCards.isEmpty() && centerRowCards.isEmpty() && opponentRowCards.isEmpty() && handCards.isEmpty()) {
            notifyEndGame()
        }
    }

    fun sendDefeat(){
        checkEnding(true)
    }

    fun stopGame() {
        delayJob.cancel()
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