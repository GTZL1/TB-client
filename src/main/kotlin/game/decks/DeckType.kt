package game.decks

import game.cards.plays.PlayCard
import game.cards.types.CardType
import org.json.JSONArray
import org.json.JSONObject
import java.util.ArrayList

class DeckType(val id: Long, var name: String, var cardTypes: Map<CardType, Short>) {
    fun generatePlayDeck(playerName:String):PlayDeck{
        val deck= ArrayList<PlayCard>()
        var id=-1
        cardTypes.forEach { ct, s ->
            for(x in 0 until s){
                deck.add(ct.generatePlayCard(playerName, ++id))
            }
        }
        return PlayDeck(name, deck, id)
    }

    fun serialize():JSONObject{
        val cards:JSONArray= JSONArray()
        cardTypes.forEach { ct, s -> cards.put(JSONObject().put("name",ct.name).put("quantity", s)) }
        return JSONObject().put("id", id).put("name", name).put("cards", cards)
    }
}