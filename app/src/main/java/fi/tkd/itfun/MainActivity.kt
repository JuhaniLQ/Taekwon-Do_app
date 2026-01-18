package fi.tkd.itfun


import android.app.Activity
import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import fi.tkd.itfun.ui.theme.ITFunTheme
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowInsetsControllerCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import fi.tkd.itfun.PrefKeys.CONTENT_READY
import fi.tkd.itfun.PrefKeys.PRIMARY_COLOR
import fi.tkd.itfun.data.db.DbProvider
import fi.tkd.itfun.data.db.downloadAllVideos
import fi.tkd.itfun.ui.theme.Green
import fi.tkd.itfun.ui.theme.Navy
import fi.tkd.itfun.ui.theme.Navy1
import fi.tkd.itfun.ui.theme.Red
import fi.tkd.itfun.ui.theme.Yellow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

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

        runBlocking(Dispatchers.IO) {
            DbProvider.ensureSeeded(applicationContext)
        }

        val themeDefaultInt = Navy.toArgb()
        runBlocking(Dispatchers.IO) {
            applicationContext.dataStore.data
                .map { prefs -> prefs[PRIMARY_COLOR] ?: themeDefaultInt }
                .first()
        }

        // first run flag
        val initialContentReady = runBlocking(Dispatchers.IO) {
            applicationContext.dataStore.data
                .map { prefs -> prefs[CONTENT_READY] ?: false }
                .first()
        }

        setContent {
            val appContext = applicationContext
            val lightGrey = colorResource(id = R.color.light_grey)

            var contentReady by remember { mutableStateOf(initialContentReady) }
            var progress by remember { mutableFloatStateOf(0f) }

            LaunchedEffect(Unit) {

                // first run only, download videos with progress
                if (!initialContentReady) {
                    downloadAllVideos(appContext) {p ->
                        progress = p
                    }
                    appContext.dataStore.edit { prefs ->
                        prefs[CONTENT_READY] = true
                    }
                    contentReady = true
                }
            }

            ITFunTheme {
                if (!contentReady) {
                    // fake splash while first videos + DB are being prepared
                    SystemBarsSolidColor(color = lightGrey, darkIcons = true)
                    DownloadSplashScreen(
                        backgroundColor = lightGrey,
                        progress = progress
                    )
                } else {
                    SystemBarsSolidColor(
                        color = if (isDarkTheme) Color.Black else Color.White,
                        darkIcons = !isDarkTheme
                    )

                    Box(Modifier.safeDrawingPadding()) {
                        Surface(Modifier.fillMaxSize()) {
                            Pager()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadSplashScreen(
    backgroundColor: Color,
    progress: Float
) {
    val percent = (progress * 100).toInt()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = progress.coerceIn(0f, 1f),
                    modifier = Modifier.size(220.dp),
                    strokeWidth = 6.dp
                )

                Image(
                    painter = painterResource(R.drawable.onboard_icon),
                    contentDescription = null,
                    modifier = Modifier.size(290.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Downloading white belt techniques ($percent%)",
                style = MaterialTheme.typography.bodyMedium
            )
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
            val window = (view.context as Activity).window
            window.statusBarColor = color.toArgb()
            window.navigationBarColor = color.toArgb()

            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.isAppearanceLightStatusBars = darkIcons
            controller.isAppearanceLightNavigationBars = darkIcons
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Pager(){
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { DbProvider.get(context) }

    //screen one (body)
    val bodyPartRows by db.bodyPartDao().getAll()
        .collectAsState(initial = emptyList())

    //Screen two (techniques)
    val categories by db.categoryDao().getAll()
        .collectAsState(initial = emptyList())
    val itemRows by db.itemDao().getAll()
        .collectAsState(initial = emptyList())
    val links by db.itemDao().getItemPatternNames()
        .collectAsState(initial = emptyList())

    val catById = remember(categories) { categories.associate { it.id to it.name } }
    val patternsByItem = remember(links) { links.groupBy({ it.itemId }) { it.patternName } }

    val tkdItemsRoom = remember(itemRows, catById, patternsByItem) {
        itemRows.mapNotNull { e ->
            val categoryName = catById[e.categoryId] ?: return@mapNotNull null
            val category = runCatching { TKDItemCategory.valueOf(categoryName) }.getOrNull()
                ?: return@mapNotNull null
            TKDItem(
                techniqueCode = e.techniqueCode,
                name = e.name,
                koreanName = e.koreanName,
                category = category,
                pattern = patternsByItem[e.id].orEmpty(),
                beltLevel = e.beltLevel,
                videoKey = e.videoKey,
                localVideoPath = e.localVideoPath
            )
        }
    }

    val bodyItems = remember(bodyPartRows) {
        bodyPartRows.mapNotNull { e ->
            val region = runCatching { BodyRegion.valueOf(e.region) }.getOrNull()
                ?: return@mapNotNull null

            BodyItem(
                code = e.code,
                region = region,
                canBeVitalPoint = e.canBeVitalPoint,
                nameEn = e.nameEn,
                nameKo = e.nameKo
            )
        }
    }

    val pagerState = rememberPagerState ( initialPage = 1 ) { 3 }
    var isSheetOpen by rememberSaveable { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableIntStateOf(pagerState.currentPage) }

    // HOISTED state for the techniques screen
    val patternStr = "Pattern"
    val selectedTab = rememberSaveable { mutableStateOf("All") }
    val selectedPattern = rememberSaveable { mutableStateOf("") }
    val selectedFilters = rememberSaveable { mutableStateOf(setOf<String>()) }

    //Hoisted state for Body screen
    val selectedTabBody = rememberSaveable { mutableStateOf("All") }
    val selectedFiltersBody = rememberSaveable { mutableStateOf(setOf<String>()) }

    //Hoisted state for update progress bar
    var statusText by remember { mutableStateOf("") }
    var progress by remember { mutableFloatStateOf(0f) }
    var showBar by remember { mutableStateOf(false) }
    var isUpdating by remember { mutableStateOf(false) }

    LaunchedEffect(selectedTabIndex) {
        pagerState.scrollToPage(selectedTabIndex)
    }

    LaunchedEffect(pagerState.currentPage) {
        selectedTabIndex = pagerState.currentPage
    }
    Box( // close search bar keyboard when touching outside keyboard
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Final)
                        val down = event.changes.any { it.changedToDownIgnoreConsumed() }
                        if (down) {
                            focusManager.clearFocus()
                        }
                    }
                }
            }
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    modifier = Modifier.weight(1f)
                ) {
                    val icons = listOf(
                        R.drawable.car_fan_low_mid_left,
                        R.drawable.falling,
                        R.drawable.menu_book
                    )

                    icons.forEachIndexed { index, resId ->
                        Tab(
                            selected = index == selectedTabIndex,
                            onClick = { selectedTabIndex = index }
                        ) {
                            Icon(
                                painter = painterResource(resId),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }
                }

                // Settings
                IconButton(onClick = { isSheetOpen = true }) {
                    Icon(
                        painter = painterResource(R.drawable.more_vert),
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            val beltGup by context.dataStore.data
                .map { prefs -> (prefs[BELT_LEVEL] ?: 9).coerceIn(1, 9) } // default 9 = white
                .collectAsState(initial = 9)

            HorizontalPager(state = pagerState) { currentPage ->
                when (currentPage) {
                    0 -> ScreenOne(
                        selectedTab = selectedTabBody,
                        selectedFilters = selectedFiltersBody,
                        bodyItems = bodyItems
                    )
                    1 -> ScreenTwo(
                        tkdItemsRoom = tkdItemsRoom,
                        selectedTab = selectedTab,
                        selectedPattern = selectedPattern,
                        selectedFilters = selectedFilters,
                        patternStr = patternStr,
                        beltGup = beltGup
                    )
                    2 -> ScreenThree()
                }
            }
            val sheetState = rememberModalBottomSheetState()
            var sheetPage by rememberSaveable { mutableIntStateOf(0) }

            if (isSheetOpen) {
                ModalBottomSheet(
                    sheetState = sheetState,
                    onDismissRequest = { isSheetOpen = false }
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()

                    ) {

                        // --- Page selector row ---
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Middle tab: always centered
                            SheetTab(
                                text = "Color picker",
                                selected = sheetPage == 1,
                                onClick = { sheetPage = 1 }
                            )

                            // Left tab: pinned to start
                            Row(
                                modifier = Modifier.align(Alignment.CenterStart)
                            ) {
                                SheetTab(
                                    text = "Belt level",
                                    selected = sheetPage == 0,
                                    onClick = { sheetPage = 0 }
                                )
                            }

                            // Right tab: pinned to end
                            Row(
                                modifier = Modifier.align(Alignment.CenterEnd)
                            ) {
                                SheetTab(
                                    text = "Other",
                                    selected = sheetPage == 2,
                                    onClick = { sheetPage = 2 }
                                )
                            }
                        }

                        val selectedUiLevel = 10 - beltGup // 1..9

                        // --- Page content ---
                        when (sheetPage) {
                            0 -> BeltLevelSettings(
                                selectedLevel = selectedUiLevel,
                                onSelectLevel = { uiLevel ->
                                    val newUi = uiLevel.coerceIn(1, 9)
                                    val newGup = 10 - newUi
                                    scope.launch {
                                        context.dataStore.edit { it[BELT_LEVEL] = newGup }
                                    }
                                }
                            )
                            1 -> ColorPicker()
                            2 -> OtherSettings(
                                statusText = statusText,
                                progress = progress,
                                showBar = showBar,
                                isUpdating = isUpdating,
                                onUpdateClick = {
                                    scope.launch {
                                        isUpdating = true
                                        showBar = true
                                        progress = 0f
                                        statusText = "Checking for updates..."
                                        val result = DbProvider.updateSeeding(context)

                                        statusText = when (result) {
                                            DbProvider.UpdateResult.NetworkError -> "Unable to check for updates.\nPlease check your connection."
                                            DbProvider.UpdateResult.NoUpdate -> "You already have the latest content;\nchecking missing videos..."
                                            DbProvider.UpdateResult.Updated -> "Updating content..."
                                        }

                                        downloadAllVideos(context) { p ->
                                            progress = p
                                        }
                                        if (result != DbProvider.UpdateResult.NetworkError)  {
                                            statusText = "Done."
                                        }
                                        delay(3000)

                                        showBar = false
                                        progress = 0f
                                        statusText = ""
                                        isUpdating = false
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

val BELT_LEVEL = intPreferencesKey("belt_level")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SheetTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary.darker(30f),
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            containerColor = MaterialTheme.colorScheme.primary,
            labelColor = MaterialTheme.colorScheme.surface,
        ),
        border = null

    )
}


@Composable
fun ScreenOne(selectedTab: MutableState<String>,
              selectedFilters: MutableState<Set<String>>,
              bodyItems: List<BodyItem>) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier.fillMaxSize()
    ) {
        Body(selectedTab = selectedTab,
            selectedFilters = selectedFilters,
            bodyItems = bodyItems)
    }
}

@Composable
fun ScreenTwo(
              tkdItemsRoom: List<TKDItem>,
              selectedTab: MutableState<String>,
              selectedPattern: MutableState<String>,
              selectedFilters: MutableState<Set<String>>,
              patternStr: String,
              beltGup: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier.fillMaxSize()
    ) {
        Techniques(
            tkdItemsRoom = tkdItemsRoom,
            selectedTab = selectedTab,
            selectedPattern = selectedPattern,
            selectedFilters = selectedFilters,
            patternStr = patternStr,
            beltGup = beltGup
        )
    }
}


@Composable
fun ScreenThree() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier.fillMaxSize()
    ) {
        Text("Update",
            modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
fun BeltLevelSettings(
    selectedLevel: Int,
    onSelectLevel: (Int) -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top=37.dp, bottom = 37.dp),
        verticalArrangement = Arrangement.spacedBy(40.dp),
    ) {
        val boxSize = 64.dp
        val corner = RoundedCornerShape(12.dp)

        val beltColors = listOf(
            Color.White, // 1
            Yellow,      // 2
            Yellow,      // 3
            Green,       // 4
            Green,       // 5
            Navy1,       // 6
            Navy1,       // 7
            Red,         // 8
            Red          // 9
        )

        val stripeColors = mapOf(
            2 to Green,        // 3rd box gets a green stripe
            4 to Navy1,        // 5th box gets a blue stripe
            6 to Red,          // 7th box gets a red stripe
            8 to Color.Black   // 9th box gets a black stripe
        )

        var index = 0
        repeat(3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(3) {
                    val level = index + 1
                    val isSelected = level <= selectedLevel
                    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                    val contentAlpha = if (isSelected) 1f else 0.35f

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .size(boxSize)
                            .clip(corner)
                            .background(beltColors[index].copy(alpha = contentAlpha))
                            .border(5.dp, borderColor, corner)
                            .clickable { onSelectLevel(level) }
                    ) {
                        stripeColors[index]?.let { stripe ->
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.2f) // 40/20/40 split
                                    .background(stripe.copy(alpha = contentAlpha))
                            )
                        }
                    }

                    index++
                }
            }
        }

    }
}

@Composable
fun OtherSettings(
    statusText: String,
    progress: Float,
    showBar: Boolean,
    isUpdating: Boolean,
    onUpdateClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp, vertical = 5.dp)
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Button(
                onClick = onUpdateClick,
                enabled = !isUpdating) {
                Text("Update",
                    color = MaterialTheme.colorScheme.surface)
            }
        }

        Spacer(Modifier.height(8.dp))

        if (showBar) {
            UpdateProgressBar(
                statusText = statusText,
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp, vertical = 5.dp)
            )
        }
    }
}


@Composable
fun UpdateProgressBar(
    statusText: String,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val clamped = progress.coerceIn(0f, 1f)
    val trackColor = MaterialTheme.colorScheme.primary
    val fillColor = MaterialTheme.colorScheme.primary.darker(30f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(trackColor)
    ) {
        // Filled part
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(clamped)
                .clip(RoundedCornerShape(20.dp))
                .background(fillColor)
        )

        // Centered text
        Text(
            text = statusText,
            modifier = Modifier.align(Alignment.Center),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}