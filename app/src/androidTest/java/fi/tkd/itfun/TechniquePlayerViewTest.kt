package fi.tkd.itfun

import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TechniquePlayerViewTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun reusedViewBindsReplacementPlayerAndUsesTextureSurface() {
        val useSecond = mutableStateOf(false)
        lateinit var first: ExoPlayer
        lateinit var second: ExoPlayer
        lateinit var root: View
        compose.setContent {
            val context = LocalContext.current
            root = LocalView.current.rootView
            first = remember { ExoPlayer.Builder(context).build() }
            second = remember { ExoPlayer.Builder(context).build() }
            DisposableEffect(Unit) {
                onDispose {
                    first.release()
                    second.release()
                }
            }
            TechniquePlayerView(if (useSecond.value) second else first)
        }
        compose.waitForIdle()
        lateinit var originalView: PlayerView
        compose.runOnIdle {
            originalView = requireNotNull(findPlayerView(root))
            assertSame(first, originalView.player)
            assertTrue(originalView.videoSurfaceView is TextureView)
            useSecond.value = true
        }
        compose.waitForIdle()
        compose.runOnIdle {
            val currentView = requireNotNull(findPlayerView(root))
            assertSame(originalView, currentView)
            assertSame(second, currentView.player)
            currentView.performClick()
            assertTrue(second.playWhenReady)
            assertFalse(first.playWhenReady)
        }
    }

    private fun findPlayerView(view: View): PlayerView? {
        if (view is PlayerView) return view
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                findPlayerView(view.getChildAt(index))?.let { return it }
            }
        }
        return null
    }
}
