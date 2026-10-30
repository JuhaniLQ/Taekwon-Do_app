package fi.tkd.itfun

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.graphics.Color

@Composable
fun DownloadSplashScreen(
    backgroundColor: Color,
    progress: Float
) {
    val percent = (progress * 100).toInt()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {

        Box(
            modifier = Modifier
                .size(290.dp)
                .align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = progress.coerceIn(0f, 1f),
                modifier = Modifier.size(220.dp),
                strokeWidth = 6.dp
            )

            Image(
                painter = painterResource(R.drawable.onboard_icon),
                contentDescription = null,
                //modifier = Modifier.size(290.dp)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 200.dp),
            horizontalAlignment = Alignment.CenterHorizontally

        ) {
            val phaseText = when {
                progress < 0.2f -> "Downloading white belt techniques"
                progress < 0.4f -> "Downloading yellow belt techniques"
                progress < 0.6f -> "Downloading green belt techniques"
                progress < 0.9f -> "Downloading blue belt techniques"
                else -> "Downloading compendium images"
            }
            Text(
                text = "$phaseText ($percent%)",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
