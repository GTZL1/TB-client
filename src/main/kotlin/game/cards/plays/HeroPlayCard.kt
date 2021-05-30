package game.cards.plays

import game.cards.types.HeroCardType

class HeroPlayCard(val heroCardType:  HeroCardType, player:String, id :Int): UnitPlayCard(heroCardType, player, id = id) {
    fun power(){
       heroCardType.power.action()
    }
}