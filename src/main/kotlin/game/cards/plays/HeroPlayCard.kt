package game.cards.plays

import game.cards.types.HeroCardType
import java.lang.Math.min

class HeroPlayCard(val heroCardType:  HeroCardType, player:String, id :Int): UnitPlayCard(heroCardType, player, id = id) {
    override fun attack(target: PlayCard) {
        if(!heroCardType.power.action(this, target)) {
            super.attack(target)
        }
    }

    fun regainHealth(amount : Int) {
        this.health.value = (getHealth() + amount).coerceAtMost(heroCardType.life)
    }
}