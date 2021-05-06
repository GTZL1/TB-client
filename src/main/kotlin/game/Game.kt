package game

import game.cards.types.CardType
import game.powers.Power
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.reflect.KClass

class Game(val date: Date, val httpClient: HttpClient) {

    private fun cardsRequest(): JSONObject {
        try {
            val response = JSONObject(runBlocking {
                httpClient.request<String> {
                    url("http://localhost:9000/cards")
                    headers {
                        append("Content-Type", "application/json")
                    }
                    body = GameObjectsRequest(1)
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
                    body = GameObjectsRequest(1)
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
                        tc.second.constructors.first().call(
                            card.getString("name"),
                            card.getInt("lifePoints"),
                            card.getInt("attackPoints"),
                            card.getInt("maxNumberInDeck")
                        )
                    }
                )
            }
        }
        return cardTypes
    }
}

data class GameObjectsRequest(
    val idSession: Int
)