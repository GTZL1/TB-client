import androidx.compose.desktop.Window
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntSize
import game.*
import game.cards.types.*
import game.decks.DeckGUI
import game.decks.DeckScreen
import game.player.Player
import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.websocket.*
import kotlinx.coroutines.launch
import network.Login
import network.PlayerInitialization
import network.SimpleMessage
import network.WebSocketHandler
import org.json.JSONObject
import java.time.Instant
import java.util.*
import kotlin.reflect.KClass

fun main(args: Array<String>): Unit {
    System.setProperty("skiko.renderApi", "OPENGL")
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

    Window(title = "HEIG game", size = IntSize(Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT)) {
        val game= remember { mutableStateOf<Game?>(null) }

        val idSession = remember { mutableStateOf(args[0].toInt()) }
        val username = remember { mutableStateOf(args[1]) }
        val screenState = remember { mutableStateOf(Screen.BOARD) }
        val login = Login(
            httpClient = httpClient,
            onRightLogin = { screenState.value = Screen.DECK },
            idSession = idSession,
            playerPseudo = username
        )
        val websocket=WebSocketHandler()
        when (val screen = screenState.value) {
            Screen.LOGIN -> {
                login.LoginScreen()

            }
            Screen.DECK ->{
                val deckGUI=DeckGUI(idSession = idSession,
                    httpClient = httpClient,
                    cardTypes = login.generateCardTypes(cardClasses),
                    decksList = login.generateDeck(login.generateCardTypes(cardClasses)))
                DeckScreen(deckGUI = remember { deckGUI })
            }
            Screen.BOARD -> {
                val cardTypes=login.generateCardTypes(cardClasses)
                val player=Player(
                    pseudo = username.value,
                    deckType = login.generateDeck(cardTypes).first()
                )
                LaunchedEffect(true) { launch{websocket.initialize { run{}} }
                    websocket.sendMessage(JSONObject(SimpleMessage(Constants.CONNECTION_INIT_MESSAGE)))
                    websocket.receiveOne()
                    websocket.sendMessage(JSONObject(PlayerInitialization(username = username.value, deckType = player.deckType.serialize())))
                    val opponentDeck = websocket.receiveOne()
                    val g = Game(
                        Date.from(Instant.now()), websocket, idSession = idSession.value,
                        player = player,
                        opponent = Player(
                            pseudo = opponentDeck.getString("username"),
                            deckType = login.generateDeck(
                                cardTypes,
                                opponentDeck.getJSONObject("deckType")
                            )
                        )
                    )
                    g.determineFirst()
                    game.value=g
                }
                val currentGame=game.value
                if(currentGame!=null){
                    Board(currentGame)
                }
            }
        }
    }
}

enum class Screen {
    LOGIN, BOARD, DECK
}


