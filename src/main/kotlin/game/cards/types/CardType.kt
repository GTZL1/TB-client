package game.cards.types

import game.cards.plays.PlayCard
import kotlin.reflect.KClass

abstract class CardType(
    val name: String, val life: Int, val attack: Int, val maxNumberInDeck: Int, val image: String,
    val playType: KClass<out PlayCard>) {

    fun createPlayCard(){
        //return PlayCard
    }
}