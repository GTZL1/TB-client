package game.decks

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.svgResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import game.DisplayNonClickableCard
import game.cards.types.BaseCardType
import game.cards.types.CardType
import theme.cardFont
import theme.menuFont
import theme.miniFont
import theme.quantityFont
import java.lang.NumberFormatException

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DeckScreen(cardTypes: List<CardType>, decks: List<DeckType>) {
    val deck = remember { mutableStateOf(decks.first()) }
    var deckName by remember { mutableStateOf(deck.value.name) }
    val baseCards = cardTypes.filter { cardType: CardType -> cardType::class == BaseCardType::class }
    Column(
        modifier = Modifier.fillMaxSize()
    ){
        Row(
            modifier = Modifier.fillMaxWidth()
                .height(100.dp)
                .background(color = MaterialTheme.colors.primary)
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DeckChoiceMenu(decks, deck)
            TextField(
                value = deckName,
                onValueChange = { deckName = it
                    deck.value.name=deckName},
                textStyle = menuFont,
                label = {
                    Text(
                        text = "Deck name",
                        color = Color.White
                    )
                },
            )
        }
        Row(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.7f)
        ) {
            LazyVerticalGrid(
                cells = GridCells.Adaptive((Constants.CARD_WIDTH + 40).dp),
            ) {
                items(
                    cardTypes.filter { cardType: CardType -> !baseCards.contains(cardType) }.size
                ) { ixCard ->
                    val card =
                        cardTypes.filter { cardType: CardType -> !baseCards.contains(cardType) }[ixCard]
                    val quantity = deck.value.cardTypes[card] ?: 0
                    CardMenuItem(
                        cardType = card,
                        quantity = mutableStateOf(quantity)
                    )
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center){
            baseCards.forEach { baseCard ->
                Column(modifier = Modifier.padding(horizontal = 10.dp)
                    .width(Constants.CARD_WIDTH.dp),
                    horizontalAlignment = Alignment.CenterHorizontally){
                    DisplayNonClickableCard(card = baseCard.generatePlayCard("aloy", 2017),
                        toPlayer = true,
                        clicked = false,
                        hover = false)
                    RadioButton(
                        modifier = Modifier.padding(top=10.dp),
                        selected = (deck.value.cardTypes[baseCard] ?: 0) > 0,
                        enabled = true,
                        onClick = {}
                    )
                }
            }
        }
    }
}

@Composable
private fun DeckChoiceMenu(
    decks: List<DeckType>,
    deck: MutableState<DeckType>
) = key(deck){
    var expanded by remember { mutableStateOf(false) }
    Text(text = deck.value.name,
        modifier = Modifier.clickable(onClick = {
            expanded=!expanded })
            .width(250.dp)
            .padding(end = 50.dp),
        color= Color.White,
        style = menuFont)
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded=false},
        content= {
            decks.forEach { deckType: DeckType ->
                DropdownMenuItem(onClick = {
                    expanded = false
                    deck.value=deckType}){
                    Text(text = deckType.name,
                        style = menuFont)
                }
            }
        })
}

@Composable
private fun CardMenuItem(cardType: CardType,
                        quantity: MutableState<Short>){
    Box(modifier = Modifier.padding(20.dp)
        .width(Constants.CARD_WIDTH.dp)){
        Column(modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally) {
            DisplayNonClickableCard(
                card = cardType.generatePlayCard("aloy", 2017),
                toPlayer = true,
                clicked = false,
                hover = false,
            )
            Row(modifier = Modifier.padding(4.dp)) {
                Text(text = "Max "+cardType.maxNumberInDeck,
                style = miniFont,
                color = Color.Gray)
            }
            QuantitySetter(cardType = cardType,
                            quantity = quantity)
        }
    }
}

@Composable
private fun QuantitySetter(cardType: CardType,
                           quantity: MutableState<Short>){
    Row(modifier = Modifier.width(Constants.CARD_WIDTH.dp)
        .height(50.dp),
        horizontalArrangement = Arrangement.Center){
        TextField(
            modifier = Modifier.fillMaxWidth(0.5f),
            value = quantity.value.toString(),
            onValueChange = { quantity.value = try{
                    var s = it.toShort()
                    if (s> cardType.maxNumberInDeck) {
                        s= cardType.maxNumberInDeck.toShort()
                    } else if (s < 0) {
                        s=0
                    }
                    s
                } catch (t: NumberFormatException){
                    0
                }
            },
            textStyle = quantityFont,
        )
        Column(modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally){
            IconButton(modifier = Modifier.padding(bottom = 3.dp)
                .height(23.dp).width(30.dp),
                onClick = { if(quantity.value.toInt() < cardType.maxNumberInDeck) quantity.value++ },
                content = {
                    Image(painter = svgResource("icons/arrow_drop_up.svg"),
                        contentDescription = "Arrow up icon",)
                })
            IconButton(modifier = Modifier.height(23.dp).width(30.dp),
                onClick = { if(quantity.value.toInt() > 0) quantity.value-- },
                content = {
                    Image(painter = svgResource("icons/arrow_drop_down.svg"),
                        contentDescription = "Arrow up icon",)
                })
        }
    }
}
