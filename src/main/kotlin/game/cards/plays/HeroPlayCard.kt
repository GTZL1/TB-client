package game.cards.plays

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import game.cards.types.HeroCardType

class HeroPlayCard(val heroCardType:  HeroCardType, player:String, id :Int): UnitPlayCard(heroCardType, player, id = id) {
    override fun attack(target: PlayCard) {
        if(!heroCardType.power.action(this, target) && this != target) {
            super.attack(target)
        }
    }

    fun attack(target: PlayCard, onAction: () -> Unit) {
        if(!heroCardType.power.action(this, target, onAction) && this != target) {
            super.attack(target)
        }
    }

    fun regainHealth(amount : Int) {
        this.health.value = (getHealth() + amount).coerceAtMost(heroCardType.life)
    }

    @Composable
    override fun CardButton(modifier: Modifier,
        onClick: () -> Unit){
        heroCardType.power.Button(modifier, onClick)
    }
}