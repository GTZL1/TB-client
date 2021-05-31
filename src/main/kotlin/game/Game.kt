package game

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
//import androidx.compose.ui.gesture.ExperimentalPointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import game.cards.plays.PlayCard
import game.player.Player
import io.ktor.client.*
import theme.cardColors
import theme.cardFont
import java.util.*

class Game(
    val date: Date, val httpClient: HttpClient, private val idSession: Int,
    val player: Player, val opponent: Player
) {
    @Composable
    fun Board() {
        //val handCards= remember { mutableStateListOf<PlayCard>() }
        //handCards.addAll(player.hand.getAllCards())
        val playerRowCards = mutableStateListOf<PlayCard>()
        val centerRowCards = remember { mutableStateListOf<PlayCard>() }
        Column(
            modifier = Modifier.fillMaxSize(1f),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(Constants.ROW_HEIGHT.dp).zIndex(0f)
                    .background(Color.Gray)
            ) {
            }
            Row(
                modifier = Modifier.fillMaxWidth().height(Constants.ROW_HEIGHT.dp).zIndex(0f)
                    .background(Color.Gray)
            ) {
                centerRowCards.forEach { pc ->
                    DisplayCard(card = pc, isMovable = (pc.owner == player.pseudo),
                        onDragEndUp = {},
                        onDragEndDown = {playerRowCards.add(pc)
                            centerRowCards.remove(pc)})
                }
            }
            Row (modifier = Modifier.fillMaxWidth().height(Constants.ROW_HEIGHT.dp).zIndex(0f)
                .background(Color.Gray)) {
                playerRowCards.forEach { pc ->
                    DisplayCard(card = pc, isMovable = (pc.owner == player.pseudo),
                    onDragEndUp = {playerRowCards.remove(pc)
                                    centerRowCards.add(pc)
                        },
                    onDragEndDown = {})
                }
                val a=5
                playerRowCards.map { pc -> print(pc.cardType.name+" ") }
                println("player row cards\n")
            }
            Row(
                modifier = Modifier.fillMaxWidth().height(Constants.ROW_HEIGHT.dp).zIndex(0f)
                    .background(Color.Gray)
            ) {
                player.hand.getAllCards().forEach { pc: PlayCard ->
                    DisplayCard(modifier = Modifier.zIndex(1f),
                        card=pc,
                        isMovable = (pc.owner == player.pseudo),
                        onDragEndUp = {playerRowCards.add(pc)
                            player.hand.putCardOnBoard(pc)

                            },
                        onDragEndDown = {})
                }
                player.hand.getAllCards().map { pc -> print(pc.cardType.name+" ") }
                println("hand cards\n")
            }
        }
    }

    @Composable
    fun DisplayCard(
        modifier: Modifier = Modifier,
        card: PlayCard,
        isMovable:Boolean,
        onDragEndUp: () -> Unit,
        onDragEndDown:()-> Unit
    ) {
        var offsetX by remember { mutableStateOf(0f) }
        var offsetY by remember { mutableStateOf(0f) }
        var start=offsetY
        Box(
            modifier = modifier
                .offset(offsetX.dp, offsetY.dp)
                .clickable(enabled = true, onClick = {})
                .width(100.dp).height(180.dp)
                .clip(shape = CutCornerShape(5.dp))
                .border(width = 2.dp, color = Color.Red, shape = CutCornerShape(5.dp))
                .pointerInput(key1 = null) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consumeAllChanges()
                            if(isMovable){
                                offsetX += dragAmount.x
                                offsetY += dragAmount.y
                            }
                        },
                        onDragEnd = {
                            if(start > offsetY){
                                onDragEndUp()
                            } else if (start < offsetY){
                                onDragEndDown()
                            }
                            start=offsetY
                        })
                },
        ) {
            Column(
                modifier = Modifier.fillMaxSize(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .weight(2.5f)
                        .fillMaxSize()
                        .clip(
                            shape = CutCornerShape(
                                topStart = 5.dp,
                                topEnd = 5.dp,
                                bottomStart = 0.dp,
                                bottomEnd = 0.dp
                            )
                        )
                        .background(Color.White),
                )
                {
                    Image(bitmap = imageResource("card_images/"+ card.cardType.name.lowercase() +".jpg"),
                        contentDescription="Image of the card",
                        contentScale = ContentScale.Crop)

                    val statsBoxShape = CutCornerShape(bottomStart = 5.dp, topEnd = 5.dp)
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd)
                            .width(30.dp).height(45.dp)
                            .clip(shape = statsBoxShape)
                            .border(width = 2.dp, color = Color.Red, shape = statsBoxShape)
                            .background(color = cardColors[card.cardType::class]!!)
                    )
                    {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(text = card.getHealth().toString(), style = cardFont)
                            Text(text = card.cardType.attack.toString(), style = cardFont)
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(shape = CutCornerShape(bottomStart = 5.dp, bottomEnd = 5.dp))
                        .background(color = cardColors[card.cardType::class]!!)
                )
                {
                    Column(
                        modifier = Modifier.fillMaxSize()
                            .padding(horizontal = 5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = card.cardType.name,
                            style = cardFont,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}