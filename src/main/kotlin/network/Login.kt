package network

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import game.cards.types.CardType
import game.decks.DeckType
import game.powers.Power
import game.powers.powersList
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import theme.miniFont
import theme.quantityFont
import kotlin.reflect.KClass
import kotlin.reflect.full.findParameterByName

class Login(
    private val idSession: MutableState<Int>,
    private val playerPseudo: MutableState<String>,
    private val onRightLogin: (() -> Unit),
    private val httpClient: HttpClient
) {
    @Composable
    fun LoginScreen(
        modifier: Modifier = Modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            val password = remember { mutableStateOf(("")) }
            val errorText = remember { mutableStateOf("")}

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TextField(
                    value = playerPseudo.value,
                    modifier = modifier,
                    onValueChange = { playerPseudo.value = it },
                    label = { Text("Pseudo") }
                )
                TextField(
                    value = password.value,
                    modifier = modifier,
                    onValueChange = { password.value = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation()
                )
                Text(text = errorText.value,
                    style = quantityFont,
                    color = Color.Red)
                Button(modifier = modifier.padding(top = 20.dp),
                    onClick = {
                        sendLoginForm(
                            username = playerPseudo.value,
                            password = password.value,
                            onRightLogin = onRightLogin,
                            idSession = idSession,
                            playerPseudo = playerPseudo,
                            errorText = errorText,
                        )
                    }) {
                    Text(text = "Login")
                }
            }
        }
    }

    private fun sendLoginForm(
        username: String,
        password: String,
        onRightLogin: (() -> Unit),
        idSession: MutableState<Int>,
        playerPseudo: MutableState<String>,
        errorText: MutableState<String>
    ) {
        try {
            val response = runBlocking {
                httpClient.request<LoginResponse> {
                    url(System.getenv("TB_SERVER_URL")+":"+System.getenv("TB_SERVER_PORT")+"/login")
                    headers {
                        append("Content-Type", "application/json")
                    }
                    body = LoginRequest(username, password)
                    method = HttpMethod.Get
                }
            }
            if (response.granted) {
                idSession.value = response.idSession
                playerPseudo.value = username
                onRightLogin()
            }
        } catch (exception: ClientRequestException) {
            errorText.value = runBlocking { exception.response.readText() }
        }
    }

    fun logout() {
        try {
            JSONObject(runBlocking {
                httpClient.request<String> {
                    url(System.getenv("TB_SERVER_URL")+":"+System.getenv("TB_SERVER_PORT")+"/logout")
                    headers {
                        append("Content-Type", "application/json")
                    }
                    body = GameObjectsRequest(idSession = idSession.value)
                    method = HttpMethod.Get
                }
            })
        } catch (exception: ClientRequestException) {
        }
    }

    private fun cardsRequest(): JSONObject {
        try {
            val response = JSONObject(runBlocking {
                httpClient.request<String> {
                    url(System.getenv("TB_SERVER_URL")+":"+System.getenv("TB_SERVER_PORT")+"/cards")
                    headers {
                        append("Content-Type", "application/json")
                    }
                    body = GameObjectsRequest(idSession.value)
                    method = HttpMethod.Get
                }
            })
            return response
        } catch (exception: ClientRequestException) {
        }
        return JSONObject()
    }

    private fun powersRequest(): JSONArray {
        try {
            val response = JSONArray(runBlocking {
                httpClient.request<String> {
                    url(System.getenv("TB_SERVER_URL")+":"+System.getenv("TB_SERVER_PORT")+"/powers")
                    headers {
                        append("Content-Type", "application/json")
                    }
                    body = GameObjectsRequest(idSession.value)
                    method = HttpMethod.Get
                }
            })
            return response
        } catch (exception: ClientRequestException) {
        }
        return JSONArray()
    }

    private fun decksRequest(): JSONArray {
        try {
            val response = JSONArray(runBlocking<String> {
                httpClient.request {
                    url(System.getenv("TB_SERVER_URL")+":"+System.getenv("TB_SERVER_PORT")+"/decks")
                    headers {
                        append("Content-Type", "application/json")
                    }
                    body = GameObjectsRequest(idSession.value)
                    method = HttpMethod.Get
                }
            })
            return response
        } catch (exception: ClientRequestException) {
        }
        return JSONArray()
    }

    fun generateCardTypes(typesConstructs: List<Pair<String, KClass<out CardType>>>): List<CardType> {
        val cardTypes = mutableListOf<CardType>()
        val cards = cardsRequest()

        for (tc: Pair<String, KClass<out CardType>> in typesConstructs) {
            for (x in 0 until cards.getJSONArray(tc.first).length()) {
                val card: JSONObject = cards.getJSONArray(tc.first).getJSONObject(x)
                cardTypes.add(
                    if (tc.first.equals("hero")) {
                        tc.second.constructors.first().call(
                            card.getString("name"),
                            card.getInt("lifePoints"),
                            card.getInt("attackPoints"),
                            card.getInt("maxNumberInDeck"),
                            powersList[card.getInt("idxPower")]!!.constructors.first().call()
                        )
                    } else {
                        val constructor=tc.second.constructors.first()
                        val args=mapOf((constructor.findParameterByName("name")!! to card.getString("name")),
                            (constructor.findParameterByName("life")!! to card.getInt("lifePoints")),
                            (constructor.findParameterByName("attack")!! to card.getInt("attackPoints")),
                            (constructor.findParameterByName("maxNumberInDeck")!! to card.getInt("maxNumberInDeck")),)
                        constructor.callBy(args)
                    }
                )
            }
        }
        return cardTypes
    }

    fun generateDeck(cardTypes: List<CardType>, playerDecks: JSONArray = decksRequest()): List<DeckType> {
        val decks= mutableListOf<DeckType>()

        for(y in 0 until playerDecks.length()){
            val deck=playerDecks.getJSONObject(y)
            decks.add(generateDeck(cardTypes, deck))
        }
        return decks.toList()
    }

    fun generateDeck(cardTypes: List<CardType>, playerDeck: JSONObject): DeckType {
        val deckType= mutableMapOf<CardType,Short>()

        for(x in 0 until playerDeck.getJSONArray("cards").length()){
            val currentJsonCard= playerDeck.getJSONArray("cards").getJSONObject(x)
            val cardType=cardTypes.filter { ct -> ct.name.equals(
                currentJsonCard.getString("name")) }.first()
            deckType[cardType] = currentJsonCard.getInt("quantity").toShort()
        }
        return (DeckType(playerDeck.getLong("id"), playerDeck.getString("name"), deckType.toMap()))
    }
}

data class LoginRequest(
    val username: String,
    val password: String,
    val version: Double = Constants.CLIENT_VERSION
)

data class LoginResponse(
    val granted: Boolean,
    val idSession: Int,
    val type: String = ""
)

data class GameObjectsRequest(
    val idSession: Int
)
