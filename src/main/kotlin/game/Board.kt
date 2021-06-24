package game

import Constants
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import game.cards.plays.PlayCard
import game.cards.plays.SpyPlayCard
import game.cards.plays.VehiclePlayCard
import game.cards.types.SpyCardType
import game.cards.types.VehicleCardType
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import theme.cardColors
import theme.cardFont

@Composable
fun Board(game: Game) {
    //val handCards = remember { game.handCards }
    //val playerRowCards = remember { mutableStateListOf<PlayCard>() }
    val baseCards = remember { mutableStateListOf<PlayCard>() }
    val centerRowCards = remember { mutableStateListOf<PlayCard>() }
    val opponentRowCards = remember { mutableStateListOf<PlayCard>() }
    val playerRowCapacity =
        game.player.playDeck.getBaseCards().size * Constants.PLAYER_ROW_CAPACITY

    DisposableEffect(Unit) {
        /*game.player.playDeck.drawHand().forEach { pc: PlayCard ->
            handCards.add(pc.cardType.generatePlayCard(pc.owner, pc.id))
            handCards.last().changePosition(Position.HAND)
        }*/
        game.player.playDeck.getBaseCards().forEach { pc: PlayCard ->
            baseCards.add(pc.cardType.generatePlayCard(pc.owner, pc.id))
            baseCards.last().changePosition(Position.PLAYER)
        }
        game.opponent.playDeck.getBaseCards().forEach { pc: PlayCard ->
            opponentRowCards.add(pc.cardType.generatePlayCard(pc.owner, pc.id))
            opponentRowCards.last().changePosition(Position.OPPONENT)
        }
       GlobalScope.launch { game.receiveMessages() }
        onDispose { }
    }

    //to player row
    /*DisposableEffect(game) {
        val callback =
            object : GameCallback {
                override fun onNewCard(pc: PlayCard) {
                    playerRowCards.add(pc)
                    handCards.remove(pc)
                    centerRowCards.remove(pc)
                    pc.changePosition(Position.PLAYER)
                }
            }
        game.registerToPlayerRow(callback)
        onDispose { game.unregisterToPlayerRow(callback) }
    }*/
    //to center row
    DisposableEffect(game) {
        val callback =
            object : GameCallback {
                override fun onNewCard(pc: PlayCard) {
                    centerRowCards.add(pc)
                    //playerRowCards.remove(pc)
                    //handCards.remove(pc)
                    opponentRowCards.remove(pc)
                    pc.changePosition(Position.CENTER)
                }
            }
        game.registerToCenterRow(callback)
        onDispose { game.unregisterToCenterRow(callback) }
    }
    //to opponent row
    DisposableEffect(game) {
        val callback =
            object : GameCallback {
                override fun onNewCard(pc: PlayCard) {
                    opponentRowCards.add(pc)
                    centerRowCards.remove(pc)
                    //handCards.remove(pc)
                    pc.changePosition(Position.OPPONENT)
                }
            }
        game.registerToOpponentRow(callback)
        onDispose { game.unregisterToOpponentRow(callback) }
    }

    Row() {
        Column(
            modifier = Modifier.fillMaxHeight()
                .width(Constants.CARD_WIDTH.dp)
                .background(color = Color.Gray)
                .padding(top=Constants.STATS_BOX_HEIGTH.dp,
                        bottom = Constants.STATS_BOX_HEIGTH.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
        ) {
                Button(modifier= Modifier.size(Constants.STATS_BOX_WIDTH.dp),
                    onClick = { game.changeTurn() },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (notifyChangeTurn(game)) Color.Green else Color.Red,
                )){}
            Text(text= notifyChangeTurn(game).toString())
        }

        Column(
            modifier = Modifier.fillMaxSize(1f),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Opponent row
            GameRow(content = {
                opponentRowCards.forEach { pc ->
                    DisplayCard(card = pc, game = game,
                        toPlayer = (pc.owner == game.player.pseudo),
                        isPlayerTurn = notifyChangeTurn(game))
                }
            })
            // Center row
            GameRow(content = {
                centerRowCards.forEach { pc ->
                    DisplayDraggableCard(card = pc, game = game,
                        toPlayer = (pc.owner == game.player.pseudo),
                        //isPlayerTurn = notifyChangeTurn(game),
                        isMovableUp = false,
                        isMovableDown = true, //((playerRowCards.size < playerRowCapacity)
                                //&& (pc.owner == game.player.pseudo)),
                        onDragEndUpOneRank = {},
                        onDragEndDown = {
                            game.cardToPlayerRow(pc)
                            game.notifyMovement(pc, Position.PLAYER)
                        })
                }
            })
            // Player row
            GameRow(content = {
                baseCards.forEach {
                    DisplayCard(card = it, game = game,
                        toPlayer = (it.owner == game.player.pseudo),
                    isPlayerTurn = false) //base is never clickable
                }
                game.playerRowCards.forEach { pc ->
                    DisplayDraggableCard(card = pc, game = game,
                        toPlayer = (pc.owner == game.player.pseudo),
                        //isPlayerTurn = notifyChangeTurn(game),
                        isMovableUp = centerRowCards.size < Constants.CENTER_ROW_CAPACITY,
                        isMovableDown = false,
                        onDragEndUpOneRank = {
                            game.cardToCenterRow(pc)
                            game.notifyMovement(pc, Position.CENTER)
                        },
                        onDragEndDown = {})
                }
            })
            // Hand
            GameRow(content = {
                game.handCards.forEach { pc: PlayCard ->
                    DisplayDraggableCard(card = pc, game = game,
                        toPlayer = (pc.owner == game.player.pseudo),
                        //isPlayerTurn = notifyChangeTurn(game),
                        isMovableUp = true,//playerRowCards.size < playerRowCapacity,
                        isMovableUpTwoRank = (centerRowCards.size < Constants.CENTER_ROW_CAPACITY)
                                && (pc.cardType::class == VehicleCardType::class),
                        isMovableDown = false,
                        onDragEndUpOneRank = {
                            if (pc.cardType::class != SpyCardType::class) {
                                game.cardToPlayerRow(pc)
                                game.notifyMovement(pc, Position.PLAYER)
                            } else {
                                game.cardToOpponentRow(pc)
                                (pc as SpyPlayCard).changeOwner(game.opponent.pseudo)
                                game.opponent.playDeck.addCard(pc)
                                game.player.playDeck.drawMultipleCards(Constants.NEW_CARDS_SPY)
                                    .forEach { pc: PlayCard ->
                                        //handCards.add(pc.cardType.generatePlayCard(pc.owner, pc.id))
                                    }
                                game.notifyMovement(pc, Position.OPPONENT)
                            }
                        },
                        onDragEndUpTwoRank = {
                            game.cardToCenterRow(pc)
                            game.notifyMovement(pc, Position.CENTER)
                        },
                        onDragEndDown = {})
                }
            })
        }
    }
}

@Composable
fun DisplayDraggableCard(
    modifier: Modifier = Modifier,
    game: Game,
    card: PlayCard,
    toPlayer: Boolean,
    isPlayerTurn: Boolean = notifyChangeTurn(game),
    isMovableUp: Boolean,
    isMovableUpTwoRank: Boolean = false,
    isMovableDown: Boolean,
    onDragEndUpOneRank: () -> Unit,
    onDragEndUpTwoRank: () -> Unit = onDragEndUpOneRank,
    onDragEndDown: () -> Unit
) = key(card, game, isMovableUp, isMovableUpTwoRank, isMovableDown, toPlayer, isPlayerTurn) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    val startY = offsetY
    val startX = offsetX
    val isMovable= isPlayerTurn && (isMovableUp || isMovableDown || isMovableUpTwoRank)
    Box(
        modifier = modifier
            .offset(offsetX.dp, offsetY.dp)
            .pointerInput(key1 = null) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consumeAllChanges()
                        if (isMovable) {
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
            game = game,
            card = card,
            toPlayer = toPlayer,
            isPlayerTurn = notifyChangeTurn(game)
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
    game: Game,
    card: PlayCard,
    toPlayer: Boolean,
    isPlayerTurn: Boolean,
) = key(card, game, toPlayer, isPlayerTurn){
    val clicked = remember { mutableStateOf(false) }
    val hover = remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .pointerMoveFilter (onEnter = {hover.value=true
                                          false},
            onExit = {hover.value=false
            false})
            .clickable(enabled = isPlayerTurn, onClick = {
                    game.handleClick(clicked, card)
            })
            .width(Constants.CARD_WIDTH.dp).height(Constants.CARD_HEIGHT.dp)
            .clip(shape = Constants.cardShape)
            .border(
                width = 2.dp,
                color = if(clicked.value || hover.value) Color.White else (if (toPlayer) Color.Red else Color.Blue),
                shape = Constants.cardShape
            )
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
                    modifier = modifier
                        .align(Alignment.TopEnd)
                        .border(width = 2.dp,
                            color = if(clicked.value) Color.White else (if (toPlayer) Color.Red else Color.Blue),
                            shape = Constants.statsBoxShape),
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