package game

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.dp
import theme.buttonFont
import theme.endingFont
import theme.menuFont

@Composable
fun IntermediateScreen(
    modifier: Modifier = Modifier,
    username: String,
    onDeckScreen: () -> Unit,
    onGameHistory: () -> Unit,
    onQuitGame: () -> Unit
) {
    Row(
        modifier = modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(){
                Image(bitmap = imageResource("LogoFancy.png"),
                    contentDescription = "Game logo")
            }
            Text(modifier = Modifier.padding(vertical = 20.dp),
                text = "Welcome in the Ultimate Pop-culture battle, $username !",
                style = endingFont,
            )
            Button(modifier = Modifier.padding(bottom = 10.dp).height(50.dp).width(220.dp),
                onClick = {
                    onDeckScreen()
                }){
                Text(text = "Decks configuration",
                    color = Color.White,
                    style = buttonFont
                )
            }
            Button(modifier = Modifier.padding(bottom = 10.dp).height(50.dp).width(220.dp),
                onClick = {
                    onGameHistory()
                }){
                Text(text = "See game history",
                    color = Color.White,
                    style = buttonFont
                )
            }
            Button(modifier = Modifier.height(50.dp).width(220.dp),
                onClick = {
                    onQuitGame()
                }){
                Text(text = "Quit game",
                    color = Color.White,
                    style = buttonFont
                )
            }
        }
    }
}