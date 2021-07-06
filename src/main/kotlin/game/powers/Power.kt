package game.powers

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import game.cards.plays.HeroPlayCard
import game.cards.plays.PlayCard
import game.notifyChangeTurn
import theme.SpyBackgroundColor
import kotlin.reflect.KClass

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

    open fun powerAuthorization(){}

    @Composable
    open fun Button(modifier: Modifier,
        onClick: () -> Unit = {}){
    }
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
    private val distanceStrike= mutableStateOf(false)

    override fun action(owner: HeroPlayCard, target: PlayCard,
                        onAction: () -> Unit): Boolean{
        return if(distanceStrike.value &&
                target != owner){
            target.takeDamage(owner.cardType.attack/2)
            true
        } else false
    }

    @Composable
    override fun Button(modifier: Modifier,
        onClick: () -> Unit) {
        Button(
            modifier = modifier.size(Constants.STATS_BOX_WIDTH.dp),
            onClick = { //this.powerAuthorization()
                onClick() },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.SpyBackgroundColor,
            )
        ) {}
    }

    override fun powerAuthorization() {
        distanceStrike.value = true
    }

    override fun reset() {
        distanceStrike.value = false
    }
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

val powersList= mapOf<Int, KClass<out Power>>(1 to PrecisionStrikePower::class,2 to DistanceStrikePower::class,
                                    3 to HealingPower::class, 4 to DoubleStrikePower::class,
                                    5 to IncinerationPower::class, 6 to WhipStrikePower::class)