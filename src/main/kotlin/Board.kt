import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.gesture.ExperimentalPointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@ExperimentalPointerInput
@Composable
fun Board(startCards: List<Card>) {
    val handCards = remember { mutableStateListOf<Card>().apply { addAll(startCards) } }
    val playerRowCards = remember { mutableStateListOf<Card>() }
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
                card: Card -> card.displayCard(onDragEnd={playerRowCards.add(card)})
        }}
        Row(
            modifier = Modifier.fillMaxWidth().height(180.dp).zIndex(0f)
                .background(Color.Gray)
        ) {
            handCards.forEach{
                    card: Card -> card.displayCard(modifier= Modifier.zIndex(1f),
                onDragEnd={playerRowCards.add(card)})
            }
        }
    }
}