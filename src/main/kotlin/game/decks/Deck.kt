package game.decks

import game.cards.plays.*
import kotlin.random.Random

class Deck( val name: String, private val cards: ArrayList<PlayCard>) {
    fun drawCard():PlayCard{
        return drawMultipleCards(1).first()
    }

    fun drawMultipleCards(nbCards: Int): List<PlayCard>{
        var cardsDrawed: ArrayList<PlayCard> =  ArrayList()
        for (x in 0..nbCards){
            val i= Random.nextInt(cards.size)
            cardsDrawed.add(cards.removeAt(i))
        }
        return cardsDrawed
    }

    fun size():Int{
        return cards.size
    }
}