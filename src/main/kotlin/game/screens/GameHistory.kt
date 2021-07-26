package game.screens

import Constants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import theme.SpyBackgroundColor
import theme.buttonFont

class GameHistory(
    private val idSession: Int,
    private val httpClient: HttpClient,
    val username: String,
    private val onBack: () -> Unit,
    private val serverUrl: MutableState<String>,
    private val serverPort: String = Constants.SERVER_PORT
) {
    internal fun getGamesHistory(): ArrayList<GameRecord> {
        val games= ArrayList<GameRecord>()
        try {
            val response = JSONArray(runBlocking {
                httpClient.request<String> {
                    url("http://"+serverUrl.value+":"+serverPort+"/game")
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

    fun onBackClick() {
        this.onBack()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(gameHistory: GameHistory, )
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
                    gameHistory.onBackClick()
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
            if(games.isEmpty()) {
                Text(modifier = Modifier.align(Alignment.Center),
                    text = "You haven't played any game yet :(",
                    style = buttonFont)
            }
            LazyColumn(
                modifier = Modifier.align(Alignment.Center),
                ){
                items(games.size){index: Int ->
                    if(index==0) {
                        Row(modifier = Modifier.width(600.dp).padding(bottom = 5.dp)){
                            GameRecordRowText(text = "Date",
                                username = gameHistory.username,
                            color = Color.Black)
                            GameRecordRowText(text = "Winner",
                                username = gameHistory.username,
                                color = Color.Black)
                            GameRecordRowText(text = "Looser",
                                username = gameHistory.username,
                                color = Color.Black)
                            GameRecordRowText(text = "Issue",
                                username = gameHistory.username,
                                color = Color.Black)
                        }
                    }
                    GameRecordRow(gameRecord = games[index],
                                username = gameHistory.username)
                }
            }
        }
    }
}

@Composable
private fun GameRecordRow(gameRecord: GameRecord,
username: String) {
    Row(modifier = Modifier.width(600.dp).fillMaxHeight()){
        GameRecordRowText(gameRecord.date,
                        username = username)
        GameRecordRowText(text = gameRecord.winner,
                        username = username)
        GameRecordRowText(text = gameRecord.looser,
                    username = username)
        GameRecordRowText(if(gameRecord.victory) Constants.VICTORY_MESSAGE else Constants.DEFEAT_MESSAGE,
                            username = username)
    }
}

@Composable
private fun GameRecordRowText(text: String,
                              username: String,
                                color: Color = Color.Gray){
    Text(modifier = Modifier.padding(end = 5.dp).width(150.dp),
        text = text,
    style = buttonFont,
    color = if(text.equals(username)) Color.SpyBackgroundColor else color)
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
