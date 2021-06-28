package game.cards.types

import game.cards.plays.VehiclePlayCard

class VehicleCardType(name: String, life: Int, attack: Int, maxNumberInDeck: Int,) :
    UnitCardType(
        name = name,
        life = life, attack = attack, maxNumberInDeck = maxNumberInDeck,
        playType = VehiclePlayCard::class
    ) {
}