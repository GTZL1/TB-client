package game.cards.types

class VehicleCardType: UnitCardType {
    constructor(name: String, life: Int, attack: Int, maxNumberInDeck: Int): super(name = name,
        life = life, attack = attack, maxNumberInDeck=maxNumberInDeck){
    }
}