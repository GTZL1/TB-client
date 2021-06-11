package game

import game.cards.plays.PlayCard
import game.player.Player
import io.ktor.client.*
import network.CardMovement
import network.WebSocketHandler
import org.json.JSONObject
import java.util.*

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
    private val opponentRowCallback= mutableListOf<GameCallback>()

    override fun cardToPlayerRow(card: PlayCard) {
        playerRowCallback.forEach { it.onNewCard(pc = card) }
    }

    override fun cardToCenterRow(card: PlayCard) {
        toCenterRowCallback.forEach { it.onNewCard(pc = card) } }

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

    internal fun notifyMovement(card: PlayCard, position: Position){
        webSocketHandler.sendMessage(JSONObject(CardMovement(card.owner, card.id, position)))
    }
}

enum class Position {
    DECK, HAND, PLAYER, CENTRAL, DISCARD
}