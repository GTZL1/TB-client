package game.powers

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ButtonElevation
import androidx.compose.material.IconButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.svgResource
import game.Game
import game.Position
import game.cards.plays.HeroPlayCard
import game.cards.plays.PlayCard
import game.notifyChangeTurn
import theme.SpyBackgroundColor
import kotlin.reflect.KClass

//TODO make Power abstract
open class Power(val id: Int, val name: String) {
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
            onClick = { onClick()
                      },
           content = {
                     Image(modifier = Modifier.rotate(-90f),
                         painter = svgResource("icons/arrows_crossed.svg"),
                        contentDescription = "Arrows crossed")
           },
        )
    }

    override fun powerAuthorization() {
        distanceStrike.value = true
    }

    override fun reset() {
        distanceStrike.value = false
    }
}

class HealingPower: Power(3, "Healing") {
    override fun action(owner: HeroPlayCard,
                        target: PlayCard,
                        game: Game?): Boolean{
        return if(target == owner &&
            owner.getHealth() < owner.cardType.life){
            owner.regainHealth(owner.cardType.life/2)
            game!!.cardsAlreadyActed.add(owner!!.id)
            true
        } else false
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
    private val whipStrike= mutableStateOf(false)

    override fun action(owner: HeroPlayCard,
                        target: PlayCard,
                        game: Game?): Boolean {
        return if(whipStrike.value && target != owner){
            when(owner.getPosition()){
                Position.PLAYER -> game!!.cardToOpponentRow(target)
                Position.OPPONENT -> game!!.cardToPlayerRow(target)
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
            onClick = { onClick()
            },
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
}

val powersList= mapOf<Int, KClass<out Power>>(1 to PrecisionStrikePower::class,2 to DistanceStrikePower::class,
                                    3 to HealingPower::class, 4 to DoubleStrikePower::class,
                                    5 to IncinerationPower::class, 6 to WhipStrikePower::class)