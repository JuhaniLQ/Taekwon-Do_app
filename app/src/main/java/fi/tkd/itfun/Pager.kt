package fi.tkd.itfun

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import fi.tkd.itfun.data.db.CompendiumSectionEntity
import fi.tkd.itfun.data.db.DbProvider
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import fi.tkd.itfun.body.Body
import fi.tkd.itfun.body.BodyItem
import fi.tkd.itfun.body.BodyRegion
import fi.tkd.itfun.compendium.Compendium
import fi.tkd.itfun.shared.SettingsSheet
import fi.tkd.itfun.techniques.TKDItem
import fi.tkd.itfun.techniques.TKDItemCategory
import fi.tkd.itfun.techniques.Techniques

@Composable
fun Pager(
    contentUpdateViewModel: ContentUpdateViewModel,
    contentInventoryViewModel: ContentInventoryViewModel,
    onBackToOnboarding: () -> Unit,
    initialPage: Int = 1
) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val unavailableBeltToast = remember(context) {
        Toast.makeText(context, "No techniques available for this belt yet.", Toast.LENGTH_SHORT)
    }
    DisposableEffect(unavailableBeltToast) {
        onDispose { unavailableBeltToast.cancel() }
    }
    val scope = rememberCoroutineScope()
    var playbackSpeedStep by remember { mutableIntStateOf(10) }
    val db = remember { DbProvider.get(context) }
    val blackBeltsEnabled by remember(db) {
        db.contentMetaDao().observeValue(DbProvider.BLACK_BELTS_ENABLED_KEY)
            .map { it == 1 }
    }.collectAsState(initial = false)

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

    // screen three (compendium)
    val compendiumSections by db.compendiumSectionDao()
        .getAll()
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

    val stanceItems = remember(tkdItemsRoom) {
        tkdItemsRoom.filter { it.category == TKDItemCategory.STANCES }
    }

    val patternItems = remember(tkdItemsRoom) {
        tkdItemsRoom.filter { it.category == TKDItemCategory.ALL_PATTERNS }
    }

    val techniqueItems = remember(tkdItemsRoom) {
        tkdItemsRoom.filter {
            it.category != TKDItemCategory.STANCES
        }
    }

    val highestAvailableDan = remember(techniqueItems) {
        techniqueItems.filter {
            it.category == TKDItemCategory.DEFENSIVE ||
                it.category == TKDItemCategory.OFFENSIVE ||
                it.category == TKDItemCategory.KICKS ||
                it.category == TKDItemCategory.ALL_PATTERNS
        }.minOfOrNull { it.beltLevel }?.let { (-it).coerceAtLeast(0) } ?: 0
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

    val pagerState = rememberPagerState(initialPage = initialPage) { 3 }
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

    //Hoisted for compendium
    var selectedCompendiumTabKey by rememberSaveable { mutableStateOf("tenets") }
    var selectedCompendiumEntryKey by rememberSaveable { mutableStateOf<String?>(null) }
    val expandedEntries = remember {
        mutableStateMapOf<String, Boolean>()
    }

    val updateState by contentUpdateViewModel.uiState.collectAsState()
    val inventoryState by contentInventoryViewModel.uiState.collectAsState()

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
                Box(Modifier.width(20.dp * 3)) {
                    IconButton(onClick = onBackToOnboarding) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = "Back to onboarding",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
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
                .map { prefs ->
                    val level = prefs[BELT_LEVEL] ?: 9 // default 9 = white
                    (if (level == 0) -1 else level).coerceIn(if (blackBeltsEnabled) -9 else 1, 9)
                }
                .collectAsState(initial = 9)

            HorizontalPager(state = pagerState) { currentPage ->
                when (currentPage) {
                    0 -> ScreenOne(
                        selectedTab = selectedTabBody,
                        selectedFilters = selectedFiltersBody,
                        bodyItems = bodyItems
                    )
                    1 -> ScreenTwo(
                        playbackSpeedStep = playbackSpeedStep,
                        onPlaybackSpeedStepChange = { playbackSpeedStep = it.coerceIn(1, 30) },
                        tkdItemsRoom = techniqueItems,
                        selectedTab = selectedTab,
                        selectedPattern = selectedPattern,
                        selectedFilters = selectedFilters,
                        patternStr = patternStr,
                        beltGup = beltGup,
                        onOpenCompendiumEntry = { entryKey ->
                            selectedCompendiumTabKey = "patterns"
                            selectedCompendiumEntryKey = entryKey
                            selectedTabIndex = 2
                        }
                    )
                    2 -> ScreenThree(
                        stanceItems = stanceItems,
                        patternItems = patternItems,
                        compendiumSections = compendiumSections,
                        beltGup = beltGup,
                        selectedTabKey = selectedCompendiumTabKey,
                        selectedEntryKey = selectedCompendiumEntryKey,
                        onSelectedTabChange = { selectedCompendiumTabKey = it },
                        onSelectedEntryChange = { selectedCompendiumEntryKey = it },
                        expandedEntries = expandedEntries
                    )
                }
            }
            SettingsSheet(
                isOpen = isSheetOpen,
                beltGup = beltGup,
                updateState = updateState,
                blackBeltsEnabled = blackBeltsEnabled,
                inventoryState = inventoryState,
                onRefreshInventory = contentInventoryViewModel::refresh,
                onCheckAvailableContent = contentInventoryViewModel::checkForAvailableContent,
                onDismiss = { isSheetOpen = false },
                onSelectBeltLevel = { uiLevel ->
                    val level = uiLevel.coerceIn(1, if (blackBeltsEnabled) 18 else 9)
                    val newGup = if (level <= 9) 10 - level else 9 - level
                    if (newGup < 0 && -newGup > highestAvailableDan) {
                        unavailableBeltToast.show()
                    } else {
                        scope.launch {
                            context.dataStore.edit { it[BELT_LEVEL] = newGup }
                        }
                    }
                },
                onUpdateClick = contentUpdateViewModel::updateContent
            )
        }
    }
}

val BELT_LEVEL = intPreferencesKey("belt_level")

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
    playbackSpeedStep: Int,
    onPlaybackSpeedStepChange: (Int) -> Unit,
              tkdItemsRoom: List<TKDItem>,
              selectedTab: MutableState<String>,
              selectedPattern: MutableState<String>,
              selectedFilters: MutableState<Set<String>>,
              patternStr: String,
              beltGup: Int,
              onOpenCompendiumEntry: (String) -> Unit
              ) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier.fillMaxSize()
    ) {
        Techniques(
            playbackSpeedStep = playbackSpeedStep,
            onPlaybackSpeedStepChange = onPlaybackSpeedStepChange,
            tkdItemsRoom = tkdItemsRoom,
            selectedTab = selectedTab,
            selectedPattern = selectedPattern,
            selectedFilters = selectedFilters,
            patternStr = patternStr,
            beltGup = beltGup,
            onOpenCompendiumEntry = onOpenCompendiumEntry
        )
    }
}

@Composable
fun ScreenThree(
    stanceItems: List<TKDItem>,
    patternItems: List<TKDItem>,
    compendiumSections: List<CompendiumSectionEntity>,
    beltGup: Int,
    selectedTabKey: String,
    selectedEntryKey: String?,
    onSelectedTabChange: (String) -> Unit,
    onSelectedEntryChange: (String?) -> Unit,
    expandedEntries: SnapshotStateMap<String, Boolean>
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier.fillMaxSize()
    ) {
        Compendium(
            stanceItems = stanceItems,
            patternItems = patternItems,
            compendiumSections = compendiumSections,
            beltGup = beltGup,
            selectedTabKey = selectedTabKey,
            selectedEntryKey = selectedEntryKey,
            onSelectedTabChange = onSelectedTabChange,
            onSelectedEntryChange = onSelectedEntryChange,
            expandedEntries = expandedEntries
        )
    }
}
