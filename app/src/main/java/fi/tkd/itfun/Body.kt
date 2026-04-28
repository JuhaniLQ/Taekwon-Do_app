package fi.tkd.itfun

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
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

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .onGloballyPositioned {
                    rowBottomY = it.positionInParent().y.toInt() + it.size.height
                }
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

            Spacer(modifier = Modifier.weight(1f))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier
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

enum class BodyRegion { HEAD, TORSO, ARM, LEG }

data class BodyItem(
    val code: String,
    val region: BodyRegion,
    val canBeVitalPoint: Boolean,
    val nameEn: String,
    val nameKo: String
)


@Composable
fun BodyWheelPicker(
    items: List<BodyItem>,
    selectedCode: String?,
    modifier: Modifier = Modifier,
    onSelectedCodeChange: (String?) -> Unit,
    onReplayRequested: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    val rowHeight = 50.dp
    val selectionHeight = 50.dp
    val above = 2
    val below = 6
    val visibleRows = above + 1 + below

    val listState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val density = LocalDensity.current
    val rowHeightPx = remember(rowHeight, density) { with(density) { rowHeight.toPx() } }
    val topPad = rowHeight * above
    val bottomPad = rowHeight * below
    val topPadPx = remember(topPad, density) { with(density) { topPad.toPx() } }

    val transformsByIndex by remember(listState, rowHeightPx, topPadPx) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val selectionCenter = layoutInfo.viewportStartOffset + topPadPx + rowHeightPx / 2f

            buildMap {
                for (item in layoutInfo.visibleItemsInfo) {
                    val itemCenter = item.offset + item.size / 2
                    val distancePx = kotlin.math.abs(itemCenter - selectionCenter)
                    val rowsAway = distancePx / kotlin.math.max(item.size.toFloat(), 1f)

                    val scale = (1.12f - 0.12f * rowsAway).coerceIn(0.72f, 1.12f)
                    val alpha = (1.00f - 0.18f * rowsAway).coerceIn(0.25f, 1.00f)

                    put(item.index, scale to alpha)
                }
            }
        }
    }

    val centeredIndex by remember(listState, rowHeightPx, topPadPx) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            if (layoutInfo.visibleItemsInfo.isEmpty()) return@derivedStateOf -1

            val selectionCenter = layoutInfo.viewportStartOffset + topPadPx + rowHeightPx / 2f

            layoutInfo.visibleItemsInfo.minBy { item ->
                val itemCenter = item.offset + item.size / 2
                kotlin.math.abs(itemCenter - selectionCenter)
            }.index
        }
    }

    LaunchedEffect(items) {
        if (items.isEmpty()) return@LaunchedEffect

        val targetIndex = items.indexOfFirst { it.code == selectedCode }
            .takeIf { it >= 0 }
            ?: 0

        listState.scrollToItem(targetIndex)
    }

    LaunchedEffect(centeredIndex, items, selectedCode) {
        val newCode = items.getOrNull(centeredIndex)?.code
        if (newCode != selectedCode) {
            onSelectedCodeChange(newCode)
        }
    }

    val nestedConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset = available

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity = available
        }
    }

    val lineColor = MaterialTheme.colorScheme.primary
    val lineThickness = 2.dp
    val lineInset = 24.dp

    Box(
        modifier = modifier
            .height(rowHeight * visibleRows)
            .nestedScroll(nestedConnection)
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(top = topPad, bottom = bottomPad),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                count = items.size,
                key = { index -> items[index].code }
            ) { index ->
                val item = items[index]
                val (scale, alpha) = transformsByIndex[index] ?: (0.72f to 0.25f)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowHeight)
                        .padding(horizontal = lineInset, vertical = 10.dp)
                        .clickable {
                            scope.launch {
                                val currentCenteredCode = items.getOrNull(centeredIndex)?.code
                                if (item.code == currentCenteredCode) {
                                    onReplayRequested(item.code)
                                } else {
                                    listState.animateScrollToItem(index)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.nameKo,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        },
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = topPad - (selectionHeight - rowHeight) / 2)
                .fillMaxWidth()
                .height(selectionHeight)
        ) {
            Divider(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = lineInset),
                thickness = lineThickness,
                color = lineColor
            )
            Divider(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = lineInset),
                thickness = lineThickness,
                color = lineColor
            )
        }
    }
}

// 1) Coordinate model (normalized 0f..1f relative to displayed image box)
data class EllipseNorm(
    val cx: Float,  // center x (0..1)
    val cy: Float,  // center y (0..1)
    val rx: Float,  // radius x (0..1)
    val ry: Float   // radius y (0..1)
)

// 2) Hardcoded highlight coordinates
private val bodyHighlights = mapOf(
    //ARM
    "KNIFE_HAND" to EllipseNorm(cx = 0.467f, cy = 0.183f, rx = 0.154f, ry = 0.050f),
    "REVERSE_KNIFE_HAND" to EllipseNorm(cx = 0.853f, cy = 0.839f, rx = 0.145f, ry = 0.106f),
    "PALM" to EllipseNorm(cx = 0.425f, cy = 0.306f, rx = 0.075f, ry = 0.050f),
    "FINGERS" to EllipseNorm(cx = 0.600f, cy = 0.06f, rx = 0.108f, ry = 0.044f),
    "FINGERTIPS" to EllipseNorm(cx = 0.875f, cy = 0.067f, rx = 0.125f, ry = 0.056f),
    "THUMB" to EllipseNorm(cx = 0.912f, cy = 0.499f, rx = 0.087f, ry = 0.055f),
    "FOREARM" to EllipseNorm(cx = 0.396f, cy = 0.939f, rx = 0.121f, ry = 0.050f),
    "INNER_FOREARM" to EllipseNorm(cx = 0.637f, cy = 0.905f, rx = 0.137f, ry = 0.094f),
    "OUTER_FOREARM" to EllipseNorm(cx = 0.400f, cy = 0.439f, rx = 0.100f, ry = 0.078f),
    "ELBOW" to EllipseNorm(cx = 0.096f, cy = 0.922f, rx = 0.087f, ry = 0.056f),
    "SHOULDER" to EllipseNorm(cx = 0.3f, cy = 0.100f, rx = 0.127f, ry = 0.050f),

    //LEG
    "KNEE" to EllipseNorm(cx = 0.550f, cy = 0.194f, rx = 0.075f, ry = 0.050f),
    "SHIN" to EllipseNorm(cx = 0.583f, cy = 0.328f, rx = 0.075f, ry = 0.050f),
    "ANKLE" to EllipseNorm(cx = 0.242f, cy = 0.567f, rx = 0.092f, ry = 0.056f),
    "INSTEP" to EllipseNorm(cx = 0.629f, cy = 0.439f, rx = 0.096f, ry = 0.050f),
    "TOES" to EllipseNorm(cx = 0.679f, cy = 0.539f, rx = 0.071f, ry = 0.050f),
    "TOE_TIPS" to EllipseNorm(cx = 0.833f, cy = 0.361f, rx = 0.125f, ry = 0.061f),
    "HEEL_BACK" to EllipseNorm(cx = 0.200f, cy = 0.711f, rx = 0.150f, ry = 0.067f),
    "FOOT_BLADE" to EllipseNorm(cx = 0.250f, cy = 0.867f, rx = 0.167f, ry = 0.056f),
    "HEEL_SOLE" to EllipseNorm(cx = 0.508f, cy = 0.949f, rx = 0.150f, ry = 0.049f),
    "BALL_OF_FOOT" to EllipseNorm(cx = 0.775f, cy = 0.899f, rx = 0.154f, ry = 0.099f),

    //Torso
    "SOLAR_PLEXUS" to EllipseNorm(cx = 0.850f, cy = 0.233f, rx = 0.125f, ry = 0.056f),
    "TEMPLE" to EllipseNorm(cx = 0.804f, cy = 0.167f, rx = 0.087f, ry = 0.056f)
)

@Composable
fun DiagramWithHighlight(
    diagramResId: Int,
    bodyCode: String?,
    modifier: Modifier = Modifier
) {
    val ellipse = bodyCode?.let { bodyHighlights[it] }
    val primary = MaterialTheme.colorScheme.primary

    // diagram size
    val iw = 1200f
    val ih = 900f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)   // <-- makes the diagram area stable
            .padding(top = 12.dp)
    ) {
        Image(
            painter = painterResource(diagramResId),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.matchParentSize()
        )

        if (ellipse != null) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val bw = size.width
                val bh = size.height

                val s = minOf(bw / iw, bh / ih)
                val dw = iw * s
                val dh = ih * s
                val ox = (bw - dw) / 2f
                val oy = (bh - dh) / 2f

                val cx = ox + ellipse.cx * dw
                val cy = oy + ellipse.cy * dh
                val rx = ellipse.rx * dw
                val ry = ellipse.ry * dh

                drawOval(
                    color = primary.copy(alpha = 0.50f),
                    topLeft = Offset(cx - rx, cy - ry),
                    size = Size(rx * 2f, ry * 2f)
                )
                drawOval(
                    color = primary.copy(alpha = 0.85f),
                    topLeft = Offset(cx - rx, cy - ry),
                    size = Size(rx * 2f, ry * 2f),
                    style = Stroke(width = 3f)
                )
            }
        }
    }
}

