import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.gesture.ExperimentalPointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import game.cards.plays.PlayCard

@ExperimentalPointerInput
@Composable
fun Board(startCards: List<PlayCard>) {
    val handCards = remember { mutableStateListOf<PlayCard>().apply { addAll(startCards) } }
    val playerRowCards = remember { mutableStateListOf<PlayCard>() }
    //handCards.addAll(startCards)
    Column(
        modifier = Modifier.fillMaxSize(1f),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(180.dp).zIndex(0f)
                .background(Color.Gray)
        ) {}
        Row(
            modifier = Modifier.fillMaxWidth().height(180.dp).zIndex(0f)
                .background(Color.Gray)
        ) {}
        Row(
            modifier = Modifier.fillMaxWidth().height(180.dp).zIndex(0f)
                .background(Color.Gray)
        ) {playerRowCards.forEach{
                card: PlayCard -> card.displayCard(onDragEnd={playerRowCards.add(card)})
        }}
        Row(
            modifier = Modifier.fillMaxWidth().height(180.dp).zIndex(0f)
                .background(Color.Gray)
        ) {
            handCards.forEach{
                    card: PlayCard -> card.displayCard(modifier= Modifier.zIndex(1f),
                onDragEnd={playerRowCards.add(card)})
            }
        }
    }
}