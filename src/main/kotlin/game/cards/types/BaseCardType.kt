package game.cards.types

import game.cards.plays.PlayCard

class BaseCardType(name: String, life: Int, attack: Int, maxNumberInDeck: Int, image: String) :
    CardType(name, life, attack, maxNumberInDeck, image, PlayCard::class) {
}