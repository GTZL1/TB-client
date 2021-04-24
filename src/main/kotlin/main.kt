import androidx.compose.desktop.Window
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.gesture.ExperimentalPointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.ktor.client.*
import io.ktor.client.features.websocket.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

val channel = Channel<String>(1)

@ExperimentalPointerInput
fun main() = Window(title = "Compose for Desktop", size = IntSize(700, 1010)) {
    val ahsokaCard = Card("Ahsoka Tano", 8, 10)

    MaterialTheme {
        GlobalScope.launch {
            //httpRequest()
        }
        Board(listOf(ahsokaCard))
        //ahsokaCard.displayCard()
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

@ExperimentalPointerInput
@Composable
fun Board(startCards: List<Card>) {
    val handCards = remember { mutableStateListOf<Card>().apply { addAll(startCards) } }
    val playerRowCards = remember { mutableStateListOf<Card>() }
    //handCards.addAll(startCards)
    Column(
        modifier = Modifier.fillMaxSize(1f),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(180.dp).zIndex(0f)
                .background(Color.Gray)
        ) {}
        Row(
            modifier = Modifier.fillMaxWidth().height(180.dp).zIndex(0f)
                .background(Color.Gray)
        ) {}
        Row(
            modifier = Modifier.fillMaxWidth().height(180.dp).zIndex(0f)
                .background(Color.Gray)
        ) {playerRowCards.forEach{
                    card: Card -> card.displayCard(onDragEnd={playerRowCards.add(card)})
            }}
        Row(
            modifier = Modifier.fillMaxWidth().height(180.dp).zIndex(0f)
                .background(Color.Gray)
        ) {
            handCards.forEach{
                    card: Card -> card.displayCard(modifier=Modifier.zIndex(1f),
                onDragEnd={playerRowCards.add(card)})
            }
        }
    }
}
