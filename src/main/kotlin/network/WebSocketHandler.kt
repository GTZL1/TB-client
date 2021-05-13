package network

import Constants
import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.websocket.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.json.JSONObject
import java.lang.invoke.MethodHandles.loop
import java.util.*
import java.util.logging.Logger

class WebSocketHandler {
    val websocketHttpClient = HttpClient {
        install(WebSockets)
        install(JsonFeature) {
            serializer = GsonSerializer()
        }
    }
    val msgToSend = LinkedList<JSONObject>()
    val msgReceived = Channel<JSONObject>()

    suspend fun initialize(onConnectionEstablished: () -> Unit) = coroutineScope<Unit> {
        websocketHttpClient.webSocket(
            method = HttpMethod.Get,
            host = "localhost",
            port = 9000,
            path = "/plop"
        ) {
            val messageOutputRoutine = launch { outputMessages() }
            val userInputRoutine = launch { inputMessages() }

            /*val firstMsg=runBlocking { msgReceived.receive()}
            if(firstMsg.getString("type").equals(Constants.CONNECTION_INIT_MESSAGE)){
                onConnectionEstablished()
            }*/
            userInputRoutine.join() // Wait for completion; either "exit" or error

            messageOutputRoutine.cancelAndJoin()
        }
    }

    fun sendMessage(msg: JSONObject) {
        msgToSend.addFirst(msg)
        println(msgToSend.size)
    }

    suspend fun lastReceived(): JSONObject {
        return msgReceived.receive()
    }

    private suspend fun DefaultClientWebSocketSession.outputMessages() {
        while (true) {
            try {
                for (message in incoming) {
                    message as? Frame.Text ?: continue
                    msgReceived.send(JSONObject(message.readText()))
                }
            } catch (e: Exception) {
                println("Error while receiving: " + e.localizedMessage)
            }
        }
    }
    private var useful=-1138
    private suspend fun DefaultClientWebSocketSession.inputMessages() {
        while (true) {
            runBlocking {  }
            if (msgToSend.isNotEmpty()) {
                val msg =  msgToSend.removeLast()
                if (msg.equals(SimpleMessage("exit"))) break
                try {
                    send(msg.toString())
                } catch (e: Exception) {
                    println("Error while sending: " + e.localizedMessage)
                    return
                }
            }
        }
        println("joined")
    }
}

data class SimpleMessage(
    val type: String= Constants.CONNECTION_INIT_MESSAGE
)

data class PlayerInitialization(
    val type: String="player",
    val username:String,
    val deckType:JSONObject
)