package game.cards.plays

import game.Position
import game.cards.types.*

abstract class PlayCard(val cardType: CardType, var owner: String, val id: Int) {
    private var health = cardType.life
    private var position=Position.DECK

    fun getHealth(): Int {
        return health
    }

    fun takeDamage(damage: Int) {
        health -= damage
    }

    fun getPosition():Position{
        return position
    }

    fun changePosition(position: Position){
        this.position=position
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
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