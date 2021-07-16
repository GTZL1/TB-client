import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.ui.unit.dp

object Constants {
    const val CLIENT_VERSION = "1.1"

    const val NB_CARDS_HAND = 12
    const val NEW_CARDS_SPY = 2
    const val NEW_CARDS_BASE_DESTROYED = 2

    const val CONNECTION_INIT_MESSAGE = "Hello there"
    const val CARD_MOVEMENT = "movement"
    const val CARD_ATTACK = "attack"
    const val CHANGE_TURN="turn"
    const val EXIT_MESSAGE = "exit"
    const val NEW_ID_MESSAGE = "new id"

    const val MOVEMENT_DELAY : Long = 46000
    private const val CARD_RATIO = 0.55
    const val CARD_HEIGHT = 180
    const val CARD_WIDTH = (CARD_HEIGHT* CARD_RATIO).toInt()
    const val BIG_ROW_HEIGHT = CARD_HEIGHT+40
    const val SMALL_ROW_HEIGHT = CARD_HEIGHT+16

    const val CARD_DRAG_MARGIN = (CARD_HEIGHT * 0.35).toInt()
    private const val MAX_CARDS_ON_ROW = NB_CARDS_HAND
    const val SPACE_BETWEEN_CARDS = 10
    private const val STATS_BOX_RATIO = 0.66
    const val STATS_BOX_HEIGHT = CARD_HEIGHT / 4
    const val STATS_BOX_WIDTH = (STATS_BOX_HEIGHT* STATS_BOX_RATIO).toInt()

    const val PLAYER_LABEL = "You are "
    const val OPPONENT_LABEL = "You're fighting "
    const val LABEL_PADDING = 8
    const val VICTORY_MESSAGE = "Victory !"
    const val DEFEAT_MESSAGE = "Defeat !"

    const val WINDOW_WIDTH =
        ((MAX_CARDS_ON_ROW + 1) * (CARD_WIDTH + SPACE_BETWEEN_CARDS) //board rows
                + CARD_WIDTH) //information column
    const val WINDOW_HEIGHT = 950
    const val CENTER_ROW_CAPACITY = 6
    const val PLAYER_ROW_CAPACITY = 3

    const val MINIMAL_DECK_QUANTITY = 20
    const val CARD_TO_ADD_FOR_HERO = 2

    private const val SHAPE_SIZE = 5
    val cardShape = CutCornerShape(SHAPE_SIZE.dp)
    val topCardShape = CutCornerShape(topStart = SHAPE_SIZE.dp, topEnd = SHAPE_SIZE.dp)
    val statsBoxShape = CutCornerShape(bottomStart = SHAPE_SIZE.dp, topEnd = SHAPE_SIZE.dp)
    val bottomCardShape = CutCornerShape(bottomStart = SHAPE_SIZE.dp, bottomEnd = SHAPE_SIZE.dp)
}