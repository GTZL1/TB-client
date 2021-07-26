import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import game.Board
import game.EndingScreen
import game.Game
import game.IntermediateScreen
import game.cards.types.*
import game.decks.DeckGUI
import game.decks.DeckScreen
import game.decks.DeckType
import game.player.Player
import game.player.PlayerIA
import game.screens.GameHistory
import game.screens.HistoryScreen
import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.websocket.*
import kotlinx.coroutines.launch
import network.Login
import network.PlayerInitialization
import network.SimpleMessage
import network.WebSocketHandler
import org.json.JSONObject
import java.time.LocalDateTime
import kotlin.reflect.KClass

/**
 * Manage states of the app
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    System.setProperty("skiko.renderApi", "OPENGL")
    val httpClient = HttpClient {
        install(WebSockets)
        install(JsonFeature) {
            serializer = GsonSerializer()
        }
    }

    //used to assign a constructor to a PlayCard subclass
    val cardClasses = listOf<Pair<String, KClass<out CardType>>>(
        Pair("hero", HeroCardType::class),
        Pair("unit", UnitCardType::class),
        Pair("vehicle", VehicleCardType::class),
        Pair("spy", SpyCardType::class),
        Pair("base", BaseCardType::class)
    )

    lateinit var cardTypes: List<CardType>
    lateinit var login: Login

    // In application, it is a Composable scope
    application {
        val state = rememberWindowState(
            size = WindowSize(Constants.WINDOW_WIDTH.dp, Constants.WINDOW_HEIGHT.dp),
            position = WindowPosition(x = 200.dp, y = 35.dp)
        )
        val screenState = remember { mutableStateOf(Screen.LOGIN) }
        val game = remember { mutableStateOf<Game?>(null) }
        val ia = remember { mutableStateOf(false) }

        val serverUrl = remember { mutableStateOf("localhost") }

        Window(
            title = "uPCb !!",
            state = state,
            resizable = true,
            onCloseRequest = {
                if(screenState.value == Screen.BOARD && game.value != null) {
                    game.value!!.sendDefeat()
                }
                login.logout()
                state.isOpen = false
            }
        ) {
            val playerDeck = remember { mutableStateOf<DeckType?>(null) }
            val deckGUI = remember { mutableStateOf<DeckGUI?>(null) }
            val gameHistory = remember { mutableStateOf<GameHistory?>(null) }
            val idSession = remember { mutableStateOf(0) }
            val username = remember { mutableStateOf("aloy") }
            val opponentName = remember { mutableStateOf("ikrie") }
            val victory = remember { mutableStateOf(false) }

            login = remember {
                Login(
                    httpClient = httpClient,
                    onRightLogin = { screenState.value = Screen.INTERMEDIATE },
                    idSession = idSession,
                    playerPseudo = username,
                    serverUrl = serverUrl,
                )
            }

            //switches between different screen states
            when (screenState.value) {
                Screen.LOGIN -> {
                    login.LoginScreen()
                }
                Screen.INTERMEDIATE -> {
                    IntermediateScreen(
                        username = username.value,
                        onDeckScreen = { screenState.value = Screen.DECK },
                        onGameHistory = { screenState.value = Screen.HISTORY },
                        onQuitGame = {
                            login.logout()
                            state.isOpen = false
                        }
                    )
                }
                Screen.HISTORY -> {
                    LaunchedEffect(true) {
                        gameHistory.value = GameHistory(
                            idSession = idSession.value,
                            httpClient = httpClient,
                            username = username.value,
                            onBack = { screenState.value = Screen.INTERMEDIATE },
                            serverUrl = serverUrl,
                        )
                    }
                    val currentGameHistory = gameHistory.value
                    if (currentGameHistory != null) {
                        HistoryScreen(
                            gameHistory = remember { currentGameHistory },
                        )
                    }
                }
                Screen.DECK -> {
                    LaunchedEffect(true) {
                        cardTypes = login.generateCardTypes(cardClasses)
                        val dG = DeckGUI(
                            idSession = idSession,
                            httpClient = httpClient,
                            serverUrl = serverUrl,
                            cardTypes = cardTypes,
                            decksList = login.generateDecks(cardTypes),
                            playIA = ia
                        )
                        deckGUI.value = dG
                    }
                    val currentDeckGUI = deckGUI.value
                    if (currentDeckGUI != null) {
                        DeckScreen(deckGUI = remember { currentDeckGUI },
                            onSelect = { deck: DeckType ->
                                playerDeck.value = deck
                                screenState.value = Screen.BOARD
                            },
                            onBack = { screenState.value = Screen.INTERMEDIATE })
                    }
                }
                Screen.BOARD -> {
                    LaunchedEffect(true) {
                        val websocket = WebSocketHandler(serverUrl)
                        val player = Player(
                            pseudo = username.value,
                            deckType = playerDeck.value!!
                        )
                        if(!ia.value){
                            launch {
                                websocket.initialize {
                                    run {
                                        if(game.value != null) game.value!!.stopGame()
                                        game.value = null
                                        screenState.value = Screen.DECK
                                    }
                                }
                            }
                            websocket.sendMessage(JSONObject(SimpleMessage(Constants.CONNECTION_INIT_MESSAGE)))
                            websocket.receiveOne()
                            websocket.sendMessage(
                                JSONObject(
                                    PlayerInitialization(
                                        username = username.value,
                                        deckType = player.deckType.serializeBases()
                                    )
                                )
                            )
                        }
                        //not used if fighting IA
                        val opponentInfos = if(!ia.value) websocket.receiveOne() else JSONObject()

                        val g = Game(
                            date = LocalDateTime.now(),
                            webSocketHandler = websocket,
                            httpClient = httpClient,
                            serverUrl = serverUrl,
                            idSession = idSession,
                            cardTypes = cardTypes,
                            player = player,
                            opponent = if(!ia.value) {
                                            Player(
                                                pseudo = opponentInfos.getString("username"),
                                                deckType = login.generateDeck(
                                                    cardTypes,
                                                    opponentInfos.getJSONObject("deckType"))
                                            )
                                        } else {
                                            PlayerIA(cardTypes)
                                        },
                            playIA = ia.value,
                            onEnding = { oppName: String, vic: Boolean ->
                                opponentName.value = oppName
                                victory.value = vic
                                game.value = null
                                screenState.value = Screen.ENDING
                            }
                        )
                        g.determineFirst()
                        game.value = g
                    }
                    val currentGame = game.value
                    if (currentGame != null) {
                        Board(game = currentGame,
                                playIA = ia.value)
                        if(ia.value) (currentGame.opponent as PlayerIA).play(currentGame)
                    }
                }
                Screen.ENDING -> {
                    EndingScreen(playerName = username.value,
                        opponentName = opponentName.value,
                        victory = victory.value,
                        onIntermediateScreen = { screenState.value = Screen.INTERMEDIATE },
                        onGameAgain = { screenState.value = Screen.BOARD })
                }
            }
        }
    }
}

enum class Screen {
    LOGIN, INTERMEDIATE, BOARD, DECK, ENDING, HISTORY
}


