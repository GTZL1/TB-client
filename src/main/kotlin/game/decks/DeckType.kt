package game.decks

import game.cards.plays.PlayCard
import game.cards.types.BaseCardType
import game.cards.types.CardType
import io.ktor.util.reflect.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.ArrayList

class DeckType(var id: Long, var name: String, var cardTypes: Map<CardType, Short>) {
    fun generatePlayDeck(playerName:String):PlayDeck{
        val deck= ArrayList<PlayCard>()
        var id=-1
        cardTypes.forEach { (ct, s) ->
            for(x in 0 until s){
                deck.add(ct.generatePlayCard(playerName, ++id))
            }
        }
        return PlayDeck(name, deck, id)
    }

    fun serialize(cardTypes: Map<CardType, Short> = this.cardTypes):JSONObject{
        val cards = JSONArray()
        cardTypes.forEach { (ct, s) -> cards.put(JSONObject().put("name",ct.name).put("quantity", s)) }
        return JSONObject().put("id", id).put("name", name).put("cards", cards)
    }

    fun serializeBases(): JSONObject {
        return serialize(cardTypes = cardTypes.filter { entry: Map.Entry<CardType, Short> -> entry.key.instanceOf(BaseCardType::class) })
    }
}