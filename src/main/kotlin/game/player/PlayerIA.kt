package game.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import game.Game
import game.Position
import game.cards.plays.PlayCard
import game.cards.types.CardType
import game.decks.DeckType
import game.notifyChangeTurn

class PlayerIA(cardTypes: List<CardType>) : Player(pseudo = "JARVIS",
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
    fun play(game: Game, ){
        if(!notifyChangeTurn(game = game)){
            while (game.movableFromHand(Position.OPPONENT) && handCards.isNotEmpty()) {
                val cardToPlay= cardSelector(handCards)
                game.cardToOpponentRow(cardToPlay)
                handCards.remove(cardToPlay)
            }

            game.startPlayerTurn()
        }
    }

    private fun cardSelector(cards: List<PlayCard>): PlayCard {
        return cards.maxByOrNull { playCard: PlayCard -> playCard.cardType.attack }!!
    }
}