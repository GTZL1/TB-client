package game

import game.cards.plays.PlayCard
import game.cards.types.CardType
import game.decks.Deck
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
import kotlin.reflect.full.findParameterByName

class Game(val date: Date, val httpClient: HttpClient, private val idSession: Int) {


}