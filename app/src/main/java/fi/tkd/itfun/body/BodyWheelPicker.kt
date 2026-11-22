package fi.tkd.itfun.body

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun BodyWheelPicker(
    items: List<BodyItem>,
    selectedCode: String?,
    modifier: Modifier = Modifier,
    onSelectedCodeChange: (String?) -> Unit,
    onReplayRequested: (String) -> Unit = {},
    listState: LazyListState = rememberLazyListState()
) {
    val scope = rememberCoroutineScope()

    val rowHeight = 50.dp
    val selectionHeight = 50.dp
    val above = 2
    val below = 6
    val visibleRows = above + 1 + below

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
                        color = MaterialTheme.colorScheme.onSurface,
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
