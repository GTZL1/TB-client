package game.cards.types

import androidx.compose.ui.graphics.Color
import game.cards.plays.PlayCard
import kotlin.reflect.KClass


open class UnitCardType constructor(
    name: String, life: Int, attack: Int, maxNumberInDeck: Int,
    playType: KClass<out PlayCard> = PlayCard::class
) : CardType(name, life, attack, maxNumberInDeck, playType) {
    //override val backgroundColor: Color=Color(238f, 189f, 146f, 0.28f)
}