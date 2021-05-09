package game.cards.types

import game.cards.plays.HeroPlayCard
import game.powers.Power

class HeroCardType(name: String, life: Int, attack: Int, maxNumberInDeck: Int, val power: Power):
    UnitCardType(name, life, attack, maxNumberInDeck, HeroPlayCard::class){
    //override val backgroundColor: Color=Color(255f, 226f, 0f, 0.1f)
}