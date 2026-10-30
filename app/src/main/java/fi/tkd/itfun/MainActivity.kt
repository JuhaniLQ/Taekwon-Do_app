package fi.tkd.itfun


import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import fi.tkd.itfun.ui.theme.ITFunTheme
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import fi.tkd.itfun.PrefKeys.PRIMARY_COLOR
import fi.tkd.itfun.ui.theme.Navy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.SideEffect

class MainActivity : ComponentActivity() {

    private var currentSystemBarColor: Int? = null
    private var currentSystemBarDarkIcons = true

    internal fun setSystemBarAppearance(color: Int, darkIcons: Boolean) {
        currentSystemBarColor = color
        currentSystemBarDarkIcons = darkIcons
        restoreSystemBarAppearance()
    }

    private fun restoreSystemBarAppearance() {
        val color = currentSystemBarColor ?: return
        window.statusBarColor = color
        window.navigationBarColor = color
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = currentSystemBarDarkIcons
            isAppearanceLightNavigationBars = currentSystemBarDarkIcons
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            restoreSystemBarAppearance()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            splashScreenView.remove()
            // SplashScreen can restore the XML theme's grey after Compose sets
            // the restored screen's black/white bars. Reapply the active style.
            restoreSystemBarAppearance()
        }

        val isDarkTheme = resources.configuration.uiMode and
                UI_MODE_NIGHT_MASK == UI_MODE_NIGHT_YES

        enableEdgeToEdge(
            navigationBarStyle = if (isDarkTheme) {
                SystemBarStyle.dark(android.graphics.Color.BLACK)
            } else {
                SystemBarStyle.light(android.graphics.Color.WHITE, android.graphics.Color.BLACK)
            },
            statusBarStyle = if (isDarkTheme) {
                SystemBarStyle.dark(android.graphics.Color.BLACK)
            } else {
                SystemBarStyle.light(android.graphics.Color.WHITE, android.graphics.Color.BLACK)
            }
        )

        super.onCreate(savedInstanceState)

        val themeDefaultInt = Navy.toArgb()
        runBlocking(Dispatchers.IO) {
            applicationContext.dataStore.data
                .map { prefs -> prefs[PRIMARY_COLOR] ?: themeDefaultInt }
                .first()
        }

        val startupViewModel =
            ViewModelProvider(this)[StartupViewModel::class.java]
        val contentUpdateViewModel =
            ViewModelProvider(this)[ContentUpdateViewModel::class.java]
        val contentInventoryViewModel =
            ViewModelProvider(this)[ContentInventoryViewModel::class.java]

        setContent {
            val lightGrey = colorResource(id = R.color.light_grey)
            val startupState by startupViewModel.uiState.collectAsState()

            ITFunTheme {
                if (!startupState.contentReady) {
                    // fake splash while first videos + DB are being prepared
                    SystemBarsSolidColor(color = lightGrey, darkIcons = true)
                    DownloadSplashScreen(
                        backgroundColor = lightGrey,
                        progress = startupState.progress
                    )
                } else {
                    var selectedStartPage by rememberSaveable { mutableStateOf<Int?>(null) }


                    if (selectedStartPage == null) {
                        SystemBarsSolidColor(color = lightGrey, darkIcons = true)
                        OnboardingScreen(
                            backgroundColor = lightGrey,
                            onOpenTechniques = { selectedStartPage = 1 },
                            onOpenBodyParts = { selectedStartPage = 0 },
                            onOpenCompendium = { selectedStartPage = 2 }
                        )
                    } else {
                        SystemBarsSolidColor(
                            color = if (isDarkTheme) Color.Black else Color.White,
                            darkIcons = !isDarkTheme
                        )
                        Box(Modifier.safeDrawingPadding()) {
                            Surface(Modifier.fillMaxSize()) {
                                Pager(
                                    contentUpdateViewModel = contentUpdateViewModel,
                                    contentInventoryViewModel = contentInventoryViewModel,
                                    initialPage = selectedStartPage!!
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SystemBarsSolidColor(
    color: Color,
    darkIcons: Boolean
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as MainActivity).setSystemBarAppearance(color.toArgb(), darkIcons)
        }
    }
}
