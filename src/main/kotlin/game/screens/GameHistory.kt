package game.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import game.decks.UpdateDeckRequest
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import theme.buttonFont
import theme.menuFont

class GameHistory(
    private val idSession: Int,
    private val httpClient: HttpClient,
    private val username: String
) {
    internal fun getGamesHistory(): ArrayList<GameRecord> {
        val games= ArrayList<GameRecord>()
        try {
            val response = JSONArray(runBlocking {
                httpClient.request<String> {
                    url(System.getenv("TB_SERVER_URL")+":"+System.getenv("TB_SERVER_PORT")+"/game")
                    headers {
                        append("Content-Type", "application/json")
                    }
                    body = GameHistoryRequest(idSession = idSession)
                    method = HttpMethod.Get
                }
            })
            for(x in 0 until response.length()){
                val game = response.getJSONObject(x)
                games.add(
                    GameRecord(
                        date = game.getString("date"),
                        winner = game.getString("winner"),
                        looser = game.getString("looser"),
                        victory = game.getString("winner").equals(username)
                    )
                )
            }
        } catch (exception: ClientRequestException) {
        }
        return games
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(gameHistory: GameHistory,
               onBack: () -> Unit)
{
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .height(100.dp)
                .background(color = MaterialTheme.colors.primary)
                .padding(20.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Button(modifier = Modifier.height(50.dp).width(160.dp),
                onClick = {
                    onBack()
                }) {
                Text(
                    text = "Back to menu",
                    color = Color.White,
                    style = buttonFont
                )
            }
        }
        Box(modifier = Modifier.fillMaxSize()){
            val games = gameHistory.getGamesHistory()
            LazyColumn(
                modifier = Modifier.align(Alignment.Center),
                ){
                items(games.size){index: Int ->
                    val game = games.get(index)
                    Text(text = game.date+" "+game.winner+" "+game.looser,
                    style = menuFont)
                }
            }
        }
    }
}

data class GameHistoryRequest(
    val idSession: Int
)
data class GameRecord(
    val date: String,
    val winner: String,
    val looser: String,
    val victory: Boolean
)
