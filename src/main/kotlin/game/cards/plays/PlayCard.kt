package game.cards.plays

import game.cards.types.CardType

abstract class PlayCard(val cardType: CardType, var owner: String, val id: Int) {
    private var health = cardType.life

    fun getHealth(): Int {
        return health
    }

    fun takeDamage(damage: Int) {
        health -= damage
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