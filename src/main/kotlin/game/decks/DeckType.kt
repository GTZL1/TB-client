package game.decks

import game.cards.plays.PlayCard
import game.cards.types.CardType
import java.util.ArrayList

class DeckType(val name: String, val cardTypes: Map<CardType, Short>) {
    fun generatePlayDeck():PlayDeck{
        val deck= ArrayList<PlayCard>()
        cardTypes.forEach { ct, s ->
            for(x in 0 until s){
                deck.add(ct.playType.constructors.first().call(ct))
            }
        }
        return PlayDeck(name, deck)
    }
}