import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.gesture.ExperimentalPointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

data class Card(val name: String, val life: Int, val attack:Int){
    var health=this.life

    fun takeDamage(damage:Int){
        health-=damage
    }

    fun get():Card{
        return this
    }

    @ExperimentalPointerInput
    @Composable
    fun displayCard(modifier: Modifier=Modifier,
    onDragEnd: ()->Unit) {
        var offsetX by remember { mutableStateOf(0f) }
        var offsetY by remember { mutableStateOf(0f) }
        Box(modifier = modifier
            .offset(offsetX.dp, offsetY.dp)
            .clickable(enabled = true, onClick = {})
            .width(100.dp).height(180.dp)
            .clip(shape = RoundedCornerShape(5.dp))
            .background(color = Color.Gray)
            .border(width = 2.dp, color = Color.Red, shape = RoundedCornerShape(5.dp))
            .pointerInput {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consumeAllChanges()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    },
                    onDragEnd = {
                         onDragEnd()
                            //handCards.remove(card)
                        })
                    },
                ) {
            Column(modifier = Modifier.fillMaxSize(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ){
                Box(modifier = Modifier
                    .weight(2.5f)
                    .fillMaxSize()
                    .clip(shape = RoundedCornerShape(topLeft = 5.dp, topRight = 5.dp, bottomLeft = 0.dp, bottomRight = 0.dp))
                    .background(Color.White))
                {}
                Box(modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    //.clip(shape = RoundedCornerShape(topLeft = 5.dp, topRight = 5.dp, bottomLeft = 0.dp, bottomRight = 0.dp))
                    .background(Color.Green))
                {}
            }
        }
    }

}