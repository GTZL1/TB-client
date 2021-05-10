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
import network.WebSocketHandler
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


    val cardsTypes = listOf<Pair<String, KClass<out CardType>>>(
        Pair("hero", HeroCardType::class),
        Pair("unit", UnitCardType::class),
        Pair("vehicle", VehicleCardType::class),
        Pair("spy", SpyCardType::class),
        Pair("base", BaseCardType::class)
    )

    Window(title = "HEIG game", size = IntSize(700, 1010)) {
        val idSession = remember { mutableStateOf((3)) }
        val username = remember { mutableStateOf("aloy") }
        val screenState = remember { mutableStateOf(Screen.MATCHMAKING) }
        val login = Login(
            httpClient = httpClient,
            onRightLogin = { screenState.value = Screen.MATCHMAKING },
            idSession = idSession,
            playerPseudo = username
        )

        when (val screen = screenState.value) {
            Screen.LOGIN -> {
                login.LoginScreen()

            }
            Screen.MATCHMAKING -> {
                GlobalScope.launch { WebSocketHandler.initialize {
                    run {
                        screenState.value = Screen.BOARD
                    }
                }
                }
                WebSocketHandler.sendMessage(Constants.CONNECTION_INIT_MESSAGE)

            }
            Screen.BOARD -> {
                val game = Game(
                    Date.from(Instant.now()), httpClient, idSession = idSession.value,
                    Player(
                        pseudo = username.value,
                        deckType = login.generateDeck(login.generateCardTypes(cardsTypes))
                    )
                )
                game.Board()
            }
        }
    }
}

enum class Screen {
    LOGIN, BOARD, MATCHMAKING
}


