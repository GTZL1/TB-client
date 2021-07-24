package game.player

import game.decks.DeckType

open class Player(val pseudo: String, val deckType: DeckType) {
    val playDeck =deckType.generatePlayDeck(pseudo)

    fun nextId(): Int {
        return playDeck.nextId()
    }
}