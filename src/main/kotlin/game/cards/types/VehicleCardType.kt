package game.cards.types

import androidx.compose.ui.graphics.Color

class VehicleCardType: UnitCardType {
    constructor(name: String, life: Int, attack: Int, maxNumberInDeck: Int): super(name = name,
        life = life, attack = attack, maxNumberInDeck=maxNumberInDeck){
    }
    //override val backgroundColor: Color=Color(73f, 226f, 0f, 0.1f)
}