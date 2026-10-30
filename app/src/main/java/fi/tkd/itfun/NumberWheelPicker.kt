package fi.tkd.itfun

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Divider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.rememberTextMeasurer
import kotlinx.coroutines.launch
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.abs
import kotlin.math.max

internal object NumberWheelDimensions {
    val RowHeight = 32.dp
    const val RowsAbove = 4
    const val RowsBelow = 6
    val Height = RowHeight * (RowsAbove + 1 + RowsBelow)
}

@Composable
fun NumberWheelPicker(
    count: Int,
    labels: List<String>? = null,
    modifier: Modifier = Modifier,
    onCenteredIndexChanged: (Int) -> Unit = {},
    onReplayRequested: (Int) -> Unit = {}   // NEW
) {
    val scope = rememberCoroutineScope()

    val rowHeight = NumberWheelDimensions.RowHeight

    val listState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = LocalTextStyle.current
    val extraLabelWidth = remember(textMeasurer, labelStyle, density) {
        textMeasurer.measure("00", style = labelStyle, maxLines = 1).size.width
    }
    val rowHeightPx = remember(rowHeight, density) { with(density) { rowHeight.toPx() } }
    val topPad = rowHeight * NumberWheelDimensions.RowsAbove
    val bottomPad = rowHeight * NumberWheelDimensions.RowsBelow
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
    val lineColor = MaterialTheme.colorScheme.surface
    val lineThickness = 2.dp
    val lineInset = 10.dp
    Box(
        modifier = modifier
            .height(NumberWheelDimensions.Height)
            .nestedScroll(nestedConnection)
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(top = topPad, bottom = bottomPad),
            modifier = Modifier.fillMaxSize()
        ) {
            items(count) { index ->
                val (scale, alpha) = transformsByIndex[index] ?: (0.72f to 0.25f)

                val label = buildString {
                    val desc = labels?.getOrNull(index)

                    if (index == count - 1 || index == 0) {
                        // last item and first item, no leading number
                        append("   ")
                        if (!desc.isNullOrBlank()) append(desc)
                    } else {
                        append(index)
                        if (!desc.isNullOrBlank()) {
                            append("  ")
                            append(desc)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowHeight)
                        .padding(horizontal = lineInset)
                        .clickable {
                            scope.launch {
                                val currentCenter = centerIndex
                                if (index == currentCenter) {
                                    // already centered -> replay
                                    onReplayRequested(index)
                                } else {
                                    // not centered -> scroll to make it center
                                    listState.animateScrollToItem(index)
                                }
                            }
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = label,
                        modifier = Modifier
                            .fillMaxWidth()
                            .layout { measurable, constraints ->
                                // Give the label two characters of drawing space without
                                // increasing the wheel's share of the card layout.
                                val textWidth = constraints.maxWidth + extraLabelWidth
                                val placeable = measurable.measure(
                                    constraints.copy(minWidth = textWidth, maxWidth = textWidth)
                                )
                                layout(constraints.maxWidth, placeable.height) {
                                    placeable.placeRelative(0, 0)
                                }
                            }
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                this.alpha = alpha
                            },
                        color = MaterialTheme.colorScheme.surface,
                        textAlign = TextAlign.Start,              // LEFT aligned text
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

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
