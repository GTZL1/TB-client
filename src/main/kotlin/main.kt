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
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.*
import java.time.Instant
import java.util.*
import kotlin.reflect.KClass

val channel = LinkedList<String>()

@ExperimentalPointerInput
fun main() = Window(title = "HEIG game", size = IntSize(700, 1010)) {
    val httpClient = HttpClient {
        install(WebSockets)
        install(JsonFeature) {
            serializer = GsonSerializer()
        }
    }

    val idSession = remember { mutableStateOf((3)) }
    val username = remember { mutableStateOf("aloy") }
    val screenState = remember { mutableStateOf(Screen.BOARD) }
    val login = Login(
        httpClient = httpClient,
        onRightLogin = { screenState.value = Screen.BOARD },
        idSession = idSession,
        playerPseudo = username
    )
    val cardsTypes = listOf<Pair<String, KClass<out CardType>>>(
        Pair("hero", HeroCardType::class),
        Pair("unit", UnitCardType::class),
        Pair("vehicle", VehicleCardType::class),
        Pair("spy", SpyCardType::class),
        Pair("base", BaseCardType::class)
    )
    channel.addFirst("hello")
    when (val screen = screenState.value) {
        Screen.LOGIN -> {
            login.LoginScreen()

        }
        Screen.BOARD -> {
            val game = Game(
                Date.from(Instant.now()), httpClient, idSession = idSession.value,
                Player(
                    pseudo = username.value,
                    deckType = login.generateDeck(login.generateCardTypes(cardsTypes))
                )
            )
            //GlobalScope.launch { websocketInitializer(httpClient) }
            game.Board()
        }
    }
}

suspend fun websocketInitializer(httpClient: HttpClient) = coroutineScope<Unit> {
    runBlocking {
        httpClient.webSocket(
            method = HttpMethod.Get,
            host = "localhost",
            port = 9000,
            path = "/plop"
        ) {
            val messageOutputRoutine = launch { outputMessages() }
            val userInputRoutine = launch { inputMessages(channel.removeLast()) }

            userInputRoutine.join() // Wait for completion; either "exit" or error
            messageOutputRoutine.join()
        }
    }
    //httpClient.close()
}

enum class Screen {
    LOGIN, BOARD
}

suspend fun DefaultClientWebSocketSession.outputMessages() {
    while (true) {
        try {
            for (message in incoming) {
                message as? Frame.Text ?: continue
                println(message.readText())
            }
        } catch (e: Exception) {
            println("Error while receiving: " + e.localizedMessage)
        }
    }
}

suspend fun DefaultClientWebSocketSession.inputMessages(msg: String) {
    if (msg.equals("exit", true)) return
    try {
            send(msg)
    } catch (e: Exception) {
        println("Error while sending: " + e.localizedMessage)
        return
    }
}
