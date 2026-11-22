package fi.tkd.itfun.techniques

import android.net.Uri
import android.view.LayoutInflater
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import fi.tkd.itfun.R

@Composable
fun TechniqueVideoSegment(
    localVideoPath: String,
    segmentStartMs: Long? = null,
    segmentEndMs: Long? = null,
    playTrigger: Int = 0,
    modifier: Modifier = Modifier,
    playbackSpeed: Float = 1f
    ) {
    val context = LocalContext.current
    val appContext = context.applicationContext

    val exoPlayer = remember(localVideoPath) {
        ExoPlayer.Builder(appContext).build().apply {
            val uri = Uri.fromFile(File(localVideoPath))
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_ALL
            playWhenReady = true
            prepare()
        }
    }

    LaunchedEffect(exoPlayer, playbackSpeed) {
        exoPlayer.setPlaybackSpeed(playbackSpeed)
    }

    LaunchedEffect(exoPlayer, segmentStartMs, segmentEndMs, playTrigger) {
        withContext(Dispatchers.Main.immediate) {

            if (segmentStartMs == 0L && segmentEndMs == 0L) {
                exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
                exoPlayer.seekTo(0L)
                exoPlayer.pause()
                return@withContext
            }

            when {
                segmentStartMs == null -> {
                    // No segment: original looping behaviour
                    exoPlayer.repeatMode = Player.REPEAT_MODE_ALL
                    exoPlayer.playWhenReady = true
                }

                segmentEndMs == null -> {
                    // Start set, no explicit end: play to natural end, no loop
                    exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
                    exoPlayer.seekTo(segmentStartMs)
                    exoPlayer.playWhenReady = true
                }

                else -> {
                    // Start and end set: play segment, stop at endMs
                    exoPlayer.repeatMode = Player.REPEAT_MODE_OFF

                    val current = exoPlayer.currentPosition
                    val toleranceMs = 40L

                    val alreadyAtStart =
                        current in (segmentStartMs - toleranceMs)..(segmentStartMs + toleranceMs)

                    if (!alreadyAtStart) {
                        exoPlayer.seekTo(segmentStartMs)
                    }


                    exoPlayer.playWhenReady = true

                    while (currentCoroutineContext().isActive) {
                        val pos = exoPlayer.currentPosition
                        if (pos >= segmentEndMs) {
                            exoPlayer.pause()
                            break
                        }
                        delay(50)
                    }
                }
            }
        }
    }


    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    TechniquePlayerView(player = exoPlayer, modifier = modifier)
}

@Composable
fun TechniqueVideo(
    localVideoPath: String,
    modifier: Modifier = Modifier,
    playbackSpeed: Float = 1f,
    playWhenReady: Boolean? = null,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val appContext = context.applicationContext

    val exoPlayer = remember(localVideoPath) {
        ExoPlayer.Builder(appContext).build().apply {
            val uri = Uri.fromFile(File(localVideoPath))

            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_ALL
            this.playWhenReady = playWhenReady ?: true
            prepare()
        }
    }

    LaunchedEffect(exoPlayer, playbackSpeed) {
        exoPlayer.setPlaybackSpeed(playbackSpeed)
    }

    LaunchedEffect(exoPlayer, playWhenReady) {
        if (playWhenReady != null) exoPlayer.playWhenReady = playWhenReady
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    TechniquePlayerView(player = exoPlayer, modifier = modifier, onClick = onClick)
}

@Composable
internal fun TechniquePlayerView(player: Player, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    AndroidView(
        modifier = modifier.aspectRatio(16f / 9f),
        factory = { context ->
            LayoutInflater.from(context)
                .inflate(R.layout.technique_player_view, null, false) as PlayerView
        },
        update = { view ->
            if (view.player !== player) {
                view.player = player
            }
            // AndroidView may survive a video change; bind taps to the current player too.
            view.setOnClickListener {
                if (onClick != null) onClick()
                else if (player.isPlaying) player.pause() else player.play()
            }
        },
        onRelease = { view -> view.player = null }
    )
}
