package game.cards.types

import game.cards.plays.HeroPlayCard
import game.powers.Power

class HeroCardType(name: String, life: Int, attack: Int, maxNumberInDeck: Int, image: String, val power: Power):
    UnitCardType(name, life, attack, maxNumberInDeck, image, HeroPlayCard::class){
}