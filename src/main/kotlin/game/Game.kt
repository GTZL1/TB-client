package game

import Constants
import androidx.compose.runtime.*
import game.cards.plays.PlayCard
import game.cards.plays.UnitPlayCard
import game.player.Player
import kotlinx.coroutines.*
import network.CardAttack
import network.CardMovement
import network.SimpleMessage
import network.WebSocketHandler
import org.json.JSONObject
import java.util.*
import kotlin.math.abs

interface GameCallback {
    fun onNewCard(pc: PlayCard)
}

interface TurnCallback {
    fun onChangeTurn()
}

class Game(
    private val date: Date, private val webSocketHandler: WebSocketHandler, private val idSession: Int,
    val player: Player, val opponent: Player
)  {
    private val turnCallback= mutableListOf<TurnCallback>()

    private lateinit var oldCard: PlayCard
    private lateinit var oldClicked: MutableState<Boolean>

    internal var playerTurn= false

    val handCards= mutableStateListOf<PlayCard>()
    val playerRowCards = mutableStateListOf<PlayCard>()
    val baseCards = mutableStateListOf<PlayCard>()
    val centerRowCards = mutableStateListOf<PlayCard>()
    val opponentRowCards = mutableStateListOf<PlayCard>()
    val discardCards = mutableStateListOf<PlayCard>()

    private val cardsAlreadyActed= mutableListOf<Int>()

    internal var cardsMovedFromHand= mutableStateOf(0)

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
        //println(card.cardType.name+" "+card.getHealth())
        playerRowCards.add(card)
        if(!isSpy && handCards.remove(card)){
            cardsMovedFromHand.value += 1
        }
        centerRowCards.remove(card)
        card.changePosition(Position.PLAYER)
        cardsAlreadyActed.add(card.id)
    }

    fun cardToCenterRow(card: PlayCard) {
        centerRowCards.add(card)
        playerRowCards.remove(card)
        if(handCards.remove(card)){
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
    }

    fun cardToOpponentRow(card: PlayCard) {
        opponentRowCards.add(card)
        centerRowCards.remove(card)
        if(handCards.remove(card)){
            cardsMovedFromHand.value += 1
        }
        card.changePosition(Position.OPPONENT)
    }

    fun cardCanAct(card: PlayCard):Boolean {
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

    private fun notifyAttack(attackerCard: PlayCard, targetCard: PlayCard) {
        webSocketHandler.sendMessage(
            JSONObject(
                CardAttack(
                    attackerOwner = attackerCard.owner,
                    attackerId = attackerCard.id,
                    targetOwner = targetCard.owner,
                    targetId = targetCard.id
                )
            )
        )
        checkChangeTurn()
    }

    internal fun changeTurn() {
        if(playerTurn){
            webSocketHandler.sendMessage(
                JSONObject(SimpleMessage(Constants.CHANGE_TURN))
            )
            playerTurn=!playerTurn
            cardsAlreadyActed.clear()
            cardsMovedFromHand.value=0

            if(this::delayJob.isInitialized) {
                runBlocking { delayJob.cancel()}
            }

            turnCallback.forEach { it.onChangeTurn() }
        }
    }

    suspend fun determineFirst() {
        val num=UUID.randomUUID().toString()
        webSocketHandler.sendMessage(JSONObject(SimpleMessage(num)))
        val msg =webSocketHandler.receiveOne()
        playerTurn=(num < msg.getString("type"))

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
            delay.value= Constants.MOVEMENT_DELAY
            while(delay.value > 0 && playerTurn) {
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
            when(msg.getString("type")){
                Constants.CARD_MOVEMENT -> {
                    applyMovement(msg.getString("owner"), msg.getInt("id"),
                        Position.valueOf(msg.getString("position")),
                        msg.getBoolean("fromDeck"))
                }
                Constants.CHANGE_TURN -> {
                    playerTurn=!playerTurn
                    cardsAlreadyActed.clear()
                    cardsMovedFromHand.value=0
                    checkChangeTurn()
                    checkTimerTurn()
                    turnCallback.forEach { it.onChangeTurn() }
                }
                Constants.CARD_ATTACK -> {
                    applyAttack(attackerOwner = msg.getString("attackerOwner"),
                                attackerId = msg.getInt("attackerId"),
                                targetOwner = msg.getString("targetOwner"),
                                targetId = msg.getInt("targetId"))
                }
            }
        }
    }

    private fun applyMovement(owner: String, id: Int, position: Position, fromDeck: Boolean) {
        val card= if(fromDeck) {
                (opponent.playDeck.getCards().first { pc: PlayCard -> pc.id == id }).cardType.generatePlayCard(owner, id)
        } else {
            filterCardsOwner(owner).first { playCard -> playCard.id==id }
        }

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

    private fun applyAttack(attackerOwner: String, attackerId: Int, targetOwner: String, targetId: Int) {
        val attacker = filterCardsOwner(attackerOwner).first { playCard -> playCard.id==attackerId }
        val target = filterCardsOwner(targetOwner).first { playCard -> playCard.id==targetId }
        (attacker as UnitPlayCard).attack(target)
        if(attacker.getHealth()<=0){
            cardToDiscard(attacker)
        }
        if(target.getHealth()<=0){
            cardToDiscard(target)
        }
    }

    internal fun handleClick(clicked: MutableState<Boolean>, card: PlayCard) {
        clicked.value = true
        if ((this::oldCard.isInitialized) && (card!=oldCard)) {
            oldClicked.value=false
            if (card.owner == oldCard.owner) {
                oldCard = card
                oldClicked = clicked
            } else if ((oldCard.owner == player.pseudo)
            ) {
                clicked.value = false
                //attacker is oldCard
                if (canAttack(oldCard, card)){
                    try {
                        cardsAlreadyActed.add(oldCard.id)
                        applyAttack(attackerOwner = oldCard.owner, attackerId = oldCard.id,
                                    targetOwner = card.owner, targetId = card.id)
                        notifyAttack(oldCard, card)
                    } catch (t: Throwable) { }
                }
            }
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
            playCard.owner==owner
        }.toList()
    }
}

enum class Position {
    DECK, HAND, PLAYER, CENTER, OPPONENT, DISCARD, SPY
}

@Composable
fun notifyChangeTurn(game: Game): Boolean {
    var state by remember { mutableStateOf(game.playerTurn) }
    DisposableEffect(game) {
        val callback = object : TurnCallback{
            override fun onChangeTurn() {
                state=game.playerTurn
            }
        }
        game.registerToTurnChange(callback)
        onDispose { game.unregisterToTurnChange(callback) }
    }
    return state
}