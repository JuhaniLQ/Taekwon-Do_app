package fi.tkd.itfun

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.min
import kotlin.math.min




private fun DrawScope.drawHorizontalNorm(
    y: Float,
    start: Float,
    length: Float,
    strokeWidthFrac: Float = 0.03f,
    color: Color = Color.Black
) {
    val w = size.width
    val h = size.height

    val yPx = h * y
    val startX = w * start
    val endX = w * (start + length)
    val strokeWidth = min(w, h) * strokeWidthFrac

    drawLine(
        color = color,
        start = Offset(startX, yPx),
        end = Offset(endX, yPx),
        strokeWidth = strokeWidth
    )
}

private fun DrawScope.drawVerticalNorm(
    x: Float,
    start: Float,
    length: Float,
    strokeWidthFrac: Float = 0.03f,
    color: Color = Color.Black
) {
    val w = size.width
    val h = size.height

    val xPx = w * x
    val startY = h * start
    val endY = h * (start + length)
    val strokeWidth = min(w, h) * strokeWidthFrac

    drawLine(
        color = color,
        start = Offset(xPx, startY),
        end = Offset(xPx, endY),
        strokeWidth = strokeWidth
    )
}

fun DrawScope.drawLetterNorm(
    text: String,
    x: Float,
    y: Float,
    textScale: Float = 0.16f
) {
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = size.minDimension * textScale
            color = android.graphics.Color.WHITE
            textAlign = android.graphics.Paint.Align.CENTER
        }
        val xPx = x.coerceIn(0f, 1f) * size.width
        val yPx = y.coerceIn(0f, 1f) * size.height

        canvas.nativeCanvas.drawText(text, xPx, yPx, paint)
    }
}

private fun DrawScope.drawFoot(
    center: Offset,
    width: Float,
    height: Float,
    color: Color = Color.White
) {
    drawRoundRect(
        color = color,
        topLeft = Offset(center.x - width / 2f, center.y - height / 2f),
        size = androidx.compose.ui.geometry.Size(width, height),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(width / 2f)
    )
}

private fun DrawScope.drawFootNorm(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    color: Color = Color.White
) {
    val cx = x.coerceIn(0f, 1f) * size.width
    val cy = y.coerceIn(0f, 1f) * size.height
    val wPx = width.coerceAtLeast(0f) * size.width
    val hPx = height.coerceAtLeast(0f) * size.height

    drawFoot(
        center = Offset(cx, cy),
        width = wPx,
        height = hPx,
        color = color
    )
}

enum class TkdPattern(
    val renderer: @Composable (Modifier) -> Unit
) {
    CHON_JI({ ChonJiPattern(it) }),
    DAN_GUN({ DanGunPattern(it) }),
    DO_SAN({ DoSanPattern(it) }),
    WON_HYO({ WonHyoPattern(it) }),
    YUL_GOK({ YulGokPattern(it) }),
    JOONG_GUN({ WonHyoPattern(it) }),
    UNKNOWN({ });

    companion object {
        private fun normalize(raw: String): String =
            raw.trim()
                .lowercase()
                .replace("""[\s_-]+""".toRegex(), "_")

        fun fromString(raw: String): TkdPattern {
            val key = normalize(raw)
            return entries.firstOrNull { normalize(it.name) == key } ?: UNKNOWN
        }
    }
}

@Composable
fun TkdPatternDiagram(
    name: String,
    modifier: Modifier = Modifier
) {
    TkdPattern.fromString(name).renderer(modifier)
}

@Composable
fun ChonJiPattern(
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        val boxSize: Dp = min(maxWidth, maxHeight)

        Canvas(
            modifier = Modifier
                .size(boxSize)
        ) {
            drawHorizontalNorm(y = 0.5f, start = 0.11f, length = 0.78f)
            drawVerticalNorm(x = 0.5f, start = 0.11f, length = 0.78f)
            drawLetterNorm("C", x = 0.5f, y = 0.1f)
            drawLetterNorm("D", x = 0.5f, y = 1f)
            drawLetterNorm("A", x = 0.05f, y = 0.55f)
            drawLetterNorm("B", x = 0.95f, y = 0.55f)
            drawFootNorm(x = 0.35f, y = 0.5f, width = 0.04f, height = 0.12f)
            drawFootNorm(x = 0.65f, y = 0.5f, width = 0.04f, height = 0.12f)
        }
    }
}

@Composable
fun DanGunPattern(
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        val boxSize: Dp = min(maxWidth, maxHeight)

        Canvas(
            modifier = Modifier
                .size(boxSize)
        ) {
            drawHorizontalNorm(y = 0.15f, start = 0.11f, length = 0.78f)
            drawHorizontalNorm(y = 0.85f, start = 0.11f, length = 0.78f)
            drawVerticalNorm(x = 0.5f, start = 0.15f, length = 0.7f)
            drawLetterNorm("C", x = 0.5f, y = 0.1f)
            drawLetterNorm("D", x = 0.5f, y = 1f)
            drawLetterNorm("A", x = 0.05f, y = 0.2f)
            drawLetterNorm("B", x = 0.95f, y = 0.2f)
            drawLetterNorm("E", x = 0.05f, y = 0.9f)
            drawLetterNorm("F", x = 0.95f, y = 0.9f)
            drawFootNorm(x = 0.35f, y = 0.15f, width = 0.04f, height = 0.12f)
            drawFootNorm(x = 0.65f, y = 0.15f, width = 0.04f, height = 0.12f)
        }
    }
}

@Composable
fun DoSanPattern(
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        val boxSize: Dp = min(maxWidth, maxHeight)

        Canvas(
            modifier = Modifier
                .size(boxSize)
        ) {
            drawHorizontalNorm(y = 0.15f, start = 0.485f, length = 0.4049f)
            drawHorizontalNorm(y = 0.85f, start = 0.11f, length = 0.4049f)
            drawVerticalNorm(x = 0.5f, start = 0.15f, length = 0.7f)
            drawLetterNorm("C", x = 0.5f, y = 0.1f)
            drawLetterNorm("D", x = 0.5f, y = 1f)
            drawLetterNorm("A", x = 0.05f, y = 0.2f)
            drawLetterNorm("B", x = 0.95f, y = 0.2f)
            drawLetterNorm("E", x = 0.05f, y = 0.9f)
            drawLetterNorm("F", x = 0.95f, y = 0.9f)
            drawFootNorm(x = 0.35f, y = 0.15f, width = 0.04f, height = 0.12f)
            drawFootNorm(x = 0.65f, y = 0.15f, width = 0.04f, height = 0.12f)
        }
    }
}

@Composable
fun WonHyoPattern(
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        val boxSize: Dp = min(maxWidth, maxHeight)

        Canvas(
            modifier = Modifier
                .size(boxSize)
        ) {
            drawHorizontalNorm(y = 0.15f, start = 0.11f, length = 0.78f)
            drawHorizontalNorm(y = 0.85f, start = 0.11f, length = 0.78f)
            drawVerticalNorm(x = 0.5f, start = 0.15f, length = 0.7f)
            drawLetterNorm("C", x = 0.5f, y = 0.1f)
            drawLetterNorm("D", x = 0.5f, y = 1f)
            drawLetterNorm("A", x = 0.05f, y = 0.2f)
            drawLetterNorm("B", x = 0.95f, y = 0.2f)
            drawLetterNorm("E", x = 0.05f, y = 0.9f)
            drawLetterNorm("F", x = 0.95f, y = 0.9f)
            drawFootNorm(x = 0.477f, y = 0.185f, width = 0.04f, height = 0.12f)
            drawFootNorm(x = 0.523f, y = 0.185f, width = 0.04f, height = 0.12f)
        }
    }
}

@Composable
fun YulGokPattern(
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        val boxSize: Dp = min(maxWidth, maxHeight)

        Canvas(
            modifier = Modifier
                .size(boxSize)
        ) {
            drawHorizontalNorm(y = 0.15f, start = 0.15f, length = 0.7f)
            drawHorizontalNorm(y = 0.65f, start = 0.05f, length = 0.9f)
            drawVerticalNorm(x = 0.5f, start = 0.15f, length = 0.7f)
            drawLetterNorm("C", x = 0.5f, y = 0.1f)
            drawLetterNorm("D", x = 0.5f, y = 1f)
            drawLetterNorm("A", x = 0.1f, y = 0.2f)
            drawLetterNorm("B", x = 0.9f, y = 0.2f)
            drawLetterNorm("E", x = 0f, y = 0.7f)
            drawLetterNorm("F", x = 1f, y = 0.7f)
            drawFootNorm(x = 0.35f, y = 0.15f, width = 0.04f, height = 0.12f)
            drawFootNorm(x = 0.65f, y = 0.15f, width = 0.04f, height = 0.12f)
        }
    }
}