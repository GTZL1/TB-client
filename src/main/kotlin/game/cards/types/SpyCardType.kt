package game.cards.types

import game.cards.plays.SpyPlayCard

class SpyCardType: UnitCardType {
    constructor(name: String, life: Int, attack: Int, maxNumberInDeck: Int):
            super(name, life, attack, maxNumberInDeck, SpyPlayCard::class)
}