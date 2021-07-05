package game.powers

import game.cards.plays.HeroPlayCard
import game.cards.plays.PlayCard

//TODO make Power abstract
open class Power(val id: Int, val name: String) {
    open fun action(owner: HeroPlayCard, target: PlayCard): Boolean{
        return false
    }
}

class PrecisionStrikePower: Power(1, "PrecisionStrike") {
    override fun action(owner: HeroPlayCard, target: PlayCard): Boolean{
        return if(target != owner &&
            target.cardType.life<= (owner.cardType.life/2)){
            target.takeDamage(owner.cardType.attack)
            true
        } else false
    }
}

class DistanceStrikePower: Power(2, "DistanceStrike") {
}

class HealingPower: Power(3, "Healing") {
    override fun action(owner: HeroPlayCard, target: PlayCard): Boolean{
        return if(target == owner &&
            owner.getHealth() < owner.cardType.life){
            owner.regainHealth(owner.cardType.life/2)
            true
        } else false
    }
}

class DoubleStrikePower: Power(4, "DoubleStrike") {
}

class IncinerationPower: Power(5, "Incineration") {
}

class WhipStrikePower: Power(6, "Whipstrike") {
}

val powersList= mapOf<Int, Power>(1 to PrecisionStrikePower(),2 to DistanceStrikePower(),
                                    3 to HealingPower(), 4 to DoubleStrikePower(),
                                    5 to IncinerationPower(), 6 to WhipStrikePower())