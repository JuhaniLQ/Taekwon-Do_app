package fi.tkd.itfun


import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fi.tkd.itfun.ui.theme.ITFunTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.*
//import fi.tkd.itfun.R

import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import fi.tkd.itfun.ui.theme.Navy1

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        //val splashscreen = installSplashScreen()
        //var keepSplashScreen = true
        super.onCreate(savedInstanceState)
        //splashscreen.setKeepOnScreenCondition { keepSplashScreen }
        /*lifecycleScope.launch {
            delay(10)
            keepSplashScreen = false
        }*/
        setContent {
            ITFunTheme {
                MyApp(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun MyApp(modifier: Modifier = Modifier) {

    var shouldShowOnboarding by rememberSaveable { mutableStateOf(false) } // change this to true to show beginning screen

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(modifier) {
            if (shouldShowOnboarding) {
                OnboardingScreen(onContinueClicked = { shouldShowOnboarding = false })
            } else {
                Greetings()
            }
        }
    }
}

@Composable
fun OnboardingScreen(
    onContinueClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("HELLO ")
        Button(
            modifier = Modifier.padding(vertical = 24.dp),
            onClick = onContinueClicked
        ) {
            Text("Continue")
        }
    }

}

@Composable
private fun Greetings() {
    val tabs = listOf("All", "Pattern", "Offensive", "Defensive", "Kicks", "All patterns")
    val patterns = listOf("Chon-Ji", "Dan-Gun", "Do-San") // Submenu items
    val selectedTab = remember { mutableIntStateOf(0) }
    val selectedPattern = remember { mutableStateOf("") }
    val isSubMenuVisible = remember { mutableStateOf(false) }

    Column {
        // Tabs Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier
                .padding(bottom = 4.dp, top = 5.dp, start = 3.dp)
        ) {
            items(tabs) { tab ->
                Genre(tab, selectedTab.intValue == tabs.indexOf(tab)) {
                    if (tab == "Pattern") {
                        isSubMenuVisible.value = true // Show submenu
                    } else {
                        selectedTab.intValue = tabs.indexOf(tab) // Update selected tab index
                        selectedPattern.value = "" // Reset pattern
                        isSubMenuVisible.value = false // Hide submenu
                    }
                }
            }
        }

        // Submenu for "Pattern" as a DropdownMenu
        Card(
            modifier = Modifier.padding(start=50.dp)
        ) {
            MaterialTheme(
                shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(16.dp))
            ) {
                DropdownMenu(
                    expanded = isSubMenuVisible.value,
                    onDismissRequest = { isSubMenuVisible.value = false },
                    modifier = Modifier
                        .background(Navy1)
                ) {
                    patterns.forEach { pattern ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = pattern,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 22.sp
                                )
                            },
                            onClick = {
                                selectedPattern.value = pattern
                                isSubMenuVisible.value = false
                                selectedTab.intValue = tabs.indexOf("Pattern")
                            },
                            modifier = Modifier
                                .padding(horizontal = 0.dp) // Ensures no extra padding on sides
                                .fillMaxWidth()
                        )

                    }
                }
            }
        }
        LazyColumn(modifier = Modifier.padding(bottom = 8.dp)) {
            val selectedCategory = tabs[selectedTab.intValue]
            items(items = tkdItems) { tkdItem ->
                // Filter content based  tab
                if (selectedCategory == "All"
                    || (selectedCategory == "Pattern" && tkdItem.pattern.contains(selectedPattern.value))
                    || (tkdItem.category == selectedCategory)) {
                    Greeting(tkdItem = tkdItem)
                }
            }
        }
    }
}

@Composable
fun Genre(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(30))
            .background(if (isSelected) Navy1 else MaterialTheme.colorScheme.primary)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ){
        Text(
            modifier = Modifier.padding(10.dp),
            text = text,
            color = if(isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.surface,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun Greeting(tkdItem: TKDItem, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        modifier = modifier.padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        CardContent(tkdItem)
    }
}

data class TKDItem(
    val drawable: Int,
    val name: String,
    val koreanName: String,
    val category: String,
    val pattern: List<String>
)

private val tkdItems = listOf(
    TKDItem(R.drawable.innerforearmmiddleblock, "Inner Forearm Middle Block", TKDTerms.INNER_FOREARM_MIDDLE_BLOCK, TKDCategory.Defensive, listOf(TKDPatterns.CHON_JI)),
    TKDItem(R.drawable.outerforearmhighblock, "Outer Forearm High Block", TKDTerms.OUTER_FOREARM_HIGH_BLOCK, TKDCategory.Defensive, listOf(TKDPatterns.DO_SAN)),
    TKDItem(R.drawable.forearmlowblock, "Outer Forearm Low Block", TKDTerms.OUTER_FOREARM_LOW_BLOCK, TKDCategory.Defensive, listOf(TKDPatterns.CHON_JI, TKDPatterns.DAN_GUN)),
    TKDItem(R.drawable.twinforearmblock, "Twin Forearm Block", TKDTerms.TWIN_FOREARM_BLOCK, TKDCategory.Defensive, listOf(TKDPatterns.DAN_GUN)),
    TKDItem(R.drawable.wedgingblock, "Wedging Block", TKDTerms.WEDGING_BLOCK, TKDCategory.Defensive, listOf(TKDPatterns.DO_SAN)),
    TKDItem(R.drawable.outerforearmrisingblock, "Outer Forearm Rising Block", TKDTerms.OUTER_FOREARM_RISING_BLOCK, TKDCategory.Defensive, listOf(TKDPatterns.DAN_GUN, TKDPatterns.DO_SAN)),
    TKDItem(R.drawable.tf, "Knifehand Guarding Block", TKDTerms.KNIFE_HAND_GUARDING_BLOCK, TKDCategory.Defensive, listOf(TKDPatterns.DAN_GUN)),
    TKDItem(R.drawable.tf, "Knifehand Low Block", TKDTerms.KNIFE_HAND_LOW_BLOCK, TKDCategory.Offensive, listOf()),
    TKDItem(R.drawable.tf, "Knifehand Strike", TKDTerms.KNIFE_HAND_STRIKE_BAKURO, TKDCategory.Offensive, listOf(TKDPatterns.DAN_GUN)),
    TKDItem(R.drawable.tf, "Knifehand Strike", TKDTerms.KNIFE_HAND_STRIKE_YOP, TKDCategory.Offensive, listOf(TKDPatterns.DO_SAN)),
    TKDItem(R.drawable.walkingstancemiddlepunch, "Middle Punch", TKDTerms.MIDDLE_PUNCH, TKDCategory.Offensive, listOf(TKDPatterns.CHON_JI, TKDPatterns.DO_SAN)),
    TKDItem(R.drawable.tf, "High Punch", TKDTerms.HIGH_PUNCH, TKDCategory.Offensive, listOf(TKDPatterns.DAN_GUN)),
    TKDItem(R.drawable.tf, "Middle Reverse Punch", TKDTerms.MIDDLE_REVERSE_PUNCH, TKDCategory.Offensive, listOf(TKDPatterns.DO_SAN)),
    TKDItem(R.drawable.backfisthighsidestrike, "Backfist High Side Strike", TKDTerms.BACKFIST_HIGH_SIDE_STRIKE, TKDCategory.Offensive, listOf(TKDPatterns.DO_SAN)),
    TKDItem(R.drawable.straightfingertipthrust, "Straight Fingertip Thrust", TKDTerms.STRAIGHT_FINGERTIP_THRUST, TKDCategory.Offensive, listOf(TKDPatterns.DO_SAN)),
    TKDItem(R.drawable.tf, "Front Elbow Strike", TKDTerms.FRONT_ELBOW_STRIKE, TKDCategory.Offensive, listOf()),
    TKDItem(R.drawable.frontsnapkick, "Front Snap Kick", TKDTerms.FRONT_SNAP_KICK, TKDCategory.Kicks, listOf(TKDPatterns.DO_SAN)),
    TKDItem(R.drawable.sidepiercingkick, "Side Piercing Kick", TKDTerms.SIDE_PIERCING_KICK, TKDCategory.Kicks, listOf()),
    TKDItem(R.drawable.turningkick, "Turning Kick", TKDTerms.TURNING_KICK, TKDCategory.Kicks, listOf()),
    TKDItem(R.drawable.tf, "Back Piercing Kick", TKDTerms.BACK_PIERCING_KICK, TKDCategory.Kicks, listOf()),
    TKDItem(R.drawable.tf, "Reverse Turning Kick", TKDTerms.REVERSE_TURNING_KICK, TKDCategory.Kicks, listOf()),
    TKDItem(R.drawable.tf, "Heaven and Earth", TKDPatterns.CHON_JI, TKDCategory.All_patterns, listOf(TKDPatterns.CHON_JI)),
    TKDItem(R.drawable.tf, "Dangun, the legendary founder of Gojoseon,", TKDPatterns.DAN_GUN, TKDCategory.All_patterns, listOf(TKDPatterns.DAN_GUN)),
    TKDItem(R.drawable.tf, "Ahn Chang-ho, Korean independence activist ", TKDPatterns.DO_SAN, TKDCategory.All_patterns, listOf(TKDPatterns.DO_SAN)),
)

private val images = mapOf(
    "Kicks" to R.drawable.kicks,
    "Offensive" to R.drawable.offensive,
    "Defensive" to R.drawable.defensive,
    "All patterns" to R.drawable.pattern
)

@Composable
private fun CardContent(tkdItem: TKDItem) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
        ) {
            Image(
                painter = painterResource(images[tkdItem.category] ?: R.drawable.guy),
                contentDescription = "dude",
                modifier = Modifier
                    .size(40.dp)
                    // Clip image to be shaped as a circle
                    .clip(CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )

            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                Text(text = tkdItem.name, color = MaterialTheme.colorScheme.surface)
                Text(
                    text = tkdItem.koreanName,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = MaterialTheme.colorScheme.surface,
                    fontSize = 22.sp
                )
            }
            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) {

                        stringResource(R.string.show_less)
                    } else {
                        stringResource(R.string.show_more)
                    }

                )
            }
        }
        if (expanded) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                shadowElevation = 1.dp,
                modifier = Modifier.padding(1.dp)
            ) {
                GifImage(
                    //gifResId = tkdItems[currentIndex].drawable,
                    gifResId = tkdItem.drawable,
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun GifImage(
    modifier: Modifier = Modifier,
    gifResId: Int // Pass the resource ID for your GIF
) {
    val context = LocalContext.current
    val imageLoader = ImageLoader.Builder(context)
        .components {
            if (SDK_INT >= 28) {
                add(ImageDecoderDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
        }
        .build()

    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(gifResId)
            .build(),
        imageLoader = imageLoader
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.78f)
    ) {
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}

object TKDCategory {
    const val Defensive = "Defensive"
    const val Offensive = "Offensive"
    const val Kicks = "Kicks"
    const val  All_patterns = "All patterns"
}

object TKDPatterns {
    const val CHON_JI = "Chon-Ji"
    const val DAN_GUN = "Dan-Gun"
    const val DO_SAN = "Do-San"
}

object TKDTerms {
    // Stances
    const val GUNNUN_SO = "Gunnun So"
    const val NIUNJA_SO = "Niunja So"
    const val ANNUN_SO = "Annun So"

    // Blocks
    const val BAKAT_PALMOK = "Bakat Palmok"
    const val AN_PALMOK = "An Palmok"
    const val SANG_PALMOK = "Sang Palmok"
    const val MAKGI = "Makgi"
    const val DAEBI = "Daebi"
    const val HECHYO = "Hechyo"
    const val CHUKYO = "Chukyo"

    // Attacks  Jirugi (punch), Taerigi (strike), Tulgi (thrust), Busigi (kick), and Chagi (strike/kick).
    const val JIRUGI = "Jirugi"
    const val TAERIGI = "Taerigi"
    const val TULGI = "Tulgi"
    const val BUSIGI = "Busigi"
    const val CHAGI = "Chagi"
    const val CHA = "Cha"

    // Hands and Body  Sonkal (knife hand), Sun Sonkut (fingertips), Joomuk (fist), Dung (backside), and Palkup (elbow)
    const val SONKAL = "Sonkal"
    const val SUN_SONKUT = "Sun Sonkut"
    const val JOOMUK = "Joomuk"
    const val DUNG = "Dung"
    const val PALKUP = "Palkup"

    // Directions and Heights  Yop (side), Ap (front), Dwit (back), Kaunde (middle), Nopunde (high), Najunde (low)
    const val YOP = "Yop"
    const val AP = "Ap"
    const val DWIT = "Dwit"
    const val KAUNDE = "Kaunde"
    const val NOPUNDE = "Nopunde"
    const val NAJUNDE = "Najunde"

    // Movements
    const val DOLLYO = "Dollyo"
    const val BANDAE = "Bandae"
    const val BAKURO = "Bakuro" // outward
    const val BARO = "Baro"

    // Predefined Templates
    const val INNER_FOREARM_MIDDLE_BLOCK = "$NIUNJA_SO $KAUNDE $AN_PALMOK $YOP $MAKGI"
    const val OUTER_FOREARM_HIGH_BLOCK = "$GUNNUN_SO $NOPUNDE $BAKAT_PALMOK $YOP $MAKGI"
    const val OUTER_FOREARM_LOW_BLOCK = "$GUNNUN_SO $NAJUNDE $BAKAT_PALMOK $MAKGI"
    const val KNIFE_HAND_LOW_BLOCK = "$SONKAL $NAJUNDE $MAKGI"
    const val OUTER_FOREARM_RISING_BLOCK = "$GUNNUN_SO $NOPUNDE $BAKAT_PALMOK $CHUKYO $MAKGI"
    const val KNIFE_HAND_STRIKE_BAKURO = "$NIUNJA_SO $KAUNDE $SONKAL $BAKURO $TAERIGI"
    const val KNIFE_HAND_STRIKE_YOP = "$ANNUN_SO $KAUNDE $SONKAL $YOP $TAERIGI"
    const val KNIFE_HAND_GUARDING_BLOCK = "$NIUNJA_SO $KAUNDE $SONKAL $DAEBI $MAKGI"
    const val MIDDLE_PUNCH = "$GUNNUN_SO $KAUNDE $BARO $JIRUGI"
    const val HIGH_PUNCH = "$GUNNUN_SO $NOPUNDE $BARO $JIRUGI"
    const val MIDDLE_REVERSE_PUNCH = "$GUNNUN_SO $KAUNDE $BANDAE $JIRUGI"
    const val TWIN_FOREARM_BLOCK = "$NIUNJA_SO $SANG_PALMOK $MAKGI"
    const val BACKFIST_HIGH_SIDE_STRIKE = "$GUNNUN_SO $NOPUNDE $DUNG $JOOMUK $YOP $TAERIGI"
    const val STRAIGHT_FINGERTIP_THRUST = "$GUNNUN_SO $KAUNDE $SUN_SONKUT $TULGI"
    const val WEDGING_BLOCK = "$GUNNUN_SO $NOPUNDE $BAKAT_PALMOK $HECHYO $MAKGI"
    const val FRONT_ELBOW_STRIKE = "$AP $PALKUP $TAERIGI"
    const val FRONT_SNAP_KICK = "$KAUNDE $AP $CHA $BUSIGI"
    const val SIDE_PIERCING_KICK = "$YOP $CHA $JIRUGI"
    const val TURNING_KICK = "$DOLLYO $CHAGI"
    const val BACK_PIERCING_KICK = "$DWIT $CHA $JIRUGI"
    const val REVERSE_TURNING_KICK = "$BANDAE $DOLLYO $CHAGI"
}

