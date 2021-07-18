package game.player

import androidx.compose.runtime.*
import game.Game
import game.Position
import game.cards.plays.PlayCard
import game.cards.types.CardType
import game.cards.types.HeroCardType
import game.cards.types.VehicleCardType
import game.decks.DeckType
import game.notifyChangeTurn

class PlayerIA(cardTypes: List<CardType>) : Player(
    pseudo = "ANNA",
    deckType = DeckType(
        id = -1,
        name = "default",
        cardTypes = createIADeck(cardTypes.toMutableList()))//cardTypes.associateWith { 1.toShort() })
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
                    val vehicles = vehicles(handCards)
                    while(vehicles.isNotEmpty() && fromHand.value){
                        val cardToPlay = vehicles.removeFirst()
                        game.cardToCenterRow(cardToPlay)
                        handCards.remove(cardToPlay)
                        fromHand.value = game.movableFromHand(Position.OPPONENT)
                    }
                    println(fromHand.value)
                    val heroes = heroes(handCards)
                    while(heroes.isNotEmpty() && fromHand.value){
                        val cardToPlay = heroes.removeFirst()
                        game.cardToOpponentRow(cardToPlay)
                        handCards.remove(cardToPlay)
                        fromHand.value = game.movableFromHand(Position.OPPONENT)
                    }
                    if(fromHand.value) {
                        val cardToPlay = powerfulCards(handCards)
                        game.cardToOpponentRow(cardToPlay)
                        handCards.remove(cardToPlay)
                    }
                    fromHand.value = game.movableFromHand(Position.OPPONENT)
                }
                game.startPlayerTurn()
            }
        }
    }

    private fun powerfulCards(cards: List<PlayCard>): PlayCard {
        return cards.maxByOrNull { playCard: PlayCard -> playCard.cardType.attack }!!
    }

    private fun vehicles(cards: List<PlayCard>): MutableList<PlayCard> {
        return try {
            cards.filter { playCard: PlayCard -> playCard.cardType::class == VehicleCardType::class }.toMutableList()
        } catch (exception: NoSuchElementException) {
            mutableListOf()
        }
    }

    private fun heroes(cards: List<PlayCard>): MutableList<PlayCard> {
        return try {
            cards.filter { playCard: PlayCard -> playCard.cardType::class == HeroCardType::class }.toMutableList()
        } catch (exception: NoSuchElementException) {
            mutableListOf()
        }
    }
}

private fun createIADeck(cardTypes: MutableList<CardType>): Map<CardType, Short>{
    val baseCards= cardTypes.filter { cardType: CardType -> cardType.name == "Base 2" }
    val powerfulCards = cardTypes.filter { cardType: CardType -> cardType.attack >= 5 }
    val otherCards = cardTypes.filter { cardType: CardType -> cardType.attack in 1..4 }.toMutableList()
    val deck = powerfulCards.associateWith { cardType: CardType -> cardType.maxNumberInDeck.toShort() }.toMutableMap()
    deck += baseCards.associateWith { cardType: CardType -> cardType.maxNumberInDeck.toShort() }

    while((deck.map{ (_, qty) -> qty}.sum() < Constants.MINIMAL_DECK_QUANTITY) && otherCards.isNotEmpty()){
        val card= otherCards.removeFirst()
        deck[card] = card.maxNumberInDeck.toShort()
    }

    return deck
}