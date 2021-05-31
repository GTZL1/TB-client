package game.decks

import Constants
import game.cards.plays.PlayCard

class Hand(cards: List<PlayCard>) {
    private val cards= arrayOfNulls<PlayCard>(Constants.NB_CARDS_HAND)

    init {
        for(x in 0 until Constants.NB_CARDS_HAND){
            this.cards[x]= cards[x]
        }
    }

    fun getAllCards():List<PlayCard>{
        return cards.filterNotNull()
    }

    fun putCardOnBoard(card: PlayCard): PlayCard{
        println("Removing "+card.cardType.name)
        val index=cards.filterNotNull().indexOf(card)
        val cardDrawn=cards.filterNotNull().get(index)
        cards[cards.filterNotNull().indexOf(card)]=null
        return cardDrawn
    }
}