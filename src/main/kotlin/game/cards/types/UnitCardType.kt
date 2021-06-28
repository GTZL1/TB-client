package game.cards.types

import game.cards.plays.*
import kotlin.reflect.KClass


open class UnitCardType constructor(
    name: String, life: Int, attack: Int, maxNumberInDeck: Int,
    playType: KClass<out PlayCard> = UnitPlayCard::class
) : CardType(name, life, attack, maxNumberInDeck, playType) {
}