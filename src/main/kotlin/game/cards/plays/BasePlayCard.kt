package game.cards.plays

import game.cards.types.CardType

class BasePlayCard(cardType: CardType, owner:String, id:Int): PlayCard(cardType, owner, id) {
}