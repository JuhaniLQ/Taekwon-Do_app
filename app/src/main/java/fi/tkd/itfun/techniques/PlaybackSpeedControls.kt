package fi.tkd.itfun.techniques

import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fi.tkd.itfun.shared.opacity
import java.util.Locale
import kotlin.math.roundToInt

@Composable
internal fun PlaybackSpeedControls(step: Int, onStepChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.height(40.dp), verticalAlignment = Alignment.CenterVertically) {
        PlaybackSpeedButton(
            isPlus = false,
            enabled = step > 1,
            onClick = { onStepChange(step - 1) }
        )
        Column(
            modifier = Modifier.weight(1f).align(Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = String.format(Locale.getDefault(), "speed %.2f×", step / 10f),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 5.dp)
            )
            Slider(
                value = step.toFloat(),
                onValueChange = { onStepChange(it.roundToInt()) },
                valueRange = 1f..30f,
                steps = 28,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary.opacity(50f),
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.onSurface.opacity(10f)              ),
                modifier = Modifier
                    .height(0.dp)
                    .semantics { contentDescription = "Playback speed" }
            )
        }
        PlaybackSpeedButton(
            isPlus = true,
            enabled = step < 30,
            onClick = { onStepChange(step + 1) }
        )
    }
}

@Composable
private fun PlaybackSpeedButton(isPlus: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = if (isPlus) "Increase playback speed" else "Decrease playback speed"
            },
        contentAlignment = Alignment.Center
    ) {
        val color = MaterialTheme.colorScheme.onSurface
        Canvas(Modifier.size(30.dp)) {
            if (isPlus) drawPlus(color) else drawMinus(color)
        }
    }
}
