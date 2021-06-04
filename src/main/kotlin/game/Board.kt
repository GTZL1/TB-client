package game

import Constants
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import game.cards.plays.*
import theme.*

@Composable
fun Board(game: Game) {
    val handCards = remember { mutableStateListOf<PlayCard>() }
    DisposableEffect(Unit) {
        game.player.hand.getAllCards().forEach { pc: PlayCard ->
            handCards.add(pc.cardType.generatePlayCard(pc.owner, pc.id))
        }
        onDispose { }
    }

    val playerRowCards = remember { mutableStateListOf<PlayCard>() }
    val centerRowCards = remember { mutableStateListOf<PlayCard>() }

    DisposableEffect(game) {
        val callback =
            object : GameCallback {
                override fun onNewCard(pc: PlayCard) {
                    playerRowCards.add(pc)
                    handCards.remove(pc)
                    centerRowCards.remove(pc)
                }
            }
        game.registerToPlayerRow(callback)
        onDispose { game.unregisterToPlayerRow(callback) }
    }

    DisposableEffect(game) {
        val callback =
            object : GameCallback {
                override fun onNewCard(pc: PlayCard) {
                    centerRowCards.add(pc)
                    playerRowCards.remove(pc)
                }
            }
        game.registerToCenterRow(callback)
        onDispose { game.unregisterToCenterRow(callback) }
    }

    DisposableEffect(game) {
        val callback =
            object : GameCallback {
                override fun onNewCard(pc: PlayCard) {
                    centerRowCards.remove(pc)
                    playerRowCards.add(pc)
                }
            }
        game.registerQuitCenterRow(callback)
        onDispose { game.unregisterQuitCenterRow(callback) }
    }

    Column(
        modifier = Modifier.fillMaxSize(1f),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        customRow(content =  {
        })
        customRow(content= {
            centerRowCards.forEach { pc ->
                DisplayCard(card = pc,
                    isMovableUp = false,
                    isMovableDown = (pc.owner == game.player.pseudo),
                    onDragEndUp = {},
                    onDragEndDown = {
                        game.cardQuitCenterRow(pc)
                    })
            }
        })
        customRow(content= {
            playerRowCards.map { pc ->
                DisplayCard(card = pc,
                    isMovableUp = true,
                    isMovableDown = false,
                    onDragEndUp = {
                        game.cardToCenterRow(pc)
                    },
                    onDragEndDown = {})
            }
        })
        customRow(content= {
            handCards.map { pc: PlayCard ->
                DisplayCard(modifier = Modifier.zIndex(1f),
                    card = pc,
                    isMovableUp = true,
                    isMovableDown = false,
                    onDragEndUp = {
                        game.cardToPlayerRow(pc)
                    },
                    onDragEndDown = {})
            }
        })
    }
}

@Composable
fun DisplayCard(
    modifier: Modifier = Modifier,
    card: PlayCard,
    isMovableUp: Boolean,
    isMovableDown: Boolean,
    onDragEndUp: () -> Unit,
    onDragEndDown: () -> Unit
) = key(card) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    val startY = offsetY
    val startX = offsetX
    Box(
        modifier = modifier
            .offset(offsetX.dp, offsetY.dp)
            .clickable(enabled = true, onClick = {})
            .width(Constants.CARD_WIDTH.dp).height(Constants.CARD_HEIGHT.dp)
            .clip(shape = Constants.cardShape)
            .border(width = 2.dp, color = Color.Red, shape = Constants.cardShape)
            .pointerInput(key1 = null) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consumeAllChanges()
                        if (isMovableUp || isMovableDown) {
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    },
                    onDragEnd = {
                        if (isMovableUp && ((startY - offsetY) > Constants.CARD_DRAG_MARGIN)) {
                            onDragEndUp()
                        } else if (isMovableDown && ((startY - offsetY) < -Constants.CARD_DRAG_MARGIN)) {
                            onDragEndDown()
                        } else{
                            offsetY=startY
                            offsetX=startX
                        }
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
            )
            {
                Image(
                    bitmap = imageResource("card_images/" + card.cardType.name.lowercase() + ".jpg"),
                    contentDescription = "Image of the card",
                    contentScale = ContentScale.Crop
                )

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

@Composable
fun customRow(modifier: Modifier=Modifier,
              content: @Composable () -> Unit){
    Row(modifier = modifier.fillMaxWidth().height(Constants.ROW_HEIGHT.dp).zIndex(0f)
        .background(Color.Gray),
        horizontalArrangement = Arrangement.spacedBy(Constants.SPACE_BETWEEN_CARDS.dp, Alignment.CenterHorizontally)){
        content()
    }
}