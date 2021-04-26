package game

import game.cards.types.*
import game.powers.Power
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList

class Game(val date: Date, val httpClient: HttpClient) {

    fun cardsRequest(){
            try {
                val response = JSONObject(runBlocking {
                    httpClient.request<String> {
                        url("http://localhost:9000/cards")
                        headers {
                            append("Content-Type", "application/json")
                        }
                        body = GameObjectsRequest(13)
                        method = HttpMethod.Get
                    }
                })
                generateCardTypes(response)
            } catch (exception: ClientRequestException) {}
    }

    private fun powersRequest(): JSONArray {
        try {
            val response = JSONArray(runBlocking {
                httpClient.request<String> {
                    url("http://localhost:9000/powers")
                    headers {
                        append("Content-Type", "application/json")
                    }
                    body = GameObjectsRequest(13)
                    method = HttpMethod.Get
                }
            })
            return response
        } catch (exception: ClientRequestException) {}
        return JSONArray()
    }

    fun generatePowerTypes(): ArrayList<Power>{
        var powersList:ArrayList<Power> = ArrayList()
        val powers=powersRequest()
        for (x in 0 until powers.length()) {
            powersList.add(Power(powers.getJSONObject(x).getInt("idPower"),
                powers.getJSONObject(x).getString("name")))
        }
        return powersList
    }

    private fun generateCardTypes(cards: JSONObject): ArrayList<CardType>{
        var cardTypes: ArrayList<CardType> = ArrayList()
        val powers=generatePowerTypes()
        for (x in 0 until cards.getJSONArray("hero").length()) {
            val card:JSONObject=cards.getJSONArray("hero").getJSONObject(x)
            cardTypes.add(HeroCardType(card.getString("name"),
                card.getInt("lifePoints"),
                card.getInt("attackPoints"),
                card.getInt("maxNumberInDeck"),
                Power(powers.find { power: Power -> power.id==card.getInt("idxPower") }!!.id,
                    powers.find { power: Power -> power.id==card.getInt("idxPower") }!!.name)
            ))
        }
        System.out.println(cardTypes)
        return cardTypes
    }
}

data class GameObjectsRequest(
    val idSession: Int
)