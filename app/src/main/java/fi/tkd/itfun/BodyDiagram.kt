package fi.tkd.itfun

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

// 1) Coordinate model (normalized 0f..1f relative to displayed image box)
data class EllipseNorm(
    val cx: Float,  // center x (0..1)
    val cy: Float,  // center y (0..1)
    val rx: Float,  // radius x (0..1)
    val ry: Float   // radius y (0..1)
) {
    fun contains(x: Float, y: Float): Boolean {
        if (rx <= 0f || ry <= 0f) return false
        val dx = (x - cx) / rx
        val dy = (y - cy) / ry
        return dx * dx + dy * dy <= 1f
    }
}

// 2) Hardcoded highlight coordinates
private val bodyHighlights = mapOf(
    //ARM
    "KNIFE_HAND" to EllipseNorm(cx = 0.467f, cy = 0.183f, rx = 0.154f, ry = 0.050f),
    "REVERSE_KNIFE_HAND" to EllipseNorm(cx = 0.853f, cy = 0.839f, rx = 0.145f, ry = 0.106f),
    "PALM" to EllipseNorm(cx = 0.425f, cy = 0.306f, rx = 0.075f, ry = 0.050f),
    "FINGERS" to EllipseNorm(cx = 0.600f, cy = 0.06f, rx = 0.108f, ry = 0.044f),
    "FINGERTIPS" to EllipseNorm(cx = 0.875f, cy = 0.067f, rx = 0.125f, ry = 0.056f),
    "THUMB" to EllipseNorm(cx = 0.912f, cy = 0.499f, rx = 0.087f, ry = 0.055f),
    "FOREARM" to EllipseNorm(cx = 0.396f, cy = 0.939f, rx = 0.121f, ry = 0.050f),
    "INNER_FOREARM" to EllipseNorm(cx = 0.637f, cy = 0.905f, rx = 0.137f, ry = 0.094f),
    "OUTER_FOREARM" to EllipseNorm(cx = 0.400f, cy = 0.439f, rx = 0.100f, ry = 0.078f),
    "ELBOW" to EllipseNorm(cx = 0.096f, cy = 0.922f, rx = 0.087f, ry = 0.056f),
    "SHOULDER" to EllipseNorm(cx = 0.3f, cy = 0.100f, rx = 0.127f, ry = 0.050f),

    //LEG
    "KNEE" to EllipseNorm(cx = 0.550f, cy = 0.194f, rx = 0.075f, ry = 0.050f),
    "SHIN" to EllipseNorm(cx = 0.583f, cy = 0.328f, rx = 0.075f, ry = 0.050f),
    "ANKLE" to EllipseNorm(cx = 0.242f, cy = 0.567f, rx = 0.092f, ry = 0.056f),
    "INSTEP" to EllipseNorm(cx = 0.629f, cy = 0.439f, rx = 0.096f, ry = 0.050f),
    "TOES" to EllipseNorm(cx = 0.679f, cy = 0.539f, rx = 0.071f, ry = 0.050f),
    "TOE_TIPS" to EllipseNorm(cx = 0.833f, cy = 0.361f, rx = 0.125f, ry = 0.061f),
    "HEEL_BACK" to EllipseNorm(cx = 0.200f, cy = 0.711f, rx = 0.150f, ry = 0.067f),
    "FOOT_BLADE" to EllipseNorm(cx = 0.250f, cy = 0.867f, rx = 0.167f, ry = 0.056f),
    "HEEL_SOLE" to EllipseNorm(cx = 0.508f, cy = 0.949f, rx = 0.150f, ry = 0.049f),
    "BALL_OF_FOOT" to EllipseNorm(cx = 0.775f, cy = 0.899f, rx = 0.154f, ry = 0.099f),

    //Torso
    "SOLAR_PLEXUS" to EllipseNorm(cx = 0.850f, cy = 0.233f, rx = 0.125f, ry = 0.056f),
    "TEMPLE" to EllipseNorm(cx = 0.804f, cy = 0.167f, rx = 0.087f, ry = 0.056f)
)

@Composable
fun DiagramWithHighlight(
    diagramResId: Int,
    bodyCode: String?,
    modifier: Modifier = Modifier,
    selectableItems: List<BodyItem> = emptyList(),
    onBodyPartClick: (String) -> Unit = {}
) {
    val ellipse = bodyCode?.let { bodyHighlights[it] }
    val primary = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)   // <-- makes the diagram area stable
            .padding(top = 12.dp)
    ) {
        Image(
            painter = painterResource(diagramResId),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.matchParentSize()
        )

        Canvas(
            modifier = Modifier.matchParentSize()
                .pointerInput(diagramResId, selectableItems, onBodyPartClick) {
                    detectTapGestures { position ->
                        val bounds = bodyDiagramBounds(size.width.toFloat(), size.height.toFloat())
                        if (bounds.width > 0f && bounds.height > 0f && bounds.contains(position)) {
                            val x = (position.x - bounds.left) / bounds.width
                            val y = (position.y - bounds.top) / bounds.height
                            selectableItems.firstOrNull { item ->
                                bodyHighlights[item.code]?.contains(x, y) == true
                            }?.let { onBodyPartClick(it.code) }
                        }
                    }
                }
        ) {
            if (ellipse != null) {
                val bounds = bodyDiagramBounds(size.width, size.height)
                val dw = bounds.width
                val dh = bounds.height
                val ox = bounds.left
                val oy = bounds.top

                val cx = ox + ellipse.cx * dw
                val cy = oy + ellipse.cy * dh
                val rx = ellipse.rx * dw
                val ry = ellipse.ry * dh

                drawOval(
                    color = primary.copy(alpha = 0.50f),
                    topLeft = Offset(cx - rx, cy - ry),
                    size = Size(rx * 2f, ry * 2f)
                )
                drawOval(
                    color = primary.copy(alpha = 0.85f),
                    topLeft = Offset(cx - rx, cy - ry),
                    size = Size(rx * 2f, ry * 2f),
                    style = Stroke(width = 3f)
                )
            }
        }
    }
}

// Shared image bounds keep tap testing aligned with the painted highlights.
internal fun bodyDiagramBounds(width: Float, height: Float): Rect {
    val scale = minOf(width / 1200f, height / 900f)
    val imageWidth = 1200f * scale
    val imageHeight = 900f * scale
    val left = (width - imageWidth) / 2f
    val top = (height - imageHeight) / 2f
    return Rect(left, top, left + imageWidth, top + imageHeight)
}
