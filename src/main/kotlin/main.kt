import androidx.compose.desktop.Window
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.gesture.ExperimentalPointerInput
import androidx.compose.ui.unit.IntSize
import game.Game
import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.websocket.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.util.*

val channel = Channel<String>(1)

@ExperimentalPointerInput
fun main() = Window(title = "HEIG game", size = IntSize(700, 1010)) {
    val httpClient = HttpClient {
        install(WebSockets)
        install(JsonFeature) {
            serializer = GsonSerializer()
        }
    }

    var idSession = remember { mutableStateOf((0)) }

    var screenState = remember { mutableStateOf(Screen.BOARD) }

    when (val screen = screenState.value) {
        Screen.LOGIN ->
            Login(
                client=httpClient,
                onRightLogin = { screenState.value = Screen.BOARD },
                idSession = idSession
            )

        Screen.BOARD -> {
            Game(Date.from(Instant.now()), httpClient).cardsRequest()
        }
    }
}

suspend fun httpRequest() = coroutineScope<Unit> {
    val httpClient = HttpClient {
        install(WebSockets)
    }
    runBlocking {
        while (true) {
            httpClient.webSocket(
                method = HttpMethod.Get,
                host = "localhost",
                port = 9000,
                path = "/plop"
            ) {
                val messageOutputRoutine = launch { outputMessages() }
                val userInputRoutine = launch { inputMessages(channel.receive()) }

                userInputRoutine.join() // Wait for completion; either "exit" or error
                messageOutputRoutine.cancelAndJoin()
            }
        }
        httpClient.close()
    }
}

enum class Screen{
    LOGIN, BOARD
}

suspend fun DefaultClientWebSocketSession.outputMessages() {
    try {
        for (message in incoming) {
            message as? Frame.Text ?: continue
            println(message.readText())
        }
    } catch (e: Exception) {
        println("Error while receiving: " + e.localizedMessage)
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
