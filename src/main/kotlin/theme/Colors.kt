package theme

import androidx.compose.ui.graphics.Color
import game.cards.types.*
import kotlin.reflect.KClass

val Color.Companion.UnitBackgroundColor get() = Color(223, 176, 132)
val Color.Companion.BaseBackgroundColor get() = Color(172, 123, 87)
val Color.Companion.VehicleBackgroundColor get() = Color(68, 212, 0)
val Color.Companion.HeroBackgroundColor get() = Color(240, 213, 0)

val cardColors: Map<KClass<out CardType>, Color> = mapOf(
    HeroCardType::class to Color.HeroBackgroundColor,
    UnitCardType::class to Color.UnitBackgroundColor,
    VehicleCardType::class to Color.VehicleBackgroundColor,
    BaseCardType::class to Color.BaseBackgroundColor,
    SpyCardType::class to Color.UnitBackgroundColor)