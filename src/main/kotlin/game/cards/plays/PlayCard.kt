package game.cards.plays

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.imageFromResource
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import game.cards.types.CardType
import theme.*

open class PlayCard(val cardType: CardType, var owner: String, val id: Int) {
    private var health = cardType.life
    private var position= Position.DECK

    fun getHealth(): Int {
        return health
    }

    fun takeDamage(damage: Int) {
        health -= damage
    }

    fun getPosition():Position{
        return position
    }

    fun changePosition(newPos: Position){
        position=newPos
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlayCard) return false

        if (cardType != other.cardType) return false
        if (owner != other.owner) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = cardType.hashCode()
        result = 31 * result + owner.hashCode()
        result = 31 * result + id
        return result
    }

    enum class Position {
        DECK, HAND, PLAYER_ROW, CENTRAL_ROW, DISCARD
    }
}