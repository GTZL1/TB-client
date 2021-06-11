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
import game.cards.plays.PlayCard
import game.cards.plays.VehiclePlayCard
import game.cards.types.VehicleCardType
import theme.cardColors
import theme.cardFont

@Composable
fun Board(game: Game) {
    val handCards = remember { mutableStateListOf<PlayCard>() }
    val playerRowCards = remember { mutableStateListOf<PlayCard>() }
    val baseCards = remember { mutableStateListOf<PlayCard>() }
    val centerRowCards = remember { mutableStateListOf<PlayCard>() }
    val opponentRowCards = remember { mutableStateListOf<PlayCard>() }
    val playerRowCapacity =
        game.player.playDeck.getBaseCards().size * Constants.PLAYER_ROW_CAPACITY

    DisposableEffect(Unit) {
        game.player.hand.getAllCards().forEach { pc: PlayCard ->
            handCards.add(pc.cardType.generatePlayCard(pc.owner, pc.id))
        }
        game.player.playDeck.getBaseCards().forEach { pc: PlayCard ->
            baseCards.add(pc.cardType.generatePlayCard(pc.owner, pc.id))
        }
        game.opponent.playDeck.getBaseCards().forEach { pc: PlayCard ->
            opponentRowCards.add(pc.cardType.generatePlayCard(pc.owner, pc.id))
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
                    opponentRowCards.remove(pc)
                }
            }
        game.registerToCenterRow(callback)
        onDispose { game.unregisterToCenterRow(callback) }
    }

    DisposableEffect(game) {
        val callback =
            object : GameCallback {
                override fun onNewCard(pc: PlayCard) {
                    opponentRowCards.add(pc)
                }
            }
        game.registerToOpponentRow(callback)
        onDispose { game.unregisterToOpponentRow(callback) }
    }

    Column(
        modifier = Modifier.fillMaxSize(1f),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        GameRow(content = {
            opponentRowCards.forEach { pc ->
                DisplayCard(card = pc)
            }
        })
        GameRow(content = {
            centerRowCards.forEach { pc ->
                DisplayDraggableCard(card = pc,
                    isMovableUp = false,
                    isMovableDown = (playerRowCards.size < playerRowCapacity)
                            && (pc.owner == game.player.pseudo),
                    onDragEndUpOneRank = {},
                    onDragEndDown = { game.cardToPlayerRow(pc)
                    game.notifyMovement(pc, Position.PLAYER)})
            }
        })
        GameRow(content = {
            baseCards.forEach{
                DisplayCard(card = it)
            }
            playerRowCards.forEach { pc ->
                DisplayDraggableCard(card = pc,
                    isMovableUp = centerRowCards.size < Constants.CENTER_ROW_CAPACITY,
                    isMovableDown = false,
                    onDragEndUpOneRank = { game.cardToCenterRow(pc)
                                         game.notifyMovement(pc, Position.CENTRAL)},
                    onDragEndDown = {})
            }
        })
        GameRow(content = {
            handCards.forEach { pc: PlayCard ->
                DisplayDraggableCard(card = pc,
                    isMovableUp = playerRowCards.size < playerRowCapacity,
                    isMovableUpTwoRank = (centerRowCards.size < Constants.CENTER_ROW_CAPACITY)
                            && (pc.cardType::class==VehicleCardType::class),
                    isMovableDown = false,
                    onDragEndUpOneRank = { game.cardToPlayerRow(pc)
                                         game.notifyMovement(pc, Position.PLAYER)},
                    onDragEndUpTwoRank = { game.cardToCenterRow(pc)
                                        game.notifyMovement(pc, Position.CENTRAL)},
                    onDragEndDown = {})
            }
        })
    }
}

@Composable
fun DisplayDraggableCard(
    modifier: Modifier = Modifier,
    card: PlayCard,
    isMovableUp: Boolean,
    isMovableUpTwoRank: Boolean=false,
    isMovableDown: Boolean,
    //positionUp: Game.Position,
    //positionDown: Game.Position=Game.Position.PLAYER_ROW,
    onDragEndUpOneRank: () -> Unit,
    onDragEndUpTwoRank: () -> Unit = onDragEndUpOneRank,
    onDragEndDown: () -> Unit
) = key(card, isMovableUp, isMovableUpTwoRank, isMovableDown) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    val startY = offsetY
    val startX = offsetX
    Box(
        modifier = modifier
            .offset(offsetX.dp, offsetY.dp)
            .pointerInput(key1 = null) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consumeAllChanges()
                        if (isMovableUp || isMovableDown || isMovableUpTwoRank) {
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    },
                    onDragEnd = {
                        if ((isMovableUp || isMovableUpTwoRank) && ((startY - offsetY) > Constants.CARD_DRAG_MARGIN)) {
                            try {
                                moveVehicle(
                                    card = card as VehiclePlayCard,
                                    startY = startY,
                                    offsetY = offsetY,
                                    isMovableUpLow = isMovableUp,
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
    isMovableUpLow: Boolean,
    onDragEndUpLow: () -> Unit,
    onDragEndUpHigh: () -> Unit
) {
    if (isMovableUpLow && (startY - offsetY < (Constants.CARD_DRAG_MARGIN + Constants.ROW_HEIGHT))) {
        onDragEndUpLow()
    } else {
        onDragEndUpHigh()
    }
}

@Composable
fun GameRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
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
    Box(
        modifier = modifier.clickable(enabled = true, onClick = {})
            .width(Constants.CARD_WIDTH.dp).height(Constants.CARD_HEIGHT.dp)
            .clip(shape = Constants.cardShape)
            .border(width = 2.dp, color = Color.Red, shape = Constants.cardShape)
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