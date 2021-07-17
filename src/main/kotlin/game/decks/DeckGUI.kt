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
import game.cards.types.HeroCardType
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import theme.buttonFont
import theme.menuFont
import theme.miniFont
import theme.quantityFont
import java.util.*
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
    decksList: List<DeckType>
) {
    val decks = mutableStateListOf<DeckType>().apply { addAll(decksList) }
    val deck = mutableStateOf(decks.first())

    val baseCards = cardTypes.filter { cardType: CardType -> cardType::class == BaseCardType::class }

    val cardsDeck= mutableStateMapOf<CardType, Short>().apply { putAll(deck.value.cardTypes) }

    internal fun updateDeck(
    ) {
        try {
            val response = JSONObject(runBlocking {
                httpClient.request<String> {
                    url(System.getenv("TB_SERVER_URL")+":"+System.getenv("TB_SERVER_PORT")+"/decks")
                    headers {
                        append("Content-Type", "application/json")
                    }
                    body = UpdateDeckRequest(idSession = idSession.value,
                                            deckType = deck.value.serialize().toString())
                    method = HttpMethod.Post
                }
            })
            if(response.getLong("idDeck")!=deck.value.id) {
                deck.value.id=response.getLong("idDeck")
            }
        } catch (exception: ClientRequestException) {
        }
    }

    internal fun removeDeck(
    ) {
        try {
            runBlocking {
                httpClient.request<String> {
                    url(System.getenv("TB_SERVER_URL")+":"+System.getenv("TB_SERVER_PORT")+"/decks")
                    headers {
                        append("Content-Type", "application/json")
                    }
                    body = UpdateDeckRequest(idSession = idSession.value,
                        deckType = deck.value.serialize().toString())
                    method = HttpMethod.Delete
                }
            }
            decks.remove(deck.value)
            changeDeck(decks.first(), true)
        } catch (exception: ClientRequestException) {
        }
    }

    internal fun saveDeckLocally() {
        deck.value.cardTypes = (cardsDeck.filterValues { qty: Short -> qty > 0.toShort() })
    }

    internal fun newDeck() {
        decks.add(DeckType((-1), UUID.randomUUID().toString().take(15), mapOf()))
        changeDeck(decks.last())
    }

    internal fun changeDeck(newDeckType: DeckType, afterDeletion: Boolean = false) {
        if(!afterDeletion) saveDeckLocally()
        deck.value=newDeckType
        cardsDeck.clear()
        cardsDeck.putAll(deck.value.cardTypes)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DeckScreen(deckGUI: DeckGUI,
    onSelect: (deck: DeckType) -> Unit,
    onBack: () -> Unit)
{
    val deckName =mutableStateOf(deckGUI.deck.value.name)
    Column(
        modifier = Modifier.fillMaxSize()
    ){
        Row(modifier = Modifier.fillMaxWidth()
                .height(100.dp)
                .background(color = MaterialTheme.colors.primary)
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(modifier = Modifier.height(50.dp).padding(horizontal = 10.dp),
                onClick = {
                    onBack()
                }){
                Text(text = "Back",
                    color = Color.White,
                    style = buttonFont)
            }
            DeckChoiceMenu(deckGUI, deckGUI.decks, deckGUI.deck)
            TextField(
                value = deckName.value,
                onValueChange = { value ->
                    var text = value
                    text.forEach { c: Char ->
                        if(!c.isLetterOrDigit() && !c.isWhitespace()) {
                            text = text.replace(c, ' ', true)
                        }
                    }
                    deckName.value = text
                    deckGUI.deck.value.name=text },
                textStyle = menuFont,
                label = {
                    Text(
                        text = "Deck name",
                        color = Color.White
                    )
                },
            )
            Column {
                Text(text = "Hero cards: "+ TotalHeroes(deckGUI.cardsDeck),
                    color = Color.White)
                Text(text = "Total cards: "+ Total(deckGUI.cardsDeck)
                    +" (min "+ TotalMinimum(deckGUI.cardsDeck) +")",
                    color = Color.White)
            }
            Button(modifier = Modifier.height(50.dp).width(160.dp).padding(end = 10.dp),
                onClick = {
                    deckGUI.newDeck()
                }){
                Text(text = "New deck",
                    color = Color.White,
                    style = buttonFont)
            }
            Button(modifier = Modifier.height(50.dp).padding(end = 10.dp),
                enabled = deckGUI.decks.size > 1,
                onClick = {
                    deckGUI.removeDeck()
                }){
                Text(text = "Delete deck",
                    color = Color.White,
                    style = buttonFont)
            }
            Button(modifier = Modifier.height(50.dp).width(160.dp).padding(end = 10.dp),
                    enabled = Total(deckGUI.cardsDeck) >= TotalMinimum(deckGUI.cardsDeck),
                    onClick = {
                        deckGUI.saveDeckLocally()
                        deckGUI.updateDeck()
                    }){
                    Text(text = "Save deck",
                        color = Color.White,
                        style = buttonFont)
            }
            Column(modifier = Modifier.fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,){
                Button(modifier = Modifier.height(50.dp).padding(bottom = 5.dp),
                    enabled = Total(deckGUI.cardsDeck) >= TotalMinimum(deckGUI.cardsDeck),
                    onClick = {
                        deckGUI.saveDeckLocally()
                        onSelect(deckGUI.deck.value)
                    }) {
                    Text(
                        text = "Select and play !",
                        color = Color.White,
                        style = buttonFont
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center){
                    Text(text = "Play against IA ?",
                        style = miniFont)
                    Switch(checked = false,
                            onCheckedChange = {})
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.72f)
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
                    )
                }
            }
        }
        BaseCardsRow(modifier = Modifier.defaultMinSize(minHeight = (Constants.CARD_HEIGHT+40).dp),
                    deck = deckGUI.deck.value,
                    baseCards = deckGUI.baseCards,
                    cardDecks = deckGUI.cardsDeck)
    }
}

@Composable
private fun DeckChoiceMenu(
    deckGUI: DeckGUI,
    decks: List<DeckType>,
    deck: MutableState<DeckType>
) = key(decks, deck) {
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
                    deckGUI.changeDeck(deckType)
                }){
                    Text(text = deckType.name,
                        style = menuFont)
                }
            }
        })
}

@Composable
private fun CardMenuItem(
    cardsDeck: MutableMap<CardType, Short>,
    cardType: CardType,
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
                            cardType = cardType,)
        }
    }
}

@Composable
private fun QuantitySetter(cardDecks: MutableMap<CardType, Short>,
                            cardType: CardType,){
    Row(modifier = Modifier.width(Constants.CARD_WIDTH.dp)
        .height(50.dp),
        horizontalArrangement = Arrangement.Center){
        TextField(
            modifier = Modifier.fillMaxWidth(0.5f),
            value = (cardDecks[cardType] ?: 0).toString(),
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
            },
            textStyle = quantityFont,
        )
        Column(modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally){
            var newQty=0
            IconButton(modifier = Modifier.padding(bottom = 3.dp)
                .height(23.dp).width(30.dp),
                onClick = { if(cardDecks[cardType]== null || cardDecks[cardType]!! < cardType.maxNumberInDeck) {
                                cardDecks[cardType] = ((cardDecks[cardType] ?: 0) + 1.toShort()).toShort()
                }
                },
                content = {
                    Image(painter = svgResource("icons/arrow_drop_up.svg"),
                        contentDescription = "Arrow up icon",)
                })
            IconButton(modifier = Modifier.height(23.dp).width(30.dp),
                onClick = { if(cardDecks[cardType]!= null && cardDecks[cardType]!! > 0) {
                    cardDecks[cardType] = (cardDecks[cardType]!! - 1.toShort()).toShort()
                }
                    },
                content = {
                    Image(painter = svgResource("icons/arrow_drop_down.svg"),
                        contentDescription = "Arrow up icon",)
                })
        }
    }
}

@Composable
private fun BaseCardsRow(
    modifier: Modifier = Modifier,
    deck: DeckType,
    cardDecks: MutableMap<CardType, Short>,
    baseCards: List<CardType>) = key(deck, cardDecks){
    Row(modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center){
        val currentBase = remember { mutableStateOf(
            if(cardDecks.filter { (card, _) -> baseCards.contains(card) }.isEmpty()) {
                baseCards.first()
            } else {
                cardDecks.filter { (card, _) -> baseCards.contains(card) }.map { (card, _) -> card }.first()
            }
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

@Composable
private fun Total(
    cardsDeck: Map<CardType, Short>
): Int{
    return cardsDeck.filter { (card, _) -> card::class!=BaseCardType::class }
        .map { (_, qty) -> qty }.sum()
}

@Composable
private fun TotalHeroes(
    cardsDeck: Map<CardType, Short>
): Int{
    return Total(cardsDeck.filter { (card, _) -> card::class==HeroCardType::class })
}

@Composable
private fun TotalMinimum(
    cardsDeck: Map<CardType, Short>
): Int {
    return (Constants.MINIMAL_DECK_QUANTITY + TotalHeroes(cardsDeck)*Constants.CARD_TO_ADD_FOR_HERO)
}

data class UpdateDeckRequest(
    val idSession: Int,
    val deckType: String
)
