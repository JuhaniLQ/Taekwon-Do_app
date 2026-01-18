package fi.tkd.itfun

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max


@Composable
fun Body(selectedTab: MutableState<String>,
         selectedFilters: MutableState<Set<String>>,
         bodyItems: List<BodyItem>
) {
    val regions = BodyRegion.entries

    Column {
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
                        selectedTab.value = vital          // just open submenu
                    }
                )
            }


            Spacer(modifier = Modifier.weight(1f))

            // filter icons
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
        val selectedWheelIndex = rememberSaveable { mutableIntStateOf(0) }

        val currentTab = selectedTab.value          // "All" or "Vital"
        val activeFilters = selectedFilters.value   // Set<String> of region names
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
        val currentItem = filteredBodyItems
            .getOrNull(selectedWheelIndex.intValue)

        val diagramResId =
            diagramImages[currentItem?.region] ?: R.drawable.questionmark

        val highlightKey = currentItem?.code

        DiagramWithHighlight(
            diagramResId = diagramResId,
            bodyCode = highlightKey
        )

        Spacer(Modifier.height(16.dp))

        WheelPicker(
            items = filteredBodyItems,
            modifier = Modifier.fillMaxWidth(),
            onCenteredIndexChanged = { selectedWheelIndex.intValue = it }
        )
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
    BodyRegion.TORSO to R.drawable.torso,
    BodyRegion.HEAD to R.drawable.head
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
private fun WheelPicker(
    items: List<BodyItem>,
    modifier: Modifier = Modifier,
    onCenteredIndexChanged: (Int) -> Unit
) {
    val scope = rememberCoroutineScope()

    val rowHeight: Dp = 52.dp

    val above = 2
    val below = 6
    val visibleRows = above + 1 + below // = 7

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
                    val distancePx = abs(itemCenter - selectionCenter)
                    val rowsAway = distancePx / max(item.size.toFloat(), 1f)

                    val scale = (1.12f - 0.12f * rowsAway).coerceIn(0.72f, 1.12f)
                    val alpha = (1.00f - 0.18f * rowsAway).coerceIn(0.25f, 1.00f)

                    put(item.index, scale to alpha)
                }
            }
        }
    }
    val centerIndex by remember(listState, rowHeightPx, topPadPx) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            if (layoutInfo.visibleItemsInfo.isEmpty()) return@derivedStateOf 0

            val selectionCenter = layoutInfo.viewportStartOffset + topPadPx + rowHeightPx / 2f

            layoutInfo.visibleItemsInfo.minBy { item ->
                val itemCenter = item.offset + item.size / 2
                abs(itemCenter - selectionCenter)
            }.index
        }
    }

    LaunchedEffect(centerIndex) {
        onCenteredIndexChanged(centerIndex)
    }


    Box(
        modifier = modifier.height(rowHeight * visibleRows)
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(top = topPad, bottom = bottomPad),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(items, key = { i, _ -> i }) { index, item ->
                val (scale, alpha) = transformsByIndex[index] ?: (0.72f to 0.25f)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowHeight)
                        .clickable {
                            scope.launch {
                                listState.animateScrollToItem(index)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.nameKo,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        },
                        color = MaterialTheme.colorScheme.primary
                    )
                }

            }
        }


        val lineColor = MaterialTheme.colorScheme.primary
        val lineThickness = 2.dp
        val lineInset = 24.dp

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = topPad)
                .fillMaxWidth()
                .height(rowHeight)
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
    "KNIFE_HAND"   to EllipseNorm(cx = 0.467f, cy = 0.183f, rx = 0.154f, ry = 0.050f),
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
    "BALL_OF_FOOT" to EllipseNorm(cx = 0.775f, cy = 0.899f, rx = 0.154f, ry = 0.099f)
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

