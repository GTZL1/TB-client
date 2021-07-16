package game.cards.plays

import androidx.compose.runtime.mutableStateOf
import game.Position
import game.cards.types.CardType

abstract class PlayCard(val cardType: CardType, var owner: String, var id: Int) {
    protected val health = mutableStateOf(cardType.life)
    private var position=Position.DECK
    open val buttonIconSvg: String? = null

    fun getHealth(): Int {
        return health.value
    }

    fun takeDamage(damage: Int) {
        health.value -= damage
    }

    fun getPosition():Position{
        return position
    }

    fun changePosition(position: Position){
        this.position=position
    }

    fun changeId(newId: Int){
        id=newId
    }

    open fun overrideDistanceAttack(): Boolean {
        return false
    }

    override fun equals(other: Any?): Boolean {
        if (other !is PlayCard) return false

        if (cardType != other.cardType) return false
        if (owner != other.owner) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = cardType.hashCode()
        result = 31 * result + owner.hashCode()
        result = 31 * result + id
        return result
    }
}