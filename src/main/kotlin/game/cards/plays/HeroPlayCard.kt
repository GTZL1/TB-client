package game.cards.plays

import game.cards.types.HeroCardType

class HeroPlayCard(val heroCardType:  HeroCardType): PlayCard(heroCardType) {
    fun power(){
       heroCardType.power.action()
    }
}