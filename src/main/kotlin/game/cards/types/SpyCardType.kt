package game.cards.types

import game.cards.plays.SpyPlayCard

class SpyCardType: UnitCardType {
    constructor(name: String, life: Int, attack: Int, maxNumberInDeck: Int, image: String):
            super(name, life, attack, maxNumberInDeck, image, SpyPlayCard::class)
}