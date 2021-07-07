package game.cards.plays

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import game.Game
import game.cards.types.HeroCardType

class HeroPlayCard(val heroCardType:  HeroCardType, player:String, id :Int): UnitPlayCard(heroCardType, player, id = id) {
    override fun attack(target: PlayCard) {
        if(!heroCardType.power.action(owner = this, target = target) && this != target) {
            super.attack(target)
        }
    }

    fun attack(target: PlayCard, game: Game) {
        if(!heroCardType.power.action(this, target, game) && this != target) {
            super.attack(target)
        }
    }

    fun regainHealth(amount : Int) {
        this.health.value = (getHealth() + amount).coerceAtMost(heroCardType.life)
    }

    override fun overrideDistanceAttack(): Boolean {
        return heroCardType.power.overrideDistanceAttack()
    }

    @Composable
    override fun CardButton(modifier: Modifier,
        onClick: () -> Unit){
        heroCardType.power.Button(modifier, onClick)
    }
}