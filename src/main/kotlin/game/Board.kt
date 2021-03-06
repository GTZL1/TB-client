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
import androidx.compose.material.IconButton
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
import androidx.compose.ui.res.svgResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import game.cards.plays.PlayCard
import game.cards.plays.SpyPlayCard
import game.cards.types.SpyCardType
import game.cards.types.VehicleCardType
import theme.cardColors
import theme.cardFont
import theme.discardCardFont
import theme.miniFont

/**
 * Display the board and update it according game state
 * @game the Game to display
 * @playIA if the player is playing against IA
 */
@Composable
fun Board(game: Game,
        playIA: Boolean) {
    LaunchedEffect(playIA) {
        if(!playIA) game.receiveMessages()
    }

    //Window content
    Row() {
        //Infos on the side
        Column(
            modifier = Modifier.fillMaxHeight()
                .width(Constants.CARD_WIDTH.dp)
                .background(color = Color.Gray)
                .padding(
                    top = Constants.STATS_BOX_HEIGHT.dp,
                    bottom = Constants.STATS_BOX_HEIGHT.dp
                ),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            //Upper side of column
            Column(
                modifier = Modifier.fillMaxWidth()
                    .height((Constants.CARD_HEIGHT * 1.5).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    modifier = Modifier.size(Constants.STATS_BOX_WIDTH.dp),
                    onClick = { game.endPlayerTurn() },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (notifyChangeTurn(game)) Color.Green else Color.Red,
                    )
                ) {}
                if (notifyChangeTurn(game)) {
                    val time = (game.delay.value / 1000)
                    Text(text = "00:" + (if (time < 10) "0" else "") + time.toString())
                }
            }
            //Discard
            Box(
                modifier = Modifier.fillMaxWidth()
                    .height(Constants.CARD_HEIGHT.dp)
            ) {
                if (game.discardCards.isNotEmpty()) {
                    val topDiscardCard: PlayCard = game.discardCards.last()
                    DisplayCard(
                        modifier = Modifier.align(Alignment.Center),
                        card = topDiscardCard,
                        game = game,
                        toPlayer = (topDiscardCard.owner == game.player.pseudo),
                        isPlayerTurn = false,
                        width = (Constants.CARD_WIDTH * 0.7).toInt(),
                        height = (Constants.CARD_HEIGHT * 0.7).toInt(),
                        inDiscard = true
                    )
                }
            }
            //Lower side of column
            Column(
                modifier = Modifier.fillMaxWidth()
                    .height((Constants.CARD_HEIGHT * 1.5).dp)
                    .padding(start = 5.dp),
                horizontalAlignment = Alignment.Start
            ) {
                if(notifyChangeTurn(game)){
                    Text(text = "Ready for action :",
                        style = miniFont)
                    game.actionableCards(game.player.pseudo).forEach { s ->
                        Text(text = "- $s",
                        style = miniFont)
                    }
                    Text(modifier = Modifier.padding(top = 10.dp),
                        text = minOf(game.handCards.size,(game.player.playDeck.getBaseCards().size - game.cardsMovedFromHand.value)).toString()
                                + " card(s) still deployable",
                        style = miniFont
                    )
                }
            }
        }

        //Game board
        Column(
            modifier = Modifier.fillMaxSize(1f),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            //Opponent row with label
            GameRowPlayerLabel(
                labelPaddingModifier = Modifier.background(Color.Gray)
                    .padding(top = Constants.LABEL_PADDING.dp),
                labelText = (Constants.OPPONENT_LABEL + game.opponent.pseudo + " !"),
                top = true,
                gameRowContent = {
                    game.opponentBaseCards.forEach { pc: PlayCard ->
                        DisplayCard( //base is never clickable
                            card = pc,
                            game = game,
                            toPlayer = game.cardBelongsToOwner(pc),
                            isPlayerTurn = notifyChangeTurn(game)
                        )
                    }
                    game.opponentRowCards.forEach { pc ->
                        DisplayCard(
                            card = pc, game = game,
                            toPlayer = game.cardBelongsToOwner(pc),
                            isPlayerTurn = notifyChangeTurn(game)
                        )
                    }
                }
            )
            // Center row
            GameRow(content = {
                game.centerRowCards.forEach { pc ->
                    DisplayDraggableCard(card = pc, game = game,
                        toPlayer = game.cardBelongsToOwner(pc),
                        isMovableUp = false,
                        isMovableDown = (game.movableToPlayerRow()
                                && game.cardBelongsToOwner(pc)),
                        onDragEndUpOneRank = {},
                        onDragEndDown = {
                            game.cardToPlayerRow(card = pc,
                                                position = Position.PLAYER)
                        }
                    )
                }
            })
            // Player row
            GameRow(content = {
                game.playerBaseCards.forEach { pc: PlayCard ->
                    DisplayCard( //base is never clickable
                        card = pc, game = game,
                        toPlayer = game.cardBelongsToOwner(pc),
                        isPlayerTurn = notifyChangeTurn(game)
                    )
                }
                game.playerRowCards.forEach { pc ->
                    DisplayDraggableCard(card = pc, game = game,
                        toPlayer = game.cardBelongsToOwner(pc),
                        isMovableUp = game.movableToCenterRow(),
                        isMovableDown = false,
                        onDragEndUpOneRank = {
                            game.cardToCenterRow(card = pc,
                                                position = Position.CENTER)
                        },
                        onDragEndDown = {})
                }
            })
            // Hand with label
            GameRowPlayerLabel(labelPaddingModifier = Modifier.background(Color.Gray)
                .padding(bottom = Constants.LABEL_PADDING.dp),
                labelText = (Constants.PLAYER_LABEL + game.player.pseudo + "."),
                top = false,
                gameRowContent = {
                    game.handCards.forEach { pc ->
                        DisplayDraggableCard(card = pc, game = game,
                            toPlayer = game.cardBelongsToOwner(pc),
                            isMovableUp = game.movableFromHand(Position.PLAYER),
                            isMovableUpTwoRank = game.movableFromHand(Position.CENTER)
                                    && (pc.cardType::class == VehicleCardType::class),
                            isMovableDown = false,
                            onDragEndUpOneRank = {
                                if (pc.cardType::class != SpyCardType::class) {
                                    game.cardToPlayerRow(
                                        card = pc,
                                        position = Position.PLAYER,
                                        fromDeck = true
                                    )
                                } else {
                                    game.playSpyCard(card = pc as SpyPlayCard)
                                }
                            },
                            onDragEndUpTwoRank = {
                                game.cardToCenterRow(card = pc,
                                                    position = Position.CENTER,
                                                    fromDeck = true)
                            },
                            onDragEndDown = {})
                    }
                })
        }
    }
}

/**
 * Display a card and do it draggable
 * Handle "only" draggability, use an other function for display
 */
@Composable
private fun DisplayDraggableCard(
    modifier: Modifier = Modifier,
    game: Game,
    card: PlayCard,
    toPlayer: Boolean,
    isPlayerTurn: Boolean = notifyChangeTurn(game),
    hasCardActed: Boolean = game.cardCanAct(card),
    isMovableUp: Boolean,
    isMovableUpTwoRank: Boolean = false,
    isMovableDown: Boolean,
    onDragEndUpOneRank: () -> Unit,
    onDragEndUpTwoRank: () -> Unit = onDragEndUpOneRank,
    onDragEndDown: () -> Unit
) = key(
        card,
        game,
        isMovableUp,
        isMovableUpTwoRank,
        isMovableDown,
        toPlayer,
        isPlayerTurn,
        hasCardActed
) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    val startY = offsetY
    val startX = offsetX
    val isMovable =
        hasCardActed && isPlayerTurn && (isMovableUp || isMovableDown || isMovableUpTwoRank)
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
                        if (isMovableUpTwoRank && (startY - offsetY > (Constants.CARD_DRAG_MARGIN + Constants.BIG_ROW_HEIGHT))) {
                            onDragEndUpTwoRank()
                        } else if ((isMovableUp) && ((startY - offsetY) > Constants.CARD_DRAG_MARGIN)) {
                            onDragEndUpOneRank()
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

/**
 * Display a row of cards
 */
@Composable
private fun GameRow(
    modifier: Modifier = Modifier,
    height: Int = Constants.SMALL_ROW_HEIGHT,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(height.dp).zIndex(0f)
            .background(Color.Gray),
        horizontalArrangement = Arrangement.spacedBy(
            Constants.SPACE_BETWEEN_CARDS.dp,
            Alignment.CenterHorizontally
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
}

/**
 * Display a row with a label above or under it
 */
@Composable
private fun GameRowPlayerLabel(
    modifier: Modifier = Modifier,
    labelPaddingModifier: Modifier,
    labelText: String,
    top: Boolean,
    gameRowContent: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center
    ) {
        if (!top) {
            GameRow(content = gameRowContent)
            PlayerLabel(
                paddingModifier = labelPaddingModifier,
                text = labelText
            )
        } else {
            PlayerLabel(
                paddingModifier = labelPaddingModifier,
                text = labelText
            )
            GameRow(content = gameRowContent)
        }
    }
}

/**
 * Display a little text set to appear above or under a GameRow, centered
 */
@Composable
private fun PlayerLabel(
    paddingModifier: Modifier,
    text: String
) {
    Row(
        modifier = paddingModifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            style = cardFont
        )
    }
}

/**
 * Handle click on card and display it
 */
@Composable
fun DisplayCard(
    modifier: Modifier = Modifier,
    game: Game,
    card: PlayCard,
    toPlayer: Boolean,
    isPlayerTurn: Boolean,
    width: Int = Constants.CARD_WIDTH,
    height: Int = Constants.CARD_HEIGHT,
    inDiscard: Boolean = false
) = key(card, game, toPlayer, isPlayerTurn, inDiscard) {
    val clicked = remember { mutableStateOf(false) }
    val hover = remember { mutableStateOf(false) }
    DisplayNonClickableCard(
        modifier = modifier
            .pointerMoveFilter(onEnter = {
                hover.value = true
                false
            },
                onExit = {
                    hover.value = false
                    false
                })
            .clickable(enabled = isPlayerTurn, onClick = {
                game.handleClick(clicked, card)
            }),
        card = card,
        toPlayer = toPlayer,
        clicked = clicked.value,
        hover = hover.value,
        width = width,
        height = height,
        inDiscard = inDiscard,
        onHeroButtonClick = { playCard: PlayCard ->
            game.handleClick(clicked, playCard, true)
        }
    )
}

/**
 * Display card itself and handle click on power button (for heroes)
 */
@Composable
fun DisplayNonClickableCard(
    modifier: Modifier = Modifier,
    card: PlayCard,
    toPlayer: Boolean,
    clicked: Boolean,
    hover: Boolean,
    width: Int = Constants.CARD_WIDTH,
    height: Int = Constants.CARD_HEIGHT,
    inDiscard: Boolean = false,
    onHeroButtonClick: (PlayCard) -> Unit = {}
) = key(card, toPlayer, clicked, hover, inDiscard) {
    Box(
        modifier = modifier
            .clip(shape = Constants.cardShape)
            .border(
                width = 2.dp,
                color = if (clicked || hover) Color.White else (if (toPlayer) Color.Red else Color.Blue),
                shape = Constants.cardShape
            )
            .width(width.dp).height(height.dp)
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
                if (!inDiscard) {
                    StatsBox(
                        modifier = modifier
                            .align(Alignment.TopEnd)
                            .border(
                                width = 2.dp,
                                color = if (clicked) Color.White else (if (toPlayer) Color.Red else Color.Blue),
                                shape = Constants.statsBoxShape
                            ),
                        card = card,
                    )
                    //Distance and whip strike powers
                    if(card.buttonIconSvg != null){
                        IconButton(
                            modifier = modifier.padding(0.dp)
                                                .size(Constants.STATS_BOX_WIDTH.dp)
                                                .align(Alignment.BottomCenter),
                            onClick = { onHeroButtonClick(card) },
                            content = {
                                Image(painter = svgResource("icons/"+card.buttonIconSvg!!),
                                    contentDescription = "Power logo")
                            },
                        )
                    }
                }
            }
            CardEtiquette(
                modifier = Modifier.weight(1f),
                card = card,
                inDiscard = inDiscard,
            )
        }
    }
}

/**
 * Display stats box of the card
 */
@Composable
fun StatsBox(
    modifier: Modifier = Modifier,
    card: PlayCard,
) {
    Box(
        modifier = modifier.width(Constants.STATS_BOX_WIDTH.dp)
            .height(Constants.STATS_BOX_HEIGHT.dp)
            .clip(shape = Constants.statsBoxShape)
            .background(color = cardColors[card.cardType::class]!!)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = card.getHealth().toString(),
                style = cardFont,
                color = if(card.getHealth() < card.cardType.life) {
                            Color.Red
                        } else {
                            Color.Black
                        }
            )
            Text(text = card.cardType.attack.toString(), style = cardFont)
        }
    }
}

/**
 * Display the etiquette of the card (lower part, with name)
 */
@Composable
fun CardEtiquette(
    modifier: Modifier = Modifier,
    card: PlayCard,
    inDiscard: Boolean = false,
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
                style = if (!inDiscard) cardFont else discardCardFont,
                textAlign = TextAlign.Center
            )
        }
    }
}