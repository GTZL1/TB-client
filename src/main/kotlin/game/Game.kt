package game

import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.util.*

class Game(val date: Date, val httpClient: HttpClient) {

    fun generateCardTypes(){
            try {
                val response = runBlocking {
                    httpClient.request<String> {
                        url("http://localhost:9000/cards")
                        headers {
                            append("Content-Type", "application/json")
                        }
                        body = GameObjectsRequest(13)
                        method = HttpMethod.Get
                    }
                }
               System.out.println(JSONObject(response))
            } catch (exception: ClientRequestException) {}
    }
}

data class GameObjectsRequest(
    val idSession: Int
)