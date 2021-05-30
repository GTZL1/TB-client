package game.cards.types

import game.cards.plays.PlayCard
import kotlin.reflect.KClass

abstract class CardType(
    val name: String, val life: Int, val attack: Int, val maxNumberInDeck: Int, val image: String,
    private val playType: KClass<out PlayCard>) {

    fun generatePlayCard(owner: String, id: Int): PlayCard{
        return playType.constructors.first().call(this, owner, id)
    }
}