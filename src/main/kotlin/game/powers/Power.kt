package game.powers

import androidx.compose.runtime.mutableStateOf
import game.Game
import game.Position
import game.cards.plays.HeroPlayCard
import game.cards.plays.PlayCard
import kotlin.reflect.KClass

abstract class Power(val id: Int, val name: String) {
    open val buttonIcon: String? = null
    open fun action(owner: HeroPlayCard,
                    target: PlayCard,
                    game: Game? = null): Boolean{
        return false
    }

    open fun action(cards: List<PlayCard>,
                    onAction: (PlayCard) -> Unit): Boolean{
        return false
    }

    open fun reset() {}

    open fun powerAuthorization(){}

    open fun overrideDistanceAttack(): Boolean {
        return false
    }
}

class PrecisionStrikePower: Power(1, "PrecisionStrike") {
    override fun action(owner: HeroPlayCard,
                        target: PlayCard,
                        game: Game?): Boolean {
        return if(target != owner &&
                target.getHealth() <= (owner.cardType.life/2)){
            target.takeDamage(owner.cardType.attack)
            true
        } else false
    }
}

class DistanceStrikePower: Power(2, "DistanceStrike") {
    override val buttonIcon = "distance strike.svg"
    private val distanceStrike= mutableStateOf(false)

    override fun action(owner: HeroPlayCard,
                        target: PlayCard,
                        game: Game?): Boolean{
        return if(distanceStrike.value &&
                target != owner){
            target.takeDamage(owner.cardType.attack/2)
            true
        } else false
    }

    override fun powerAuthorization() {
        distanceStrike.value = true
    }

    override fun reset() {
        distanceStrike.value = false
    }

    override fun overrideDistanceAttack(): Boolean {
        return true
    }
}

class HealingPower: Power(3, "Healing") {
    override val buttonIcon = "healing.svg"
    override fun action(owner: HeroPlayCard,
                        target: PlayCard,
                        game: Game?): Boolean{
        return if(target == owner &&
            owner.getHealth() < owner.cardType.life){
            owner.regainHealth(owner.cardType.life/2)
            game!!.cardsAlreadyActed.add(owner.id)
            true
        } else false
    }

}

class DoubleStrikePower: Power(4, "DoubleStrike") {
    override val buttonIcon = "double strike.svg"
    private val doubleStrike= mutableStateOf(true)
    override fun action(owner: HeroPlayCard,
                        target: PlayCard,
                        game: Game?): Boolean{
        return if(target != owner && doubleStrike.value){
            doubleStrike.value = false
            owner.attack(target)
            game!!.cardsAlreadyActed.remove(owner.id)
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
    override val buttonIcon = "incineration.svg"
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
    override val buttonIcon = "whip strike.svg"
    private val whipStrike= mutableStateOf(false)

    override fun action(owner: HeroPlayCard,
                        target: PlayCard,
                        game: Game?): Boolean {
        return if(whipStrike.value && target != owner){
            when(owner.getPosition()){
                Position.PLAYER -> {
                    when(target.getPosition()){
                        Position.OPPONENT -> game!!.cardToCenterRow(target)
                        Position.CENTER -> game!!.cardToOpponentRow(target)
                        else -> {}
                    }
                }
                Position.OPPONENT -> {
                    when(target.getPosition()){
                        Position.PLAYER -> game!!.cardToCenterRow(target)
                        Position.CENTER -> game!!.cardToPlayerRow(target)
                        else -> {}
                    }
                }
                Position.CENTER -> {
                    when(target.getPosition()){
                        Position.OPPONENT -> game!!.cardToCenterRow(target)
                        Position.PLAYER -> game!!.cardToCenterRow(target)
                        Position.CENTER -> if(target.owner == game!!.player.pseudo) {
                                                game.cardToPlayerRow(target)
                                            } else {
                                                game.cardToOpponentRow(target)
                                            }
                        else -> {}
                    }
                }
                else -> {}
            }
            true
        } else false
    }

    override fun powerAuthorization() {
        whipStrike.value = true
    }

    override fun reset() {
        whipStrike.value = false
    }

    override fun overrideDistanceAttack(): Boolean {
        return true
    }
}

val powersList= mapOf<Int, KClass<out Power>>(1 to PrecisionStrikePower::class,2 to DistanceStrikePower::class,
                                    3 to HealingPower::class, 4 to DoubleStrikePower::class,
                                    5 to IncinerationPower::class, 6 to WhipStrikePower::class)