package fi.tkd.itfun

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import kotlinx.coroutines.delay

@Composable
fun Techniques(
               tkdItemsRoom: List<TKDItem>,
               selectedTab: MutableState<String>,
               selectedPattern: MutableState<String>,
               selectedFilters: MutableState<Set<String>>,
               patternStr: String,
               beltGup: Int,
               onOpenCompendiumEntry: (String) -> Unit) {

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
        selectedTab.value = section
        isSubMenuVisible.value = false
    }

    val filterCategories = listOf(
        TKDItemCategory.KICKS,
        TKDItemCategory.OFFENSIVE,
        TKDItemCategory.DEFENSIVE
    )
    val searchInput = rememberSaveable { mutableStateOf("") }      // what user types
    val searchQuery = remember { mutableStateOf("") }              // debounced version
    val listState = rememberLazyListState()

    LaunchedEffect(searchInput.value) {
        delay(130)
        searchQuery.value = searchInput.value
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp, top = 5.dp, start = 3.dp, end = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ImageFilterButton(
                        imageResId = R.drawable.techniques,
                        contentDescription = "All techniques",
                        isSelected = selectedTab.value == "All",
                        onClick = {
                            selectSection("All")
                            selectedPattern.value = ""
                        }
                    )
                    ImageFilterButton(
                        imageResId = R.drawable.patterns,
                        contentDescription = "Patterns",
                        isSelected = selectedTab.value == patternStr,
                        onClick = {
                            menuCategory = TKDItemCategory.ALL_PATTERNS
                            isSubMenuVisible.value = true
                        }
                    )
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
                if (selectedTab.value == "All") {
                    Row(
                        modifier = Modifier.padding(start = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
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
                }
            }


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
                            .background(MaterialTheme.colorScheme.primary.darker())
                    ) {
                        availableMenuNames.forEach { pattern ->
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
                                    selectSection(
                                        if (menuCategory == TKDItemCategory.ALL_PATTERNS) patternStr
                                        else "Sparring"
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                            )

                        }
                    }
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
            val resultSummary = if (currentTab != patternStr) {
                "blocks ${categoryCounts[TKDItemCategory.DEFENSIVE] ?: 0}\n" +
                    "attacks ${categoryCounts[TKDItemCategory.OFFENSIVE] ?: 0}\n" +
                    "kicks ${categoryCounts[TKDItemCategory.KICKS] ?: 0}"
            } else ""

            if (currentTab == "All") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 5.dp, top = 5.dp, start = 15.dp, end = 15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SearchBar(
                        searchInput = searchInput.value,
                        onInputChange = { searchInput.value = it },
                        resultSummary = resultSummary
                    )
                }
            }

            val bottomSpace = LocalConfiguration.current.screenHeightDp.dp / 4
            val hoveringSpace = with(LocalDensity.current) {
                if (hoveringItem != null) hoveringHeight.toDp() else 0.dp
            }
            LazyColumn(state = listState,
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
