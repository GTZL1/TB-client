package game.cards.types

import androidx.compose.ui.graphics.Color
import game.cards.plays.PlayCard

class BaseCardType(name: String, life: Int, attack: Int, maxNumberInDeck: Int) :
    CardType(name, life, attack, maxNumberInDeck, PlayCard::class) {
    //override val backgroundColor: Color= Color(0.675f, 0.481f, 0.34f, 1f)
}