package game.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
        val turn = notifyChangeTurn(game)
        //println(turn.value)
        if(!turn){
            game.cardToOpponentRow(handCards.maxByOrNull { playCard: PlayCard -> playCard.cardType.attack }!!)
            game.startPlayerTurn()
        }
        //println(turn.value)
    }
}