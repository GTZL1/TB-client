package game

import androidx.compose.runtime.MutableState
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.util.*

class Game(val date: Date, val httpClient: HttpClient) {

    private fun generateCardTypes(){
            try {
                val response = runBlocking {
                    client.request<LoginResponse> {
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
                    onRightLogin()
                }
            } catch (exception: ClientRequestException) {}
    }
}