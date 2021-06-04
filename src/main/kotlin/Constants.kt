import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.ui.unit.dp

object Constants {
    const val NB_CARDS_HAND = 6

    const val CONNECTION_INIT_MESSAGE = "Hello there"

    const val ROW_HEIGHT = 180
    const val CARD_HEIGHT = ROW_HEIGHT
    const val CARD_WIDTH = 100
    const val CARD_DRAG_MARGIN = (CARD_HEIGHT * 0.35).toInt()
    private const val MAX_CARDS_ON_ROW = NB_CARDS_HAND
    const val SPACE_BETWEEN_CARDS = 10

    const val WINDOW_WIDTH = (MAX_CARDS_ON_ROW + 1) * (CARD_WIDTH + SPACE_BETWEEN_CARDS)
    const val WINDOW_HEIGHT = 1010

    val cardShape= CutCornerShape(5.dp)
}