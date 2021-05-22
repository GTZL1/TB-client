package game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.gesture.ExperimentalPointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import game.cards.plays.PlayCard
import game.player.Player
import io.ktor.client.*
import java.util.*

class Game(
    val date: Date, val httpClient: HttpClient, private val idSession: Int,
    val player: Player, val opponent: Player
) {
    @ExperimentalPointerInput
    @Composable
    fun Board() {
        //val handCards = remember { mutableStateListOf<PlayCard>().apply { addAll(player.hand) } }
        val playerRowCards = remember { mutableStateListOf<PlayCard>() }
        Column(
            modifier = Modifier.fillMaxSize(1f),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(180.dp).zIndex(0f)
                    .background(Color.Gray)
            ) { opponent.hand.getAllCards().forEach { card: PlayCard ->
                card.DisplayCard(
                    onDragEnd = {})
            }}
            Row(
                modifier = Modifier.fillMaxWidth().height(180.dp).zIndex(0f)
                    .background(Color.Gray)
            ) {}
            Row(
                modifier = Modifier.fillMaxWidth().height(180.dp).zIndex(0f)
                    .background(Color.Gray)
            ) {
                playerRowCards.forEach { card: PlayCard ->
                    card.DisplayCard(onDragEnd = { playerRowCards.add(card) })
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().height(180.dp).zIndex(0f)
                    .background(Color.Gray)
            ) {
                player.hand.getAllCards().forEach { card: PlayCard ->
                    card.DisplayCard(modifier = Modifier.zIndex(1f),
                        onDragEnd = {})
                }
            }
        }
    }
}