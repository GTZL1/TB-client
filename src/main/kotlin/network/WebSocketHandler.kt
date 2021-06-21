package network

import Constants
import game.Position
import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.websocket.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.json.JSONObject

class WebSocketHandler {
    val websocketHttpClient = HttpClient {
        install(WebSockets)
        install(JsonFeature) {
            serializer = GsonSerializer()
        }
    }
    val msgToSend = Channel<JSONObject>(Channel.UNLIMITED)
    val msgReceived = Channel<JSONObject>(Channel.UNLIMITED)

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
        msgToSend.trySend(msg)
        //println(msgToSend.size)
    }

    suspend fun receiveOne(): JSONObject {
        return msgReceived.receive()
    }

    private suspend fun DefaultClientWebSocketSession.outputMessages() {
        try {
            for (message in incoming) {
                message as? Frame.Text ?: continue
                msgReceived.send(JSONObject(message.readText()))
            }
        } catch (e: Exception) {
            println("Error while receiving: " + e.localizedMessage)
        }
    }

    private suspend fun DefaultClientWebSocketSession.inputMessages() {
        for (msg in msgToSend) {
            if (msg.equals(SimpleMessage("exit"))) break
            try {
                send(msg.toString())
            } catch (e: Exception) {
                println("Error while sending: " + e.localizedMessage)
            }
        }
        println("joined")
    }
}

data class SimpleMessage(
    val type: Any= Constants.CONNECTION_INIT_MESSAGE
)

data class PlayerInitialization(
    val type: String="player",
    val username:String,
    val deckType:JSONObject
)

data class CardMovement(
    val type: String=Constants.CARD_MOVEMENT,
    val owner: String,
    val id: Int,
    val position: Position
)