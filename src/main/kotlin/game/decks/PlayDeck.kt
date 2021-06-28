package game.decks

import game.cards.plays.*
import game.cards.types.BaseCardType
import java.lang.IndexOutOfBoundsException
import kotlin.random.Random

class PlayDeck(val name: String, private val cards: ArrayList<PlayCard>, private var currentCardId: Int) {
    private val baseCards: List<PlayCard> = cards.filter { pc -> pc::class==BasePlayCard::class}

    init {
        cards.removeAll(baseCards)
    }

    fun drawCard():PlayCard{
        return drawMultipleCards(1).first()
    }

    fun drawMultipleCards(nbCards: Int): List<PlayCard>{
        val cardsDrawed: ArrayList<PlayCard> =  ArrayList()
        for (x in 0 until nbCards){
            cardsDrawed.add(cards.removeAt(Random.nextInt(cards.size)))
        }
        return cardsDrawed
    }

    fun drawHand(): List<PlayCard>{
        return drawMultipleCards(Constants.NB_CARDS_HAND)
    }

    fun addCard(card: PlayCard){
        cards.add(card)
    }

    fun getCards():ArrayList<PlayCard>{
        return cards
    }

    fun getBaseCards():List<PlayCard>{
        return baseCards
    }

    fun size():Int{
        return cards.size
    }

    fun nextId():Int{
        return ++currentCardId
    }
}