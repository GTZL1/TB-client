package game.cards.types

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key.Companion.R
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import game.cards.plays.PlayCard
import kotlin.reflect.KClass

abstract class CardType(
    val name: String, val life: Int, val attack: Int, val maxNumberInDeck: Int,
    val playType: KClass<out PlayCard>) {

    fun createPlayCard(){
        //return PlayCard
    }
}