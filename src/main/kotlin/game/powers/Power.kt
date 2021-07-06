package game.powers

import androidx.compose.runtime.mutableStateOf
import game.cards.plays.HeroPlayCard
import game.cards.plays.PlayCard

//TODO make Power abstract
open class Power(val id: Int, val name: String) {
    open fun action(owner: HeroPlayCard,
                    target: PlayCard,
                    onAction: () -> Unit = {}): Boolean{
        return false
    }

    open fun action(cards: List<PlayCard>,
                    onAction: (PlayCard) -> Unit): Boolean{
        return false
    }

    open fun reset() {}
}

class PrecisionStrikePower: Power(1, "PrecisionStrike") {
    override fun action(owner: HeroPlayCard, target: PlayCard,
                        onAction: () -> Unit): Boolean{
        return if(target != owner &&
            target.getHealth() <= (owner.cardType.life/2)){
                target.takeDamage(owner.cardType.attack)
                true
        } else false
    }
}

class DistanceStrikePower: Power(2, "DistanceStrike") {
}

class HealingPower: Power(3, "Healing") {
    override fun action(owner: HeroPlayCard, target: PlayCard,
                        onAction: () -> Unit): Boolean{
        return if(target == owner &&
            owner.getHealth() < owner.cardType.life){
            owner.regainHealth(owner.cardType.life/2)
            onAction()
            true
        } else false
    }
}

class DoubleStrikePower: Power(4, "DoubleStrike") {
    private val doubleStrike= mutableStateOf(true)
    override fun action(owner: HeroPlayCard, target: PlayCard,
                        onAction: () -> Unit): Boolean{
        return if(target != owner && doubleStrike.value){
            doubleStrike.value = false
            owner.attack(target)
            onAction()
            true
        } else {
            false
        }
    }

    override fun reset() {
        doubleStrike.value = true
    }
}

class IncinerationPower: Power(5, "Incineration") {
    override fun action(cards: List<PlayCard>,
                        onAction: (PlayCard) -> Unit): Boolean {
        if(cards.isNotEmpty()){
            val max = cards.maxByOrNull { playCard: PlayCard -> playCard.cardType.attack }!!.cardType.attack
            cards.forEach { playCard: PlayCard ->
                if(playCard.cardType.attack==max) onAction(playCard)
            }
        }
        return false
    }
}

class WhipStrikePower: Power(6, "Whipstrike") {
}

val powersList= mapOf<Int, Power>(1 to PrecisionStrikePower(),2 to DistanceStrikePower(),
                                    3 to HealingPower(), 4 to DoubleStrikePower(),
                                    5 to IncinerationPower(), 6 to WhipStrikePower())