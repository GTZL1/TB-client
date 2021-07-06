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
            port = System.getenv("TB_SERVER_PORT").toInt(),
            path = "/plop"
        ) {
            val messageOutputRoutine = launch { outputMessages() }
            val userInputRoutine = launch { inputMessages() }

            userInputRoutine.join() // Wait for completion; either "exit" or error
            messageOutputRoutine.cancelAndJoin()

            onConnectionEstablished()
        }
    }

    fun sendMessage(msg: JSONObject) {
        msgToSend.trySend(msg)
    }

    suspend fun receiveOne(): JSONObject {
        return msgReceived.receive()
    }

    private suspend fun DefaultClientWebSocketSession.outputMessages() {
        try {
            for (message in incoming) {
                message as? Frame.Text ?: continue
                if(message.readText().equals(Constants.EXIT_MESSAGE)){
                    sendMessage(JSONObject(SimpleMessage(message.readText())))
                } else {
                    msgReceived.send(JSONObject(message.readText()))
                }
            }
        } catch (e: Exception) {
            println("Error while receiving: " + e.localizedMessage)
        }
    }

    private suspend fun DefaultClientWebSocketSession.inputMessages() {
        for (msg in msgToSend) {
            if (msg.getString("type").equals(Constants.EXIT_MESSAGE)) break
            try {
                send(msg.toString())
            } catch (e: Exception) {
                println("Error while sending: " + e.localizedMessage)
            }
        }
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
data class CardMovement(
    val type: String=Constants.CARD_MOVEMENT,
    val owner: String,
    val id: Int,
    val position: Position,
    val fromDeck: Boolean
)
data class CardAttack(
    val type: String=Constants.CARD_ATTACK,
    val attackerOwner: String,
    val attackerId: Int,
    val targetOwner: String,
    val targetId: Int,
    val specialPower: Boolean = false
)