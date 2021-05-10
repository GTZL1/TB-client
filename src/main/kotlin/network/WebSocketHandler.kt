package network

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
import java.util.*

object WebSocketHandler {
    val httpClient = HttpClient {
        install(WebSockets)
        install(JsonFeature) {
            serializer = GsonSerializer()
        }
    }
    val msgToSend = LinkedList<String>()
    val msgReceived = Channel<String>()

    suspend fun initialize(onConnectionEstablished: () -> Unit) = coroutineScope<Unit> {
        httpClient.webSocket(
            method = HttpMethod.Get,
            host = "localhost",
            port = 9000,
            path = "/plop"
        ) {
            val messageOutputRoutine = launch { outputMessages() }
            val userInputRoutine = launch { inputMessages() }

            val firstMsg=runBlocking { msgReceived.receive()}
            if(firstMsg.equals(Constants.CONNECTION_INIT_MESSAGE)){
                onConnectionEstablished()
            }
            userInputRoutine.join() // Wait for completion; either "exit" or error
            messageOutputRoutine.cancelAndJoin()
        }
    }

    fun sendMessage(msg: String) {
        msgToSend.addFirst(msg)
    }

    suspend fun lastReceived(): String {
        return msgReceived.receive()
    }

    private suspend fun DefaultClientWebSocketSession.outputMessages() {
        while (true) {
            try {
                for (message in incoming) {
                    message as? Frame.Text ?: continue
                    msgReceived.send(message.readText())
                }
            } catch (e: Exception) {
                println("Error while receiving: " + e.localizedMessage)
            }
        }
    }

    private suspend fun DefaultClientWebSocketSession.inputMessages() {
        while (true) {
            if (msgToSend.isNotEmpty()) {
                val msg = msgToSend.removeLast()
                if (msg.equals("exit", true)) break
                try {
                    send(msg)
                } catch (e: Exception) {
                    println("Error while sending: " + e.localizedMessage)
                    return
                }
            }
        }
    }
}