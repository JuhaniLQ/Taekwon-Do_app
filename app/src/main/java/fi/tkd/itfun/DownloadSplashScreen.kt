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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.graphics.Color
import fi.tkd.itfun.data.db.DbProvider
import kotlinx.coroutines.flow.map

@Composable
fun DownloadSplashScreen(
    backgroundColor: Color,
    progress: Float
) {
    val percent = (progress * 100).toInt()
    val context = LocalContext.current
    val db = remember(context) { DbProvider.get(context) }
    val hasRedBeltContent by remember(db) {
        db.itemDao().getAll().map { items -> items.any { it.beltLevel in 1..2 } }
    }.collectAsState(initial = false)

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
            val phaseText = if (hasRedBeltContent) {
                when {
                    progress < 0.15f -> "Downloading white belt techniques"
                    progress < 0.30f -> "Downloading yellow belt techniques"
                    progress < 0.50f -> "Downloading green belt techniques"
                    progress < 0.70f -> "Downloading blue belt techniques"
                    progress < 0.90f -> "Downloading red belt techniques"
                    else -> "Downloading compendium images"
                }
            } else {
                when {
                    progress < 0.20f -> "Downloading white belt techniques"
                    progress < 0.40f -> "Downloading yellow belt techniques"
                    progress < 0.65f -> "Downloading green belt techniques"
                    progress < 0.90f -> "Downloading blue belt techniques"
                    else -> "Downloading compendium images"
                }
            }
            Text(
                text = "$phaseText ($percent%)",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
