package game.decks

import game.cards.plays.*
import java.lang.IndexOutOfBoundsException
import kotlin.random.Random

class PlayDeck(val name: String, private val cards: ArrayList<PlayCard>, private var currentCardId: Int) {
    fun drawCard():PlayCard{
        return drawMultipleCards(1).first()
    }

    fun drawMultipleCards(nbCards: Int): List<PlayCard>{
        val cardsDrawed: ArrayList<PlayCard> =  ArrayList()
        for (x in 0 until nbCards){
            cardsDrawed.add(cards.removeAt(Random.nextInt(cards.size)))
        }
        cardsDrawed.forEach{
            it.changePosition(PlayCard.Position.HAND)
        }
        return cardsDrawed
    }

    fun getCards():ArrayList<PlayCard>{
        return cards
    }

    fun size():Int{
        return cards.size
    }

    fun nextId():Int{
        return ++currentCardId
    }
}