package game

import Constants
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import theme.buttonFont
import theme.endingFont
import theme.menuFont

@Composable
fun EndingScreen(
    modifier: Modifier = Modifier,
    playerName : String,
    opponentName : String,
    victory: Boolean,
    onDeckScreen: () -> Unit,
    onGameAgain: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(modifier = Modifier.padding(bottom = 10.dp),
                text = if (victory) Constants.VICTORY_MESSAGE else Constants.DEFEAT_MESSAGE,
                style = endingFont,
                color = if (victory) Color.Green else Color.Red
            )
            Text(modifier = Modifier.padding(bottom = 10.dp),
            text = "For you $playerName against $opponentName.",
            style = menuFont
            )
            Button(modifier = Modifier.padding(bottom = 10.dp).height(50.dp).width(220.dp),
                onClick = {
                    onDeckScreen()
                }){
                Text(text = "Back to decks",
                    color = Color.White,
                    style = buttonFont
                )
            }
            Button(modifier = Modifier.height(50.dp).width(220.dp),
                onClick = {
                    onGameAgain()
                }){
                Text(text = "Play again",
                    color = Color.White,
                    style = buttonFont
                )
            }
        }
    }
}