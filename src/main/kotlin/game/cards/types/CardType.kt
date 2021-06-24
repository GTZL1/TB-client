package game.cards.types

import game.cards.plays.PlayCard
import kotlin.reflect.KClass

abstract class CardType(
    val name: String, val life: Int, val attack: Int, val maxNumberInDeck: Int, val image: String,
    private val playType: KClass<out PlayCard>) {

    fun generatePlayCard(owner: String, id: Int): PlayCard{
        return playType.constructors.first().call(this, owner, id)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CardType) return false

        if (name != other.name) return false
        if (life != other.life) return false
        if (attack != other.attack) return false
        if (maxNumberInDeck != other.maxNumberInDeck) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + life
        result = 31 * result + attack
        result = 31 * result + maxNumberInDeck
        return result
    }
}