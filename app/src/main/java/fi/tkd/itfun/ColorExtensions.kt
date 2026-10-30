package fi.tkd.itfun

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

private const val DEFAULT_DARKEN_PERCENT = 30f

fun Color.darker(percent: Float = DEFAULT_DARKEN_PERCENT): Color {
    return lerp(this, Color.Black, percent / 100)
}
