package game.cards.plays

import game.cards.types.CardType

class SpyPlayCard(cardType: CardType, player:String, id:Int): UnitPlayCard(cardType, player, id) {
    fun changeOwner(newPlayer:String){
        owner=newPlayer
    }

    fun changeId(newId: Int){
        id=newId
    }
}