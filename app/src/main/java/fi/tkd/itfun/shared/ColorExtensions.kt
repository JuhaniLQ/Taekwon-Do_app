package fi.tkd.itfun.shared

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

private const val DEFAULT_DARKEN_PERCENT = 30f

fun Color.darker(percent: Float = DEFAULT_DARKEN_PERCENT): Color {
    return lerp(this, Color.Black, percent / 100)
}

private const val DEFAULT_OPACITY_PERCENT = 30f

fun Color.opacity(percent: Float = DEFAULT_OPACITY_PERCENT): Color {
    return copy(alpha = (percent / 100f).coerceIn(0f, 1f))
}