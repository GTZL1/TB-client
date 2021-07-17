import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import io.mockk.*;
import game.*
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
import kotlinx.coroutines.runBlocking
import network.Login
import network.PlayerInitialization
import network.SimpleMessage
import network.WebSocketHandler
import org.json.JSONObject
import java.time.LocalDateTime
import kotlin.reflect.KClass

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    System.setProperty("skiko.renderApi", "OPENGL")
    val httpClient = HttpClient {
        install(WebSockets)
        install(JsonFeature) {
            serializer = GsonSerializer()
        }
    }

    val cardClasses = listOf<Pair<String, KClass<out CardType>>>(
        Pair("hero", HeroCardType::class),
        Pair("unit", UnitCardType::class),
        Pair("vehicle", VehicleCardType::class),
        Pair("spy", SpyCardType::class),
        Pair("base", BaseCardType::class)
    )

    lateinit var cardTypes: List<CardType>
    lateinit var login: Login

    application {
        val state = rememberWindowState(
            size = WindowSize(Constants.WINDOW_WIDTH.dp, Constants.WINDOW_HEIGHT.dp),
            position = WindowPosition(x = 200.dp, y = 35.dp)
        )
        val screenState = remember { mutableStateOf(Screen.LOGIN) }
        val game = remember { mutableStateOf<Game?>(null) }
        Window(
            title = "uPCb !!",
            state = state,
            resizable = true,
            onCloseRequest = {
                if(screenState.value == Screen.BOARD) {
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
                    playerPseudo = username
                )
            }

            val websocket = mockk<WebSocketHandler>(relaxUnitFun = true)
            coEvery { websocket.receiveOne() } returns JSONObject(SimpleMessage(Constants.CONNECTION_INIT_MESSAGE))
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
                            onBack = { screenState.value = Screen.INTERMEDIATE }
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
                            cardTypes = cardTypes,
                            decksList = login.generateDecks(cardTypes)
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
                        launch {
                            websocket.initialize {
                                run {
                                    game.value = null
                                    screenState.value = Screen.DECK
                                }
                            }
                        }
                        val player = Player(
                            pseudo = username.value,
                            deckType = playerDeck.value!!
                        )
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
                        val opponentDeck = websocket.receiveOne()
                        val g = Game(
                            date = LocalDateTime.now(),
                            webSocketHandler = websocket,
                            httpClient = httpClient,
                            idSession = idSession,
                            cardTypes = cardTypes,
                            player = player,
                            opponent = PlayerIA(cardTypes),
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
                        (currentGame.opponent as PlayerIA).play(currentGame)
                        Board(currentGame)
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


