package game.cards.types

class VehicleCardType: UnitCardType {
    constructor(name: String, life: Int, attack: Int, maxNumberInDeck: Int, image: String): super(name = name,
        life = life, attack = attack, maxNumberInDeck=maxNumberInDeck, image = image){
    }
}