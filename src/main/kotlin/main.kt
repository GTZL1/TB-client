import androidx.compose.desktop.Window
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.gesture.ExperimentalPointerInput
import androidx.compose.ui.unit.IntSize
import game.Game
import game.cards.types.*
import game.player.Player
import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.websocket.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import network.SimpleMessage
import network.Login
import network.PlayerInitialization
import network.WebSocketHandler
import org.json.JSONObject
import java.time.Instant
import java.util.*
import kotlin.reflect.KClass

@ExperimentalPointerInput
fun main(args: Array<String>): Unit {
    val httpClient = HttpClient {
        install(WebSockets)
        install(JsonFeature) {
            serializer = GsonSerializer()
        }
    }


    val cardClasses = listOf<Pair<String, KClass<out CardType>>>(
        Pair("hero", HeroCardType::class),
        Pair("unit", UnitCardType::class),
        Pair("vehicle", VehicleCardType::class),
        Pair("spy", SpyCardType::class),
        Pair("base", BaseCardType::class)
    )

    Window(title = "HEIG game", size = IntSize(700, 1010)) {
        val idSession = remember { mutableStateOf((3)) }
        val username = remember { mutableStateOf("aloy") }
        val screenState = remember { mutableStateOf(Screen.BOARD) }
        val login = Login(
            httpClient = httpClient,
            onRightLogin = { screenState.value = Screen.BOARD },
            idSession = idSession,
            playerPseudo = username
        )
        val websocket=WebSocketHandler()
        when (val screen = screenState.value) {
            Screen.LOGIN -> {
                login.LoginScreen()

            }
            Screen.MATCHMAKING ->{

            }
            Screen.BOARD -> {
                GlobalScope.launch {  websocket.initialize { run{}} }
                websocket.sendMessage(JSONObject(SimpleMessage(Constants.CONNECTION_INIT_MESSAGE)))
                runBlocking { websocket.lastReceived() }
                val cardTypes=login.generateCardTypes(cardClasses)
                val player=Player(
                pseudo = username.value,
                deckType = login.generateDeck(cardTypes)
                )
                websocket.sendMessage(JSONObject(PlayerInitialization(username = username.value, deckType = player.deckType.serialize())))
                val opponent= runBlocking { websocket.lastReceived() }

                val game = Game(
                    Date.from(Instant.now()), httpClient, idSession = idSession.value,
                    player,
                    Player(pseudo = opponent.getString("username"),
                    deckType =  login.generateDeck(cardTypes,opponent.getJSONObject("deckType")))
                )
                game.Board()
            }
        }
    }
}

enum class Screen {
    LOGIN, BOARD, MATCHMAKING
}


