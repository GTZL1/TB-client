package game.cards.plays

import game.cards.types.CardType

open class UnitPlayCard(cardType: CardType, player:String): PlayCard(cardType=cardType, owner = player) {
    fun move() {

    }

    fun attack(target: PlayCard) {
        target.takeDamage(cardType.attack)
        takeDamage(target.cardType.attack)
    }
}