package game.player

import androidx.compose.runtime.*
import game.Game
import game.Position
import game.cards.plays.PlayCard
import game.cards.types.CardType
import game.decks.DeckType
import game.notifyChangeTurn

class PlayerIA(cardTypes: List<CardType>) : Player(
    pseudo = "ANNA",
    deckType = DeckType(
        id = -1,
        name = "default",
        cardTypes = cardTypes.associateWith { 1.toShort() })
) {
    private val handCards = mutableStateListOf<PlayCard>()

    init {
        playDeck.drawHand().forEach { pc: PlayCard ->
            handCards.add(pc.cardType.generatePlayCard(pc.owner, pc.id))
            handCards.last().changePosition(Position.HAND)
        }
    }

    @Composable
    fun play(game: Game) {
        val fromHand = remember { mutableStateOf(true) }
        LaunchedEffect(notifyChangeTurn(game)){
            fromHand.value=game.movableFromHand(Position.OPPONENT)
            if (!game.playerTurn) {
                while (fromHand.value && handCards.isNotEmpty()) {
                    val cardToPlay = cardSelector(handCards)
                    game.cardToOpponentRow(cardToPlay)
                    handCards.remove(cardToPlay)
                    fromHand.value = game.movableFromHand(Position.OPPONENT)
                }
                game.startPlayerTurn()
            }
        }
    }

    private fun cardSelector(cards: List<PlayCard>): PlayCard {
        return cards.maxByOrNull { playCard: PlayCard -> playCard.cardType.attack }!!
    }
}