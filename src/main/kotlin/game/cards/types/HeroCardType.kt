package game.cards.types

import game.cards.plays.HeroPlayCard
import game.powers.Power

class HeroCardType(name: String, life: Int, attack: Int, maxNumberInDeck: Int, val power: Power):
    UnitCardType(name, life, attack, maxNumberInDeck, HeroPlayCard::class){
}