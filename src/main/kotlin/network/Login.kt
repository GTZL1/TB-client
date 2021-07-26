package network

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import game.cards.types.CardType
import game.decks.DeckType
import game.powers.powersList
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import theme.menuFont
import theme.quantityFont
import kotlin.reflect.KClass
import kotlin.reflect.full.findParameterByName

class Login(
    private val idSession: MutableState<Int>,
    private val playerPseudo: MutableState<String>,
    private val onRightLogin: (() -> Unit),
    private val httpClient: HttpClient,
    private val serverUrl: MutableState<String>,
    private val serverPort: String = Constants.SERVER_PORT
) {
    @Composable
    fun LoginScreen(
        modifier: Modifier = Modifier,
    ) {
        val scope = rememberCoroutineScope()
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            val password = remember { mutableStateOf(("")) }
            val errorText = remember { mutableStateOf("")}

            Column(modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally) {
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
                        scope.launch { sendLoginForm(
                            username = playerPseudo.value,
                            password = password.value,
                            onRightLogin = onRightLogin,
                            idSession = idSession,
                            playerPseudo = playerPseudo,
                            errorText = errorText,
                        ) }
                    }) {
                    Text(text = "Login")
                }
            }

            Row(modifier = Modifier.align(Alignment.TopEnd)
                .padding(10.dp),
                verticalAlignment = Alignment.Bottom){
                Text(text = "Server URL",
                    modifier = Modifier.padding(end = 5.dp),
                    style = quantityFont)
                TextField(
                    value = serverUrl.value,
                    modifier = Modifier.width(150.dp),
                    onValueChange = { serverUrl.value = it },
                )
            }
        }
    }

    private suspend fun sendLoginForm(
        username: String,
        password: String,
        onRightLogin: (() -> Unit),
        idSession: MutableState<Int>,
        playerPseudo: MutableState<String>,
        errorText: MutableState<String>
    ) {
        try {
            val response = httpClient.request<LoginResponse> {
                    url("http://"+serverUrl.value+":"+serverPort+"/login")
                    headers {
                        append("Content-Type", "application/json")
                    }
                    body = LoginRequest(username, password)
                    method = HttpMethod.Get
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
                    url("http://"+serverUrl.value+":"+serverPort+"/logout")
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

    private suspend fun cardsRequest(): JSONObject {
        try {
            val response = JSONObject(
                httpClient.request<String> {
                    url("http://"+serverUrl.value+":"+serverPort+"/cards")
                    headers {
                        append("Content-Type", "application/json")
                    }
                    body = GameObjectsRequest(idSession.value)
                    method = HttpMethod.Get
                }
            )
            return response
        } catch (exception: ClientRequestException) {
        }
        return JSONObject()
    }

    private suspend fun decksRequest(): JSONArray {
        try {
            val response = JSONArray(
                    httpClient.request<String> {
                        url("http://"+serverUrl.value+":"+serverPort+ "/decks")
                        headers {
                            append("Content-Type", "application/json")
                        }
                        body = GameObjectsRequest(idSession.value)
                        method = HttpMethod.Get
                    }
                )
            return response
        } catch (exception: ClientRequestException) {
        }
        return JSONArray()
    }

    suspend fun generateCardTypes(typesConstructs: List<Pair<String, KClass<out CardType>>>): List<CardType> {
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

    suspend fun generateDecks(cardTypes: List<CardType>, decksList: JSONArray? = null): List<DeckType> {
        val playerDecks = decksList ?: decksRequest()
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
    val version: String = Constants.CLIENT_VERSION
)

data class LoginResponse(
    val granted: Boolean,
    val idSession: Int,
    val type: String = ""
)

data class GameObjectsRequest(
    val idSession: Int
)
