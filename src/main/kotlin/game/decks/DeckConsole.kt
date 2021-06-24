package game.decks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DeckScreen(decks: List<DeckType>) {
    val deck = remember { mutableStateOf(decks.first()) }
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth()
            .height(70.dp)
            .background(color = MaterialTheme.colors.primary)
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ){
        Text(modifier = Modifier.clickable(onClick = {
            expanded=!expanded
        })
            ,text = deck.value.name)
        DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded=false},
            content= {

                DropdownMenuItem(onClick = {}){
                    Text("hello")
                }
            DropdownMenuItem(onClick = {}){
                Text("hello2")
            }
        })
    }
}