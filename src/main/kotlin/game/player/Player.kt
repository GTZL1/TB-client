package game.player

import game.decks.DeckType

class Player(val pseudo: String, val deckType: DeckType) {
    val playDeck =deckType.generatePlayDeck(pseudo)
    //val hand=Hand(playDeck.drawMultipleCards(Constants.NB_CARDS_HAND))

    //var beginsGame=false
}