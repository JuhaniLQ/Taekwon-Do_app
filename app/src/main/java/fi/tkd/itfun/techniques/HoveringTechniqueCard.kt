package fi.tkd.itfun.techniques

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity

@Composable
internal fun HoveringTechniqueCard(
    item: TKDItem,
    sourceTop: Float,
    onReturn: () -> Unit,
    onHeightChanged: (Int) -> Unit,
    onOpenCompendiumEntry: (String) -> Unit,
    playbackSpeed: Float = 1f
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val heightPx = with(LocalDensity.current) { maxHeight.toPx() }
        var cardHeight by remember(item.techniqueCode) { mutableIntStateOf(0) }
        var expanded by remember(item.techniqueCode) { mutableStateOf(true) }
        val progress = remember(item.techniqueCode) { Animatable(0f) }

        LaunchedEffect(item.techniqueCode) {
            progress.animateTo(1f, tween(durationMillis = 300))
        }

        Box(
            Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onSizeChanged {
                    cardHeight = it.height
                    onHeightChanged(it.height)
                }
                .graphicsLayer {
                    translationY = (sourceTop - (heightPx - cardHeight)) * (1f - progress.value)
                }
        ) {
            Technique(
                playbackSpeed = playbackSpeed,
                tkdItem = item,
                expanded = expanded,
                onExpandedChange = { expanded = it },
                onOpenCompendiumEntry = onOpenCompendiumEntry,
                isHovering = true,
                onHoverClick = onReturn
            )
        }
    }
}
