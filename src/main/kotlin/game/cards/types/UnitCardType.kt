package game.cards.types

import game.cards.types.CardType

open class UnitCardType : CardType {
        constructor(name: String, life: Int, attack: Int, maxNumberInDeck: Int): super(name, life, attack, maxNumberInDeck)
}