package game.cards.types

import game.cards.plays.VehiclePlayCard

class VehicleCardType(name: String, life: Int, attack: Int, maxNumberInDeck: Int, image: String) :
    UnitCardType(
        name = name,
        life = life, attack = attack, maxNumberInDeck = maxNumberInDeck, image = image,
        playType = VehiclePlayCard::class
    ) {
}