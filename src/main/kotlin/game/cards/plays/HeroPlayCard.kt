package game.cards.plays

import game.cards.types.HeroCardType

class HeroPlayCard(val heroCardType:  HeroCardType, player:String): UnitPlayCard(heroCardType, player) {
    fun power(){
       heroCardType.power.action()
    }
}