package fi.tkd.itfun

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch

@Composable
fun Body(selectedTab: MutableState<String>,
         selectedFilters: MutableState<Set<String>>,
         bodyItems: List<BodyItem>
) {
    val regions = BodyRegion.entries

    var selectedCode by rememberSaveable { mutableStateOf<String?>(null) }
    var rowBottomY by remember { mutableStateOf(0) }
    var diagramBottomY by remember { mutableStateOf(0) }
    val wheelState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val currentTab = selectedTab.value
    val activeFilters = selectedFilters.value

    val filteredBodyItems by remember(
        bodyItems,
        currentTab,
        activeFilters
    ) {
        derivedStateOf {
            bodyItems.filter { bodyItem ->

                val matchesTab =
                    when (currentTab) {
                        "All" -> true
                        "Vital" -> bodyItem.canBeVitalPoint
                        else -> true
                    }

                val matchesRegion =
                    activeFilters.isEmpty() ||
                            activeFilters.contains(bodyItem.region.name)

                matchesTab && matchesRegion
            }
        }
    }


    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        val counts = filteredBodyItems.groupingBy { it.region }.eachCount()
        Box(
            modifier = Modifier.fillMaxWidth().onGloballyPositioned {
                rowBottomY = it.positionInParent().y.toInt() + it.size.height
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp, top = 5.dp, start = 3.dp, end = 10.dp),

                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Genre(
                        text = "All",
                        isSelected = selectedTab.value == "All",
                        onClick = {
                            selectedTab.value = "All"
                        }
                    )

                    val vital = "Vital"
                    Genre(
                        text = vital,
                        isSelected = selectedTab.value == vital,
                        onClick = {
                            selectedTab.value = vital
                        }
                    )
                }

                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "head area ${counts[BodyRegion.HEAD] ?: 0}\n" +
                            "torso area ${counts[BodyRegion.TORSO] ?: 0}\n" +
                            "arm area ${counts[BodyRegion.ARM] ?: 0}\n" +
                            "leg area ${counts[BodyRegion.LEG] ?: 0}",
                        fontSize = 13.sp,
                        lineHeight = 12.sp,
                        textAlign = TextAlign.End,
                        softWrap = false,
                        maxLines = 4,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.End),
                        modifier = Modifier.weight(1f, fill = false)
                            .widthIn(max = 40.dp * regions.size + 5.dp * (regions.size - 1))
                    ) {
                        items(regions) { category ->
                            CategoryFilterImage(
                                categoryName = category.name,
                                images = images.mapKeys { it.key.name },
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

        LaunchedEffect(filteredBodyItems) {
            val currentStillExists = selectedCode != null &&
                    filteredBodyItems.any { it.code == selectedCode }

            when {
                filteredBodyItems.isEmpty() -> selectedCode = null
                currentStillExists -> Unit
                else -> selectedCode = filteredBodyItems.first().code
            }
        }

        val currentItem = filteredBodyItems.firstOrNull { it.code == selectedCode }

        val diagramResId =
            diagramImages[currentItem?.region] ?: R.drawable.questionmark

        val highlightKey = currentItem?.code

        DiagramWithHighlight(
            diagramResId = diagramResId,
            bodyCode = highlightKey,
            selectableItems = filteredBodyItems.filter { it.region == currentItem?.region },
            onBodyPartClick = { code ->
                val index = filteredBodyItems.indexOfFirst { it.code == code }
                if (index >= 0) {
                    scope.launch { wheelState.scrollToItem(index) }
                }
            },
            modifier = Modifier
                .offset { IntOffset(0, rowBottomY) }
                .onGloballyPositioned {
                    diagramBottomY = it.positionInParent().y.toInt() + it.size.height
                }
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .offset {
                    IntOffset(
                        x = 0,
                        y = diagramBottomY + 16.dp.roundToPx()
                    )
                }
                .requiredHeight(450.dp)
        ) {
            BodyWheelPicker(
                items = filteredBodyItems,
                selectedCode = selectedCode,
                listState = wheelState,
                modifier = Modifier.fillMaxWidth(),
                onSelectedCodeChange = { selectedCode = it }
            )
        }
    }
} 

private val images = mapOf(
    BodyRegion.LEG to R.drawable.leg,
    BodyRegion.ARM to R.drawable.arm,
    BodyRegion.TORSO to R.drawable.torso,
    BodyRegion.HEAD to R.drawable.head
)

private val diagramImages = mapOf(
    BodyRegion.LEG to R.drawable.leg_tooltips,
    BodyRegion.ARM to R.drawable.arm_tooltips,
    BodyRegion.TORSO to R.drawable.torso_tooltips,
    BodyRegion.HEAD to R.drawable.head_tooltips
)
