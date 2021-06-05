package game

import Constants
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
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
    val playerRowCards = remember { mutableStateListOf<PlayCard>() }
    val centerRowCards = remember { mutableStateListOf<PlayCard>() }

    DisposableEffect(Unit) {
        game.player.hand.getAllCards().forEach { pc: PlayCard ->
            handCards.add(pc.cardType.generatePlayCard(pc.owner, pc.id))
        }
        game.player.playDeck.getBaseCards().forEach { pc: PlayCard ->
            playerRowCards.add(pc.cardType.generatePlayCard(pc.owner, pc.id))
        }
        onDispose { }
    }

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
                    handCards.remove(pc)
                }
            }
        game.registerToCenterRow(callback)
        onDispose { game.unregisterToCenterRow(callback) }
    }

    Column(
        modifier = Modifier.fillMaxSize(1f),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        GameRow(content = {
        })
        GameRow(content = {
            centerRowCards.forEach { pc ->
                DisplayDraggableCard(card = pc,
                    isMovableUp = false,
                    isMovableDown = (pc.owner == game.player.pseudo),
                    onDragEndUpOneRank = {},
                    onDragEndDown = { game.cardToPlayerRow(pc) })
            }
        })
        GameRow(content = {
            playerRowCards.map { pc ->
                DisplayDraggableCard(card = pc,
                    isMovableUp = true,
                    isMovableDown = false,
                    onDragEndUpOneRank = { game.cardToCenterRow(pc) },
                    onDragEndDown = {})
            }
        })
        GameRow(content = {
            handCards.map { pc: PlayCard ->
                DisplayDraggableCard(modifier = Modifier.zIndex(1f),
                    card = pc,
                    isMovableUp = true,
                    isMovableDown = false,
                    onDragEndUpOneRank = { game.cardToPlayerRow(pc) },
                    onDragEndUpTwoRank = { game.cardToCenterRow(pc) },
                    onDragEndDown = {}
                )
            }
        })
    }
}

@Composable
fun DisplayDraggableCard(
    modifier: Modifier = Modifier,
    card: PlayCard,
    isMovableUp: Boolean,
    isMovableDown: Boolean,
    onDragEndUpOneRank: () -> Unit,
    onDragEndUpTwoRank: () -> Unit = onDragEndUpOneRank,
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
                            try {
                                moveVehicle(
                                    card = card as VehiclePlayCard,
                                    startY = startY,
                                    offsetY = offsetY,
                                    onDragEndUpLow = onDragEndUpOneRank,
                                    onDragEndUpHigh = onDragEndUpTwoRank
                                )
                            } catch (t: Throwable) {
                                onDragEndUpOneRank()
                            }
                        } else if (isMovableDown && ((startY - offsetY) < -Constants.CARD_DRAG_MARGIN)) {
                            onDragEndDown()
                        } else {
                            offsetY = startY
                            offsetX = startX
                        }
                    })
            },
    ) {
        DisplayCard(
            modifier = modifier,
            card = card
        )
    }
}

private fun moveVehicle(
    card: VehiclePlayCard, startY: Float, offsetY: Float,
    onDragEndUpLow: () -> Unit,
    onDragEndUpHigh: () -> Unit
) {
    if (startY - offsetY < (Constants.CARD_DRAG_MARGIN + Constants.ROW_HEIGHT)) {
        onDragEndUpLow()
    } else {
        onDragEndUpHigh()
    }
}

@Composable
fun GameRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth().height(Constants.ROW_HEIGHT.dp).zIndex(0f)
            .background(Color.Gray),
        horizontalArrangement = Arrangement.spacedBy(
            Constants.SPACE_BETWEEN_CARDS.dp,
            Alignment.CenterHorizontally
        )
    ) {
        content()
    }
}

@Composable
fun DisplayCard(
    modifier: Modifier = Modifier,
    card: PlayCard
) {
    Column(
        modifier = modifier.fillMaxSize(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .weight(2.5f)
                .fillMaxSize()
                .clip(shape = Constants.topCardShape)
        )
        {
            Image(
                bitmap = imageResource("card_images/" + card.cardType.name.lowercase() + ".jpg"),
                contentDescription = "Image of the card",
                contentScale = ContentScale.Crop
            )

            StatsBox(
                modifier = modifier.align(Alignment.TopEnd),
                card = card
            )
        }
        CardEtiquette(
            modifier = modifier.weight(1f),
            card = card
        )
    }
}

@Composable
fun StatsBox(
    modifier: Modifier = Modifier,
    card: PlayCard
) {
    Box(
        modifier = modifier.width(Constants.STATS_BOX_WIDTH.dp)
            .height(Constants.STATS_BOX_HEIGTH.dp)
            .clip(shape = Constants.statsBoxShape)
            .border(width = 2.dp, color = Color.Red, shape = Constants.statsBoxShape)
            .background(color = cardColors[card.cardType::class]!!)
    ) {
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

@Composable
fun CardEtiquette(
    modifier: Modifier = Modifier,
    card: PlayCard
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(shape = Constants.bottomCardShape)
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