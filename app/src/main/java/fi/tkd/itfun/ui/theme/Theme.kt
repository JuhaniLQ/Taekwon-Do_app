package fi.tkd.itfun.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged

import fi.tkd.itfun.dataStore
import fi.tkd.itfun.PrefKeys.PRIMARY_COLOR

private val DarkColorScheme = darkColorScheme(
    surface = Color.Black,
    onSurface = Blue,
    primary = Navy,
    onPrimary = Chartreuse
)

private val LightColorScheme = lightColorScheme(
    surface = Color.White,
    onSurface = Blue,
    primary = Navy,
    onPrimary = Chartreuse
)

@Composable
fun ITFunTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val themeDefaultInt = Navy.toArgb()

    val primaryColorInt by remember(context, themeDefaultInt) {
        context.dataStore.data
            .map { prefs -> prefs[PRIMARY_COLOR] ?: themeDefaultInt }
            .distinctUntilChanged()
    }.collectAsState(initial = themeDefaultInt)

    val primaryColor = Color(primaryColorInt)

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q -> DarkColorScheme
        else -> LightColorScheme
    }

    val changedScheme = colorScheme.copy(primary = primaryColor)

    MaterialTheme(
        colorScheme = changedScheme,
        typography = Typography,
        content = content
    )
}

