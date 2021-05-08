package game.cards.types

import game.cards.plays.PlayCard
import kotlin.reflect.KClass

abstract class CardType(
    val name: String, val life: Int, val attack: Int, val maxNumberInDeck: Int,
    val playType: KClass<out PlayCard>) {
    fun createPlayCard(){
        //return P
    }
}