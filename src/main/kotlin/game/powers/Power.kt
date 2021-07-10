package game.powers

import Constants
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.svgResource
import androidx.compose.ui.unit.dp
import game.Game
import game.Position
import game.cards.plays.HeroPlayCard
import game.cards.plays.PlayCard
import kotlin.reflect.KClass

abstract class Power(val id: Int, val name: String) {
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

    @Composable
    open fun Button(modifier: Modifier,
        onClick: () -> Unit = {}){
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
    private val distanceStrike= mutableStateOf(false)

    override fun action(owner: HeroPlayCard,
                        target: PlayCard,
                        game: Game?): Boolean{
        println("Power: ${distanceStrike.value}")
        return if(distanceStrike.value &&
                target != owner){
            target.takeDamage(owner.cardType.attack/2)
            true
        } else false
    }

    @Composable
    override fun Button(modifier: Modifier,
        onClick: () -> Unit) {
        IconButton(
            modifier = modifier.padding(0.dp).size(Constants.STATS_BOX_WIDTH.dp),
            onClick = { onClick() },
           content = {
                     Image(modifier = Modifier.rotate(-90f),
                         painter = svgResource("icons/distance strike.svg"),
                        contentDescription = "Bow and arrow")
           },
        )
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

    @Composable
    override fun Button(modifier: Modifier,
                        onClick: () -> Unit) {
        IconButton(
            modifier = modifier.padding(0.dp).size(Constants.STATS_BOX_WIDTH.dp),
            onClick = {},
            content = {
                Image(painter = svgResource("icons/healing.svg"),
                    contentDescription = "Syringe")
            },
        )
    }
}

class DoubleStrikePower: Power(4, "DoubleStrike") {
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

    @Composable
    override fun Button(modifier: Modifier,
                        onClick: () -> Unit) {
        IconButton(
            modifier = modifier.padding(0.dp).size(Constants.STATS_BOX_WIDTH.dp),
            onClick = { onClick() },
            content = {
                Image(painter = svgResource("icons/double strike.svg"),
                    contentDescription = "Two arrows")
            },
        )
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

    @Composable
    override fun Button(modifier: Modifier,
                        onClick: () -> Unit) {
        IconButton(
            modifier = modifier.padding(0.dp).size(Constants.STATS_BOX_WIDTH.dp),
            onClick = { },
            content = {
                Image(painter = svgResource("icons/incineration.svg"),
                    contentDescription = "Vevey's flame")
            },
        )
    }
}

class WhipStrikePower: Power(6, "Whipstrike") {
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

    @Composable
    override fun Button(modifier: Modifier,
                        onClick: () -> Unit) {
        IconButton(
            modifier = modifier.padding(0.dp).size(Constants.STATS_BOX_WIDTH.dp),
            onClick = { onClick() },
            content = {
                Image(bitmap = imageResource("icons/curly-arrow.png"),
                    contentDescription = "Arrows crossed")
            },
        )
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