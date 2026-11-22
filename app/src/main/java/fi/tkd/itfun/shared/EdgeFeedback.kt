package fi.tkd.itfun.shared

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.overscroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun rememberEdgeFeedbackModifier(effect: OverscrollEffect): Modifier {
    val connection = remember(effect) { EdgeFeedbackConnection(effect) }
    return Modifier.overscroll(effect).nestedScroll(connection)
}

@OptIn(ExperimentalFoundationApi::class)
private class EdgeFeedbackConnection(private val effect: OverscrollEffect) : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (!effect.isInProgress) return Offset.Zero
        var remaining = Offset.Zero
        val consumed = effect.applyToScroll(available, source) {
            remaining = it
            // Let the list handle this remainder; only release existing stretch here.
            it
        }
        return consumed - remaining
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
        effect.applyToScroll(available, source) { Offset.Zero }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (!effect.isInProgress) return Velocity.Zero
        var remaining = Velocity.Zero
        effect.applyToFling(available) {
            remaining = it
            it
        }
        return available - remaining
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        effect.applyToFling(available) { Velocity.Zero }
        return available
    }
}
