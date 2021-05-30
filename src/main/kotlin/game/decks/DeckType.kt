package game.decks

import game.cards.plays.PlayCard
import game.cards.types.CardType
import org.json.JSONArray
import org.json.JSONObject
import java.util.ArrayList

class DeckType(val name: String, val cardTypes: Map<CardType, Short>) {
    fun generatePlayDeck(playerName:String):PlayDeck{
        val deck= ArrayList<PlayCard>()
        cardTypes.forEach { ct, s ->
            for(x in 0 until s){
                deck.add(ct.generatePlayCard(playerName))
            }
        }
        return PlayDeck(name, deck)
    }

    fun serialize():JSONObject{
        val cards:JSONArray= JSONArray()
        cardTypes.forEach { ct, s -> cards.put(JSONObject().put("name",ct.name).put("quantity", s)) }
        return JSONObject().put("name", name).put("cards", cards)
    }
}