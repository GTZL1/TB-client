import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import game.cards.plays.*
import game.cards.types.*
import game.decks.*
import game.powers.*
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.util.ArrayList
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
            //var username = remember { mutableStateOf(("")) }
            val password = remember { mutableStateOf(("")) }

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
                Button(modifier = modifier.padding(top = 20.dp),
                    onClick = {
                        sendLoginForm(
                            username = playerPseudo.value,
                            password = password.value,
                            onRightLogin = onRightLogin,
                            idSession = idSession,
                            playerPseudo = playerPseudo
                        )
                    }) {
                    Text(text = "Login")
                }
            }
        }
    }

    private fun sendLoginForm(
        username: String, password: String, onRightLogin: (() -> Unit),
        idSession: MutableState<Int>, playerPseudo: MutableState<String>,
    ) {
        try {
            val response = runBlocking {
                httpClient.request<LoginResponse> {
                    url("http://localhost:9000/login")
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
        }
    }

    private fun cardsRequest(): JSONObject {
        try {
            val response = JSONObject(runBlocking {
                httpClient.request<String> {
                    url("http://localhost:9000/cards")
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
                    url("http://localhost:9000/powers")
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
            val response = JSONArray(runBlocking {
                httpClient.request<String> {
                    url("http://localhost:9000/decks")
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

    fun generatePowerTypes(): ArrayList<Power> {
        var powersList: ArrayList<Power> = ArrayList()
        val powers = powersRequest()
        for (x in 0 until powers.length()) {
            powersList.add(
                Power(
                    powers.getJSONObject(x).getInt("idPower"),
                    powers.getJSONObject(x).getString("name")
                )
            )
        }
        return powersList
    }

    fun generateCardTypes(typesConstructs: List<Pair<String, KClass<out CardType>>>): List<CardType> {
        val cardTypes = mutableListOf<CardType>()
        val cards = cardsRequest()
        val powers = generatePowerTypes()

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
                            Power(
                                powers.find { power: Power -> power.id == card.getInt("idxPower") }!!.id,
                                powers.find { power: Power -> power.id == card.getInt("idxPower") }!!.name
                            )
                        )
                    } else {
                        val constructor=tc.second.constructors.first()
                        val args=mapOf((constructor.findParameterByName("name")!! to card.getString("name")),
                            (constructor.findParameterByName("life")!! to card.getInt("lifePoints")),
                            (constructor.findParameterByName("attack")!! to card.getInt("attackPoints")),
                            (constructor.findParameterByName("maxNumberInDeck")!! to card.getInt("maxNumberInDeck")))
                        constructor.callBy(args)
                    }
                )
            }
        }
        return cardTypes
    }

    fun generateDeck(cardTypes: List<CardType>): Deck {
        var deck= ArrayList<PlayCard>()
        val playerDeck: JSONObject =decksRequest().getJSONObject(0)
        for(x in 0 until playerDeck.getJSONArray("cards").length()){
            val currentJsonCard= playerDeck.getJSONArray("cards").getJSONObject(x)
            val cardType=cardTypes.filter { ct -> ct.name.equals(
                currentJsonCard.getString("name")) }.first()

            for(y in 0 until currentJsonCard.getInt("quantity")){
                deck.add(cardType.playType.constructors.first().call(cardType))
            }
        }
        return Deck(playerDeck.getString("name"), deck)
    }
}

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val granted: Boolean,
    val idSession: Int
)

data class GameObjectsRequest(
    val idSession: Int
)
