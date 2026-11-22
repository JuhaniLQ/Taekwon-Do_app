package fi.tkd.itfun.techniques

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import fi.tkd.itfun.shared.rememberEdgeFeedbackModifier

@OptIn(ExperimentalFoundationApi::class)
class TechniqueCardListTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun oneCardStillReceivesEdgeFeedbackAndReleaseWithoutScrollingTheContent() {
        val effect = RecordingOverscrollEffect()
        val state = showList(count = 1, enabled = true, effect = effect)
        compose.onNodeWithTag("cards").performTouchInput { swipeDown() }
        compose.runOnIdle {
            assertTrue(effect.unconsumedDrag > 0f)
            assertTrue(effect.releases > 0)
            assertEquals(0, state.firstVisibleItemScrollOffset)
            assertFalse(state.canScrollForward)
        }
    }

    @Test
    fun longerListScrollsNormallyBeforeReachingAnEdge() {
        val effect = RecordingOverscrollEffect()
        val state = showList(count = 100, enabled = true, effect = effect)
        compose.onNodeWithTag("cards").performTouchInput { swipeUp(durationMillis = 1000) }
        compose.runOnIdle {
            assertTrue(state.canScrollBackward)
            assertEquals(0f, effect.unconsumedDrag, 0.01f)
        }
    }

    @Test
    fun disabledPatternStyleListDoesNotScrollOrStretch() {
        val effect = RecordingOverscrollEffect()
        val state = showList(count = 10, enabled = false, effect = effect)
        compose.onNodeWithTag("cards").performTouchInput { swipeUp() }
        compose.runOnIdle {
            assertEquals(0f, effect.unconsumedDrag, 0f)
            assertEquals(0, state.firstVisibleItemIndex)
            assertEquals(0, state.firstVisibleItemScrollOffset)
        }
    }

    @Test
    fun shortCompendiumPageReceivesFeedbackWithoutMovingItsContent() {
        val effect = RecordingOverscrollEffect()
        val state = showScrollContent(height = 80, effect = effect)
        compose.onNodeWithTag("page").performTouchInput { swipeDown() }
        compose.runOnIdle {
            assertTrue(effect.unconsumedDrag > 0f)
            assertTrue(effect.releases > 0)
            assertEquals(0, state.value)
            assertEquals(0, state.maxValue)
        }
    }

    @Test
    fun longerCompendiumPageScrollsBeforeReceivingEdgeFeedback() {
        val effect = RecordingOverscrollEffect()
        val state = showScrollContent(height = 3000, effect = effect)
        compose.onNodeWithTag("page").performTouchInput { swipeUp(durationMillis = 1000) }
        compose.runOnIdle {
            assertTrue(state.value > 0)
            assertEquals(0f, effect.unconsumedDrag, 0.01f)
        }
    }

    private fun showScrollContent(height: Int, effect: OverscrollEffect): ScrollState {
        lateinit var state: ScrollState
        compose.setContent {
            val scrollState = rememberScrollState()
            SideEffect { state = scrollState }
            Box(
                Modifier.height(300.dp).fillMaxWidth()
                    .then(rememberEdgeFeedbackModifier(effect)).testTag("page")
            ) {
                CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                    Column(Modifier.fillMaxSize().verticalScroll(scrollState)) {
                        Box(Modifier.fillMaxWidth().height(height.dp))
                    }
                }
            }
        }
        compose.waitForIdle()
        return state
    }

    private fun showList(count: Int, enabled: Boolean, effect: OverscrollEffect): LazyListState {
        lateinit var state: LazyListState
        compose.setContent {
            val listState = rememberLazyListState()
            SideEffect { state = listState }
            TechniqueCardList(
                state = listState,
                userScrollEnabled = enabled,
                contentPadding = PaddingValues(0.dp),
                overscrollEffect = effect,
                modifier = Modifier.height(300.dp).testTag("cards")
            ) {
                items(count) { Box(Modifier.fillMaxWidth().height(80.dp)) }
            }
        }
        compose.waitForIdle()
        return state
    }

    private class RecordingOverscrollEffect : OverscrollEffect {
        var unconsumedDrag = 0f
        var releases = 0
        override val isInProgress = false
        override val effectModifier = Modifier

        override fun applyToScroll(
            delta: Offset,
            source: NestedScrollSource,
            performScroll: (Offset) -> Offset
        ): Offset {
            val consumed = performScroll(delta)
            if (source == NestedScrollSource.UserInput) {
                unconsumedDrag += kotlin.math.abs(delta.y - consumed.y)
            }
            return delta
        }

        override suspend fun applyToFling(
            velocity: Velocity,
            performFling: suspend (Velocity) -> Velocity
        ) {
            releases++
            performFling(velocity)
        }
    }
}
