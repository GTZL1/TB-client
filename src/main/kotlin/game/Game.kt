package game

import Constants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.*
import game.cards.plays.PlayCard
import game.cards.plays.UnitPlayCard
import game.player.Player
import kotlinx.coroutines.runBlocking
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

/*interface GameInterface {
    fun cardToPlayerRow(card: PlayCard)
    fun cardToCenterRow(card: PlayCard)
    fun cardToDiscard(card: PlayCard)
    fun cardToOpponentRow(card: PlayCard)
    fun registerToPlayerRow(callback: GameCallback)
    fun unregisterToPlayerRow(callback: GameCallback)
    fun registerToCenterRow(callback: GameCallback)
    fun unregisterToCenterRow(callback: GameCallback)
    fun registerToDiscard(callback: GameCallback)
    fun unregisterToDiscard(callback: GameCallback)
    fun registerToOpponentRow(callback: GameCallback)
    fun unregisterToOpponentRow(callback: GameCallback)
}*/

class Game(
    private val date: Date, val webSocketHandler: WebSocketHandler, private val idSession: Int,
    val player: Player, val opponent: Player
)  {
    private val playerRowCallback = mutableListOf<GameCallback>()
    private val toCenterRowCallback = mutableListOf<GameCallback>()
    private val toDiscardCallback = mutableListOf<GameCallback>()
    private val opponentRowCallback = mutableListOf<GameCallback>()
    private val turnCallback= mutableListOf<TurnCallback>()

    private lateinit var oldCard: PlayCard
    private lateinit var oldClicked: MutableState<Boolean>

    var playerTurn= false

    fun cardToPlayerRow(card: PlayCard) {
        playerRowCallback.forEach { it.onNewCard(pc = card) }
    }

    fun cardToCenterRow(card: PlayCard) {
        toCenterRowCallback.forEach { it.onNewCard(pc = card) }
    }

    fun cardToDiscard(card: PlayCard) {
        toDiscardCallback.forEach { it.onNewCard(pc = card) }
    }

    fun cardToOpponentRow(card: PlayCard) {
        opponentRowCallback.forEach { it.onNewCard(pc = card) }
    }

    fun registerToPlayerRow(callback: GameCallback) {
        playerRowCallback.add(callback)
    }

    fun unregisterToPlayerRow(callback: GameCallback) {
        playerRowCallback.remove(callback)
    }

    fun registerToCenterRow(callback: GameCallback) {
        toCenterRowCallback.add(callback)
    }

    fun unregisterToCenterRow(callback: GameCallback) {
        toCenterRowCallback.remove(callback)
    }

    fun registerToDiscard(callback: GameCallback) {
        toDiscardCallback.add(callback)
    }

    fun unregisterToDiscard(callback: GameCallback) {
        toDiscardCallback.remove(callback)
    }

    fun registerToOpponentRow(callback: GameCallback) {
        opponentRowCallback.add(callback)
    }

    fun unregisterToOpponentRow(callback: GameCallback) {
        opponentRowCallback.remove(callback)
    }

    fun registerToTurnChange(callback: TurnCallback) {
        turnCallback.add(callback)
    }

    fun unregisterToTurnChange(callback: TurnCallback) {
        turnCallback.remove(callback)
    }

    internal fun notifyMovement(card: PlayCard, position: Position) {
        webSocketHandler.sendMessage(
            JSONObject(
                CardMovement(
                    owner = card.owner,
                    id = card.id,
                    position = position
                )
            )
        )
    }

    internal fun changeTurn() {
        webSocketHandler.sendMessage(
            JSONObject(SimpleMessage(Constants.CHANGE_TURN))
        )
        playerTurn=!playerTurn
        turnCallback.forEach { it.onChangeTurn() }
    }

    fun determineFirst() {
        val num=UUID.randomUUID().toString()
        webSocketHandler.sendMessage(JSONObject(SimpleMessage(num)))
        val msg =runBlocking { webSocketHandler.receiveOne() }
        playerTurn=(num < msg.getString("type"))
    }

    internal suspend fun receiveMessages() {
        for (msg in webSocketHandler.msgReceived) {
            when(msg.getString("type")){
                Constants.CARD_MOVEMENT -> {
                    applyMovement(msg.getString("owner"), msg.getInt("id"),
                        Position.valueOf(msg.getString("position")))
                }
                Constants.CHANGE_TURN -> {
                    playerTurn=!playerTurn
                    turnCallback.forEach { it.onChangeTurn() }
                }
            }
        }
    }

    private fun applyMovement(owner: String, id: Int, position: Position) {
        when (position) {
            Position.PLAYER -> {
                cardToOpponentRow(
                    (opponent.playDeck.getCards()
                        .first { pc: PlayCard -> pc.id == id }).cardType.generatePlayCard(owner, id)
                )
            }
            Position.CENTER -> {
                cardToCenterRow(
                    (opponent.playDeck.getCards()
                        .first { pc: PlayCard -> pc.id == id }).cardType.generatePlayCard(owner, id)
                )
            }
            Position.OPPONENT -> {
                cardToPlayerRow(
                    (opponent.playDeck.getCards()
                        .first { pc: PlayCard -> pc.id == id }).cardType.generatePlayCard(owner, id)
                )
            }
            else -> {
            }
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
                if (canAttack(card.getPosition(), oldCard.getPosition())){
                    try {
                        (oldCard as UnitPlayCard).attack(card)
                    } catch (t: Throwable) { }
                }
            }
        } else {
            oldCard = card
            oldClicked = clicked
        }
    }

    private fun canAttack(posCard1: Position, posCard2: Position): Boolean {
        return abs(posCard1.ordinal - posCard2.ordinal) <= 1
    }
}

enum class Position {
    DECK, HAND, PLAYER, CENTER, OPPONENT, DISCARD
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