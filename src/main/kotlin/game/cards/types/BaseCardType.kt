package game.cards.types

import game.cards.plays.PlayCard

class BaseCardType(name: String, life: Int, attack: Int, maxNumberInDeck: Int) :
    CardType(name, life, attack, maxNumberInDeck, PlayCard::class) {
}