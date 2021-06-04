package game

import game.cards.plays.PlayCard
import game.player.Player
import io.ktor.client.*
import java.util.*

interface GameCallback {
    fun onNewCard(pc: PlayCard)
}

interface GameInterface {
    fun cardToPlayerRow(card: PlayCard)
    fun cardToCenterRow(card: PlayCard)
    fun cardQuitCenterRow(card: PlayCard)
    fun registerToPlayerRow(callback: GameCallback)
    fun unregisterToPlayerRow(callback: GameCallback)
    fun registerToCenterRow(callback: GameCallback)
    fun unregisterToCenterRow(callback: GameCallback)
    fun registerQuitCenterRow(callback: GameCallback)
    fun unregisterQuitCenterRow(callback: GameCallback)
}

class Game(
    val date: Date, val httpClient: HttpClient, private val idSession: Int,
    val player: Player, val opponent: Player
) : GameInterface {
    private val playerRowCallback = mutableListOf<GameCallback>()
    private val toCenterRowCallback = mutableListOf<GameCallback>()
    private val quitCenterRowCallback = mutableListOf<GameCallback>()

    override fun cardToPlayerRow(card: PlayCard) {
        playerRowCallback.forEach { it.onNewCard(pc = card) }
    }

    override fun cardToCenterRow(card: PlayCard) {
        toCenterRowCallback.forEach { it.onNewCard(pc = card) } }

    override fun cardQuitCenterRow(card: PlayCard) {
        quitCenterRowCallback.forEach { it.onNewCard(pc = card) }
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
        toCenterRowCallback.add(callback)
    }

    override fun registerQuitCenterRow(callback: GameCallback) {
        quitCenterRowCallback.add(callback)
    }

    override fun unregisterQuitCenterRow(callback: GameCallback) {
        quitCenterRowCallback.add(callback)
    }
}
