package game.decks

import Constants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.svgResource
import androidx.compose.ui.unit.dp
import game.DisplayNonClickableCard
import game.cards.types.BaseCardType
import game.cards.types.CardType
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import theme.buttonFont
import theme.menuFont
import theme.miniFont
import theme.quantityFont
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableMap
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.filter
import kotlin.collections.filterValues
import kotlin.collections.first
import kotlin.collections.forEach
import kotlin.collections.map
import kotlin.collections.set
import kotlin.collections.sum

class DeckGUI(
    private val idSession: MutableState<Int>,
    private val httpClient: HttpClient,
    val cardTypes: List<CardType>,
    val decks: List<DeckType>
) {
    val deck = mutableStateOf(decks.first())
    var deckName =mutableStateOf(deck.value.name)
    val baseCards = cardTypes.filter { cardType: CardType -> cardType::class == BaseCardType::class }

    val cardsDeck= mutableStateMapOf<CardType, Short>()

    init {
        cardsDeck.putAll(deck.value.cardTypes)
    }

    internal fun updateDeck(
    ) {
        println(deck.value.serialize())
        try {
            val response = runBlocking {
                httpClient.request<String> {
                    url("http://localhost:9000/decks")
                    headers {
                        append("Content-Type", "application/json")
                    }
                    body = UpdateDeckRequest(idSession = idSession.value,
                                            deckType = deck.value.serialize().toString())
                    method = HttpMethod.Post
                }
            }
        } catch (exception: ClientRequestException) {
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DeckScreen(deckGUI: DeckGUI) {
    //val cardsTotal =  remember { mutableStateOf(deckGUI.deck.value.cardTypes.map { (_, qty) -> qty }.sum()) }
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
            DeckChoiceMenu(deckGUI.decks, deckGUI.deck)
            TextField(
                value = deckGUI.deckName.value,
                onValueChange = { value ->
                    deckGUI.deckName.value = value
                    deckGUI.deck.value.name=value},
                textStyle = menuFont,
                label = {
                    Text(
                        text = "Deck name",
                        color = Color.White
                    )
                },
            )
            Column {
                Text(text = "Hero cards: ",
                    color = Color.White)
                Text(text = "Total cards: "+ Total(deckGUI.cardsDeck),
                    color = Color.White)
            }

            Button(modifier = Modifier.height(50.dp),
                    onClick = {
                        deckGUI.deck.value.cardTypes = (deckGUI.cardsDeck.filterValues { qty: Short -> qty > 0.toShort() })
                        deckGUI.updateDeck()
                    }){
                    Text(text = "Save deck",
                        color = Color.White,
                        style = buttonFont)
            }
        }
        Row(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.7f)
        ) {
            LazyVerticalGrid(
                cells = GridCells.Adaptive((Constants.CARD_WIDTH + 40).dp),
            ) {
                items(
                    deckGUI.cardTypes.filter { cardType: CardType -> !deckGUI.baseCards.contains(cardType) }.size
                ) { ixCard ->
                    val card =
                        deckGUI.cardTypes.filter { cardType: CardType -> !deckGUI.baseCards.contains(cardType) }[ixCard]
                    CardMenuItem(
                        cardsDeck = deckGUI.cardsDeck,
                        cardType = card,
                        quantity = mutableStateOf(deckGUI.deck.value.cardTypes[card] ?: 0),
                        //cardsCount = cardsTotal
                    )
                }
            }
        }
        /*if(cardsDeck.isNotEmpty()){
            BaseCardsRow(deck = deck.value, baseCards = baseCards,
                cardDecks = cardsDeck)
        }*/
    }
}

@Composable
private fun Total(
    cardsDeck: Map<CardType, Short>
): Int{
    /*Column {
        Text(text = "Hero cards: ",
            color = Color.White)
        Text(text = "Total cards: "+ ,
            color = Color.White)
    }*/
    return cardsDeck.map { (_, qty) -> qty }.sum()
}

@Composable
private fun DeckChoiceMenu(
    decks: List<DeckType>,
    deck: MutableState<DeckType>
) = key(decks, deck){
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
private fun CardMenuItem(//deck: DeckType,
    cardsDeck: MutableMap<CardType, Short>,
    cardType: CardType,
    quantity: MutableState<Short>,
    //cardsCount: MutableState<Int>
) = key(cardsDeck){
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
            QuantitySetter(cardDecks = cardsDeck,
                            cardType = cardType,
                            quantity = quantity,)
        }
    }
}

@Composable
private fun QuantitySetter(//deck: DeckType,
                            cardDecks: MutableMap<CardType, Short>,
                            cardType: CardType,
                           quantity: MutableState<Short>)=key(quantity){
    Row(modifier = Modifier.width(Constants.CARD_WIDTH.dp)
        .height(50.dp),
        horizontalArrangement = Arrangement.Center){
        TextField(
            modifier = Modifier.fillMaxWidth(0.5f),
            value = cardDecks[cardType].toString(),
            onValueChange = { cardDecks[cardType] = try{
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
               //cardDecks[cardType]=quantity.value
            },
            textStyle = quantityFont,
        )
        /*Column(modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally){
            IconButton(modifier = Modifier.padding(bottom = 3.dp)
                .height(23.dp).width(30.dp),
                onClick = { if(quantity.value.toInt() < cardType.maxNumberInDeck) {
                                cardDecks[cardType] += 1
                }
                            //cardDecks[cardType]= quantity.value
                },
                content = {
                    Image(painter = svgResource("icons/arrow_drop_up.svg"),
                        contentDescription = "Arrow up icon",)
                })
            IconButton(modifier = Modifier.height(23.dp).width(30.dp),
                onClick = { if(quantity.value.toInt() > 0) {
                                quantity.value--

                            }
                            //cardDecks[cardType]= quantity.value
                    },
                content = {
                    Image(painter = svgResource("icons/arrow_drop_down.svg"),
                        contentDescription = "Arrow up icon",)
                })
        }*/
    }
}

@Composable
private fun BaseCardsRow(
    deck: DeckType,
    cardDecks: MutableMap<CardType, Short>,
    baseCards: List<CardType>){
    Row(modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center){
        val currentBase = remember { mutableStateOf<CardType>(
            cardDecks.filter { (card, _) -> baseCards.contains(card) }.map { (card, _) -> card }.first()
        ) }
        baseCards.forEach { baseCard: CardType ->
            Column(modifier = Modifier.padding(horizontal = 10.dp)
                .width(Constants.CARD_WIDTH.dp),
                horizontalAlignment = Alignment.CenterHorizontally){
                DisplayNonClickableCard(card = baseCard.generatePlayCard("aloy", 2017),
                    toPlayer = true,
                    clicked = false,
                    hover = false)
                Row(modifier = Modifier.padding(4.dp)) {
                    Text(text = "Qty "+baseCard.maxNumberInDeck,
                        style = miniFont,
                        color = Color.Gray)
                }
                RadioButton(
                    selected = baseCard == currentBase.value,
                    enabled = true,
                    onClick = {
                        cardDecks[currentBase.value]=0
                        currentBase.value=baseCard
                        cardDecks[currentBase.value]=currentBase.value.maxNumberInDeck.toShort()
                    }
                )
            }
        }
    }
}

data class UpdateDeckRequest(
    val idSession: Int,
    val deckType: String
)
