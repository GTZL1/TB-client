package game.player

import Constants
import game.decks.*

class Player(val pseudo: String, val deckType: DeckType) {
    val playDeck =deckType.generatePlayDeck(pseudo)
    //val hand=Hand(playDeck.drawMultipleCards(Constants.NB_CARDS_HAND))
}