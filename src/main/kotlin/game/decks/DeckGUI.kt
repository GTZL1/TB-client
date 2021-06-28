package game.decks

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyGridScope
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import game.DisplayCard
import theme.menuFont

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DeckScreen(decks: List<DeckType>) {
    val deck = remember { mutableStateOf(decks.first()) }
    Row(
        modifier = Modifier.fillMaxWidth()
            .height(100.dp)
            .background(color = MaterialTheme.colors.primary)
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ){
        DeckChoiceMenu(decks, deck)
        TextField(
            value = deck.value.name,
            onValueChange = { deck.value.name = it},
            textStyle = menuFont,
            label = { Text(text = "Deck name",
                            color = Color.White) },
        )
    }
    LazyVerticalGrid(
        cells = GridCells.Adaptive(Constants.CARD_WIDTH.dp),
        contentPadding = PaddingValues(20.dp)
    ){
        items(deck.value.cardTypes.size) { card ->
            //DisplayCard()
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