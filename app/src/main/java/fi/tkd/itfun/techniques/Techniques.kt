package fi.tkd.itfun.techniques

import androidx.compose.foundation.text.input.rememberTextFieldState

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.DpOffset
import kotlinx.coroutines.delay
import fi.tkd.itfun.R
import fi.tkd.itfun.shared.CategoryFilterImage
import fi.tkd.itfun.shared.ImageFilterButton
import fi.tkd.itfun.shared.SearchBar

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Techniques(
               tkdItemsRoom: List<TKDItem>,
               selectedTab: MutableState<String>,
               selectedPattern: MutableState<String>,
               selectedFilters: MutableState<Set<String>>,
               patternStr: String,
               beltGup: Int,
               onOpenCompendiumEntry: (String) -> Unit,
               playbackSpeedStep: Int = 10,
               onPlaybackSpeedStepChange: (Int) -> Unit = {}) {

    var menuCategory by remember { mutableStateOf(TKDItemCategory.ALL_PATTERNS) }
    val availableMenuNames by remember(tkdItemsRoom, beltGup, menuCategory) {
        derivedStateOf {
            tkdItemsRoom
                .asSequence()
                .filter { it.category == menuCategory }
                .mapNotNull { item ->
                    val name = if (menuCategory == TKDItemCategory.ALL_PATTERNS) {
                        item.pattern.firstOrNull() ?: return@mapNotNull null
                    } else {
                        item.koreanName
                    }
                    name to item.beltLevel
                }
                .filter { (_, requiredBelt) ->
                    menuCategory != TKDItemCategory.ALL_PATTERNS || requiredBelt >= beltGup
                }
                .map { (name, _) -> name }
                .distinct()
                .toList()
        }
    }

    var hoveringCode by rememberSaveable { mutableStateOf<Int?>(null) }
    var hoverSourceTop by remember { mutableFloatStateOf(0f) }
    var containerTop by remember { mutableFloatStateOf(0f) }
    var hoveringHeight by remember { mutableIntStateOf(0) }
    val hoveringItem = tkdItemsRoom.firstOrNull { it.techniqueCode == hoveringCode }

    val expandedByCode = remember { mutableStateMapOf<Int, Boolean>() }

    val isSubMenuVisible = remember { mutableStateOf(false) }

    fun selectSection(section: String) {
        if (selectedTab.value != section) {
            hoveringCode = null
        }
        if (section != patternStr && section != "Sparring") {
            selectedPattern.value = ""
        }
        selectedTab.value = section
        isSubMenuVisible.value = false
    }

    val filterCategories = listOf(
        TKDItemCategory.KICKS,
        TKDItemCategory.OFFENSIVE,
        TKDItemCategory.DEFENSIVE
    )
    val searchInput = rememberTextFieldState()                    // text and cursor selection
    val searchQuery = remember { mutableStateOf("") }              // debounced version
    val listState = rememberLazyListState()

    LaunchedEffect(searchInput.text.toString()) {
        delay(130)
        searchQuery.value = searchInput.text.toString()
    }
    LaunchedEffect(
        searchQuery.value,
        selectedFilters.value,
        selectedTab.value,
        beltGup) {
        listState.scrollToItem(0)
    }

    LaunchedEffect(
        selectedFilters.value,
        selectedPattern.value,
        selectedTab.value,
        beltGup
    ) {
        expandedByCode.clear()
    }


    Box(Modifier.fillMaxSize().onGloballyPositioned {
        containerTop = it.boundsInRoot().top
    }) {
        Column(Modifier.fillMaxSize()) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                // Two speed buttons, speed text, inter-group gap and screen-edge padding.
                val maxSectionWidth = (maxWidth - (30.dp * 2 + 52.dp + 5.dp + 3.dp * 2)).coerceAtLeast(0.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp, top = 5.dp, start = 3.dp, end = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Row(
                        modifier = Modifier
                        .widthIn(max = maxSectionWidth)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ImageFilterButton(
                            imageResId = R.drawable.techniques,
                            contentDescription = "All techniques",
                            isSelected = selectedTab.value == "All",
                            onClick = {
                                selectSection("All")
                            }
                        )
                        Box {
                            ImageFilterButton(
                                imageResId = R.drawable.patterns,
                                contentDescription = "Patterns",
                                isSelected = selectedTab.value == patternStr,
                                onClick = {
                                    menuCategory = TKDItemCategory.ALL_PATTERNS
                                    isSubMenuVisible.value = true
                                }
                            )
                            MaterialTheme(
                                shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(16.dp))
                            ) {
                                DropdownMenu(
                                    offset = DpOffset(x = 0.dp, y = 1.dp),
                                    expanded = isSubMenuVisible.value,
                                    onDismissRequest = { isSubMenuVisible.value = false },
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.primary,//.darker(),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .layout { measurable, constraints ->
                                            // Material 3 1.1.1 adds 8.dp above and below the items.
                                            val inset = 8.dp.roundToPx()
                                            val menu = measurable.measure(constraints)
                                            layout(menu.width, (menu.height - inset * 2).coerceAtLeast(0)) {
                                                menu.placeRelative(0, -inset)
                                            }
                                        }
                                ) {
                                    availableMenuNames.forEachIndexed { index, pattern ->
                                        if (index > 0) {
                                            val touchesSelection = pattern == selectedPattern.value ||
                                                availableMenuNames[index - 1] == selectedPattern.value
                                            Divider(
                                                modifier = Modifier.padding(
                                                    horizontal = if (touchesSelection) 0.dp else 3.dp
                                                ),
                                                thickness = 1.dp,
                                                color = MaterialTheme.colorScheme.primary)
                                        }
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = pattern,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontSize = 22.sp
                                                )
                                            },
                                            onClick = {
                                                selectedPattern.value = pattern
                                                isSubMenuVisible.value = false
                                                selectSection(
                                                    if (menuCategory == TKDItemCategory.ALL_PATTERNS) patternStr
                                                    else "Sparring"
                                                )
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    if (pattern == selectedPattern.value) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.surface
                                                )
                                        )

                                    }
                                }
                            }
                        }
                        listOf(
                            "Sparring" to R.drawable.sparring,
                            "Crushing" to R.drawable.crushing,
                            "Special" to R.drawable.special
                        ).forEach { (section, image) ->
                            ImageFilterButton(
                                imageResId = image,
                                contentDescription = section,
                                isSelected = selectedTab.value == section,
                                onClick = {
                                    if (section == "Sparring") {
                                        menuCategory = TKDItemCategory.SPARRING
                                        isSubMenuVisible.value = true
                                    } else {
                                        selectSection(section)
                                    }
                                }
                            )
                        }
                    }
                    PlaybackSpeedControls(playbackSpeedStep, onPlaybackSpeedStepChange, Modifier.weight(1f))
                }
            }


            val currentTab = selectedTab.value
            val currentPattern = selectedPattern.value
            val activeFilters = selectedFilters.value

            val filteredItems by remember(
                tkdItemsRoom,
                currentTab,
                currentPattern,
                activeFilters,
                searchQuery.value,
                beltGup
            ) {
                derivedStateOf {
                    fun String.normalizeForSearch(): String =
                        replace('\u00A0', ' ')   // NBSP to normal space
                            .trim()
                            .lowercase()

                    val q = searchQuery.value.normalizeForSearch()

                    if (currentTab == patternStr) {
                        tkdItemsRoom.filter { tkdItem ->
                            tkdItem.category == TKDItemCategory.ALL_PATTERNS &&
                                    (currentPattern.isEmpty() || tkdItem.pattern.contains(currentPattern)) &&
                                    tkdItem.beltLevel >= beltGup
                        }
                    } else if (currentTab == "All") {
                        tkdItemsRoom.filter { tkdItem ->
                            val matchesTab =
                                currentTab == "All" &&
                                        tkdItem.category in filterCategories

                            val matchesCategory =
                                activeFilters.isEmpty() || activeFilters.contains(tkdItem.category.name)

                            val matchesSearch =
                                q.isEmpty() ||
                                        tkdItem.name.normalizeForSearch().contains(q) ||
                                        tkdItem.koreanName.normalizeForSearch().contains(q)

                            val matchesBelt = tkdItem.beltLevel >= beltGup

                            matchesTab && matchesCategory && matchesSearch && matchesBelt
                        }
                    } else if (currentTab == "Sparring") {
                        tkdItemsRoom.filter {
                            it.category == TKDItemCategory.SPARRING &&
                                (currentPattern.isEmpty() || it.koreanName == currentPattern)
                        }
                    } else if (currentTab == "Special") {
                        tkdItemsRoom.filter { it.category == TKDItemCategory.SPECIAL }
                    } else if (currentTab == "Crushing") {
                        tkdItemsRoom.filter { it.category == TKDItemCategory.CRUSHING }
                    } else {
                        // These sections have no content source yet.
                        emptyList()
                    }
                }
            }

            val listItems = filteredItems.filterNot { it.techniqueCode == hoveringCode }
            val categoryCounts = listItems.groupingBy { it.category }.eachCount()
            val resultCounts = listOf(
                "blocks" to (categoryCounts[TKDItemCategory.DEFENSIVE] ?: 0),
                "attacks" to (categoryCounts[TKDItemCategory.OFFENSIVE] ?: 0),
                "kicks" to (categoryCounts[TKDItemCategory.KICKS] ?: 0)
            )

            if (currentTab == "All") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp, top = 1.dp, start = 3.dp, end = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SearchBar(
                        searchInput = searchInput,
                        resultCounts = resultCounts,
                        totalItems = tkdItemsRoom.size,
                        filters = {
                            Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                                filterCategories.forEach { category ->
                                    CategoryFilterImage(
                                        categoryName = category.name,
                                        images = techniqueCategoryImages.mapKeys { it.key.name },
                                        isSelected = selectedFilters.value.contains(category.name),
                                        onClick = {
                                            selectedFilters.value =
                                                if (selectedFilters.value.contains(category.name)) {
                                                    selectedFilters.value - category.name
                                                } else {
                                                    selectedFilters.value + category.name
                                                }
                                        }
                                    )
                                }
                            }
                        }
                    )
                }
            }

            val bottomSpace = LocalConfiguration.current.screenHeightDp.dp / 4
            val hoveringSpace = with(LocalDensity.current) {
                if (hoveringItem != null) hoveringHeight.toDp() else 0.dp
            }
            TechniqueCardList(state = listState,
                userScrollEnabled = currentTab != patternStr && currentTab != "Sparring",
                contentPadding = PaddingValues(bottom = bottomSpace + hoveringSpace),
                modifier = Modifier.padding(bottom = 8.dp)) {
                items(
                    items = listItems,
                    key = { it.techniqueCode }
                ) { tkdItem ->
                    val isPattern = tkdItem.category == TKDItemCategory.ALL_PATTERNS
                    val expanded = if (isPattern) true else (expandedByCode[tkdItem.techniqueCode] ?: false)

                    var cardTop by remember { mutableFloatStateOf(0f) }
                    Technique(
                        playbackSpeed = playbackSpeedStep / 10f,
                        tkdItem = tkdItem,
                        expanded = expanded,
                        onExpandedChange = { newValue ->
                            if (!isPattern) {
                                expandedByCode[tkdItem.techniqueCode] = newValue
                            }
                        },
                        onOpenCompendiumEntry = onOpenCompendiumEntry,
                        modifier = Modifier.onGloballyPositioned { cardTop = it.boundsInRoot().top },
                        onHoverClick = {
                            hoverSourceTop = cardTop - containerTop
                            hoveringCode = tkdItem.techniqueCode
                        }
                    )
                }
            }
        }
        if (hoveringItem != null) {
            HoveringTechniqueCard(
                playbackSpeed = playbackSpeedStep / 10f,
                item = hoveringItem,
                sourceTop = hoverSourceTop,
                onReturn = { hoveringCode = null },
                onHeightChanged = { hoveringHeight = it },
                onOpenCompendiumEntry = onOpenCompendiumEntry
            )
        }
    }
}

internal val techniqueCategoryImages = mapOf(
    TKDItemCategory.KICKS to R.drawable.kicks,
    TKDItemCategory.OFFENSIVE to R.drawable.offensive,
    TKDItemCategory.DEFENSIVE to R.drawable.defensive,
    TKDItemCategory.SPECIAL to R.drawable.special,
    TKDItemCategory.CRUSHING to R.drawable.crushing
)
