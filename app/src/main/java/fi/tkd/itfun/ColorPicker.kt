package fi.tkd.itfun

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.github.skydoves.colorpicker.compose.*
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.launch

import fi.tkd.itfun.PrefKeys.PRIMARY_COLOR

@Composable
fun ColorPicker() {
    val controller = rememberColorPickerController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val initialColor = MaterialTheme.colorScheme.primary

    val initHsv = FloatArray(3).also { android.graphics.Color.colorToHSV(initialColor.toArgb(), it) }
    val lastHS = remember { floatArrayOf(initHsv[0], initHsv[1]) }

    val MIN_V = 0.5f
    val MIN_A = 0.3f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(all = 3.dp)
    ) {
        HsvColorPicker(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(10.dp),
            controller = controller,
            onColorChanged = { env ->
                val hsv = FloatArray(3)
                android.graphics.Color.colorToHSV(env.color.toArgb(), hsv)
                var h = hsv[0]; var s = hsv[1]; var v = hsv[2]
                var a = env.color.alpha

                // clamp with hue/sat preservation
                if (v < MIN_V) {
                    v = MIN_V
                    h = lastHS[0]
                    s = lastHS[1]
                } else {

                    lastHS[0] = h
                    lastHS[1] = s
                }

                if (a < MIN_A) a = MIN_A

                val argb = android.graphics.Color.HSVToColor((a * 255).toInt(), floatArrayOf(h, s, v))
                val clamped = Color(argb)

                controller.selectByColor(clamped, fromUser = false)   // keep UI in sync
                scope.launch {
                    context.dataStore.edit { it[PRIMARY_COLOR] = clamped.toArgb() }
                }
            },
            initialColor = initialColor
        )
        AlphaSlider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .height(24.dp),
            controller = controller,
            tileOddColor = Color.White,
            tileEvenColor = Color.Black,
            wheelColor = Color.Blue,
            initialColor = initialColor
        )
        BrightnessSlider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .height(24.dp),
            controller = controller,
            wheelColor = Color.Blue,
            initialColor = initialColor
        )
    }
}
