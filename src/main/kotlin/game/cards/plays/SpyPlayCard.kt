package game.cards.plays

import game.cards.types.CardType

class SpyPlayCard(cardType: CardType, player:String): UnitPlayCard(cardType, player) {
    fun changeOwner(newPlayer:String){
        owner=newPlayer
    }
}