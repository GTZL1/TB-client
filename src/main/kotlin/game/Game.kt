package game

import Constants
import androidx.compose.runtime.MutableState
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
import kotlin.random.Random.Default.nextInt

interface GameCallback {
    fun onNewCard(pc: PlayCard)
}

interface GameInterface {
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
}

class Game(
    val date: Date, val webSocketHandler: WebSocketHandler, private val idSession: Int,
    val player: Player, val opponent: Player
) : GameInterface {
    private val playerRowCallback = mutableListOf<GameCallback>()
    private val toCenterRowCallback = mutableListOf<GameCallback>()
    private val toDiscardCallback = mutableListOf<GameCallback>()
    private val opponentRowCallback = mutableListOf<GameCallback>()

    private lateinit var oldCard: PlayCard
    private lateinit var oldClicked: MutableState<Boolean>

    fun determineFirst() {
        var num:Int
        do {
            num= abs(nextInt()%2)
            webSocketHandler.sendMessage(JSONObject(SimpleMessage(num)))
            val msg =runBlocking { webSocketHandler.receiveOne() }
        } while (msg.getInt("type") == num)
        player.beginsGame=((num) > 0)
        println(player.beginsGame)
    }

    override fun cardToPlayerRow(card: PlayCard) {
        playerRowCallback.forEach { it.onNewCard(pc = card) }
    }

    override fun cardToCenterRow(card: PlayCard) {
        toCenterRowCallback.forEach { it.onNewCard(pc = card) }
    }

    override fun cardToDiscard(card: PlayCard) {
        toDiscardCallback.forEach { it.onNewCard(pc = card) }
    }

    override fun cardToOpponentRow(card: PlayCard) {
        opponentRowCallback.forEach { it.onNewCard(pc = card) }
    }

    override fun registerToPlayerRow(callback: GameCallback) {
        playerRowCallback.add(callback)
    }

    override fun unregisterToPlayerRow(callback: GameCallback) {
        playerRowCallback.remove(callback)
    }

    override fun registerToCenterRow(callback: GameCallback) {
        toCenterRowCallback.add(callback)
    }

    override fun unregisterToCenterRow(callback: GameCallback) {
        toCenterRowCallback.remove(callback)
    }

    override fun registerToDiscard(callback: GameCallback) {
        toDiscardCallback.add(callback)
    }

    override fun unregisterToDiscard(callback: GameCallback) {
        toDiscardCallback.remove(callback)
    }

    override fun registerToOpponentRow(callback: GameCallback) {
        opponentRowCallback.add(callback)
    }

    override fun unregisterToOpponentRow(callback: GameCallback) {
        opponentRowCallback.remove(callback)
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

    internal suspend fun receiveMessages(
        handleMovement: (Game, String, Int, Position) -> Unit,
        game: Game
    ) {
        for (msg in webSocketHandler.msgReceived) {
            if (msg.getString("type").equals(Constants.CARD_MOVEMENT)) {
                handleMovement(
                    game, msg.getString("owner"), msg.getInt("id"),
                    Position.valueOf(msg.getString("position"))
                )
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