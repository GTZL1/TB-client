package game.cards.types

import androidx.compose.ui.graphics.Color
import game.cards.plays.PlayCard
import kotlin.reflect.KClass


open class UnitCardType constructor(
    name: String, life: Int, attack: Int, maxNumberInDeck: Int,
    playType: KClass<out PlayCard> = PlayCard::class
) : CardType(name, life, attack, maxNumberInDeck, playType) {
}