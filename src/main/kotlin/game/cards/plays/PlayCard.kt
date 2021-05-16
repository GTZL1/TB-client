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
import androidx.compose.ui.gesture.ExperimentalPointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import game.cards.types.CardType
import loadNetworkImage
import theme.*

open class PlayCard(val cardType: CardType) {
    private var health = cardType.life

    fun getHealth(): Int {
        return health
    }

    fun takeDamage(damage: Int) {
        health -= damage
    }

    fun move() {

    }

    fun attack(target: PlayCard) {
        target.takeDamage(cardType.attack)
        takeDamage(target.cardType.attack)
    }

    @ExperimentalPointerInput
    @Composable
    fun DisplayCard(
        modifier: Modifier = Modifier,
        onDragEnd: () -> Unit
    ) {
        var offsetX by remember { mutableStateOf(0f) }
        var offsetY by remember { mutableStateOf(0f) }
        Box(
            modifier = modifier
                .offset(offsetX.dp, offsetY.dp)
                .clickable(enabled = true, onClick = {})
                .width(100.dp).height(180.dp)
                .clip(shape = CutCornerShape(5.dp))
                .background(color = Color.Gray)
                .border(width = 2.dp, color = Color.Red, shape = CutCornerShape(5.dp))
                .pointerInput {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consumeAllChanges()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        },
                        onDragEnd = {
                            onDragEnd()
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
                                topLeft = 5.dp,
                                topRight = 5.dp,
                                bottomLeft = 0.dp,
                                bottomRight = 0.dp
                            )
                        )
                        .background(Color.White),
                )
                {

                    Image(bitmap = loadNetworkImage(cardType.image),
                            contentScale = ContentScale.Crop)
                    val statsBoxShape = CutCornerShape(bottomLeft = 5.dp, topRight = 5.dp)
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd)
                            .width(30.dp).height(45.dp)
                            .clip(shape = statsBoxShape)
                            .border(width = 2.dp, color = Color.Red, shape = statsBoxShape)
                            .background(color = cardColors[cardType::class]!!)
                    )
                    {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(text = health.toString(), style = cardFont)
                            Text(text = cardType.attack.toString(), style = cardFont)
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(shape = CutCornerShape(bottomLeft = 5.dp, bottomRight = 5.dp))
                        .background(color = cardColors[cardType::class]!!)
                )
                {
                    Column(
                        modifier = Modifier.fillMaxSize()
                            .padding(horizontal = 5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = cardType.name,
                            style = cardFont,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}