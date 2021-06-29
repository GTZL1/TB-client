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
import androidx.compose.runtime.snapshots.SnapshotStateMap
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
import network.LoginRequest
import network.LoginResponse
import org.json.JSONObject
import theme.*

class DeckGUI(
    private val idSession: MutableState<Int>,
    private val httpClient: HttpClient
) {
    internal fun updateDeck(
    ) {
        /*try {
            val response = runBlocking {
                httpClient.request<LoginResponse> {
                    url("http://localhost:9000/login")
                    headers {
                        append("Content-Type", "application/json")
                    }
                    body = UpdateDeckRequest(idSession = idSession.value,
                                            deckType = )
                    method = HttpMethod.Get
                }
            }
            if (response.granted) {
                idSession.value = response.idSession

            }
        } catch (exception: ClientRequestException) {
        }*/
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DeckScreen(cardTypes: List<CardType>, decks: List<DeckType>) {
    val deck = remember { mutableStateOf(decks.first()) }
    var deckName by remember { mutableStateOf(deck.value.name) }
    val cardsTotal =  remember { mutableStateOf(deck.value.cardTypes.map { (_, qty) -> qty }.sum()) }
    val baseCards = cardTypes.filter { cardType: CardType -> cardType::class == BaseCardType::class }

    val cardsDeck= mutableStateMapOf<CardType, Short>()
    DisposableEffect(Unit){
        cardsDeck.putAll(deck.value.cardTypes)

        onDispose {  }
    }

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
            Total(cardsDeck)
            Button(modifier = Modifier.height(50.dp),
                    onClick = {
                        deck.value.cardTypes = (cardsDeck.filterValues { qty: Short -> qty > 0.toShort() })
                        println(deck.value.serialize())
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
                    cardTypes.filter { cardType: CardType -> !baseCards.contains(cardType) }.size
                ) { ixCard ->
                    val card =
                        cardTypes.filter { cardType: CardType -> !baseCards.contains(cardType) }[ixCard]
                    CardMenuItem(
                        cardDecks = cardsDeck,
                        cardType = card,
                        quantity = mutableStateOf(deck.value.cardTypes[card] ?: 0),
                        cardsCount = cardsTotal
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
){
    Column {
        Text(text = "Hero cards: ",
            color = Color.White)
        Text(text = "Total cards: "+ (cardsDeck.map { (_, qty) -> qty }.sum()),
            color = Color.White)
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
private fun CardMenuItem(//deck: DeckType,
                         cardDecks: MutableMap<CardType, Short>,
                        cardType: CardType,
                        quantity: MutableState<Short>,
                        cardsCount: MutableState<Int>){
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
            QuantitySetter(cardDecks = cardDecks,
                            cardType = cardType,
                            quantity = quantity,
                            cardsCount = cardsCount)
        }
    }
}

@Composable
private fun QuantitySetter(//deck: DeckType,
                            cardDecks: MutableMap<CardType, Short>,
                            cardType: CardType,
                           quantity: MutableState<Short>,
                           cardsCount: MutableState<Int>){
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
                val oldQuantity = cardDecks[cardType] ?: 0
                if(oldQuantity < quantity.value){
                    cardsCount.value += (quantity.value-oldQuantity)
                } else {
                    cardsCount.value -= (oldQuantity- quantity.value)
                }
                cardDecks[cardType]=quantity.value
            },
            textStyle = quantityFont,
        )
        Column(modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally){
            IconButton(modifier = Modifier.padding(bottom = 3.dp)
                .height(23.dp).width(30.dp),
                onClick = { if(quantity.value.toInt() < cardType.maxNumberInDeck) {
                                quantity.value++
                                cardsCount.value++
                            }
                            cardDecks[cardType]= quantity.value
                },
                content = {
                    Image(painter = svgResource("icons/arrow_drop_up.svg"),
                        contentDescription = "Arrow up icon",)
                })
            IconButton(modifier = Modifier.height(23.dp).width(30.dp),
                onClick = { if(quantity.value.toInt() > 0) {
                                quantity.value--
                                cardsCount.value--
                            }
                            cardDecks[cardType]= quantity.value},
                content = {
                    Image(painter = svgResource("icons/arrow_drop_down.svg"),
                        contentDescription = "Arrow up icon",)
                })
        }
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
    val deckType: JSONObject
)
