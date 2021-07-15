package game.cards.plays

import game.cards.types.CardType

open class UnitPlayCard(cardType: CardType, player:String, id: Int): PlayCard(cardType=cardType, owner = player, id = id) {
    open suspend fun attack(target: PlayCard) {
        target.takeDamage(cardType.attack)
        takeDamage(target.cardType.attack)
    }
}