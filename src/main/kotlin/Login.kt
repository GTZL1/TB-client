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
import androidx.compose.ui.unit.dp
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking

@Composable
fun Login(
    modifier: Modifier = Modifier,
    client: HttpClient,
    onRightLogin: (() -> Unit),
    idSession: MutableState<Int>
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        var username = remember { mutableStateOf(("player")) }
        var password = remember { mutableStateOf(("1234")) }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TextField(
                value = username.value,
                modifier = modifier,
                onValueChange = { username.value = it },
                label = { Text("Pseudo") }
            )
            TextField(
                value = password.value,
                modifier = modifier,
                onValueChange = { password.value = it },
                label = { Text("Password") }
            )
            Button(modifier = modifier.padding(top = 20.dp),
                onClick = {
                    sendLoginForm(
                        client = client,
                        username = username.value,
                        password = password.value,
                        onRightLogin = onRightLogin,
                        idSession = idSession
                    )
                }) {
                Text(text = "Login")
            }
        }
    }
}

private fun sendLoginForm(
    client: HttpClient, username: String, password: String, onRightLogin: (() -> Unit),
    idSession: MutableState<Int>
) {
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

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val granted: Boolean,
    val idSession: Int
)
