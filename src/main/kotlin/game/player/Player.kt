package game.player

import Constants
import game.decks.*

class Player(val pseudo: String, val deck: Deck) {
    val hand=Hand(deck.drawMultipleCards(Constants().NB_CARDS_HAND))
}