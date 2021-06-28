package game.cards.types

import game.cards.plays.BasePlayCard

class BaseCardType(name: String, life: Int, attack: Int, maxNumberInDeck: Int,) :
    CardType(name, life, attack, maxNumberInDeck, BasePlayCard::class) {
}