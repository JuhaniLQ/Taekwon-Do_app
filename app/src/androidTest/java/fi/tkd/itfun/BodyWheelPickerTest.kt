package fi.tkd.itfun

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BodyWheelPickerTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun partialFlingsSettleAtAnItemAcrossWheelHeights() {
        lateinit var state: LazyListState
        val wheelHeight = mutableStateOf(163.dp)
        val items = List(3) { BodyItem("ITEM_$it", BodyRegion.ARM, false, "Item $it", "Item $it") }
        compose.setContent {
            state = rememberLazyListState()
            Box(Modifier.width(300.dp).height(wheelHeight.value)) {
                BodyWheelPicker(items, items.first().code, onSelectedCodeChange = {}, listState = state)
            }
        }
        for (height in listOf(151, 163, 223, 333, 450)) {
            compose.runOnIdle { wheelHeight.value = height.dp }
            compose.waitForIdle()
            for (distance in listOf(0.2f, 0.6f, 1.1f)) {
                runBlocking { state.scrollToItem(1) }
                compose.waitForIdle()
                compose.onNode(hasScrollAction()).performTouchInput {
                    swipeUp(startY = this.height * 0.8f, endY = this.height * 0.8f - 50.dp.toPx() * distance, durationMillis = 80)
                }
                compose.waitForIdle()
                compose.runOnIdle {
                    val closest = state.layoutInfo.visibleItemsInfo.minBy { kotlin.math.abs(it.offset) }
                    assertEquals("height=$height distance=$distance index=${closest.index}", 0f, closest.offset.toFloat(), 1f)
                }
            }
        }
    }

    @Test
    fun flingToLastItemAlignsOnShortWheel() {
        lateinit var state: LazyListState
        val items = List(3) { BodyItem("ITEM_$it", BodyRegion.ARM, false, "Item $it", "Item $it") }
        compose.setContent {
            state = rememberLazyListState()
            Box(Modifier.width(300.dp).height(163.dp)) {
                BodyWheelPicker(
                    items = items,
                    selectedCode = items.first().code,
                    listState = state,
                    onSelectedCodeChange = {}
                )
            }
        }
        compose.waitForIdle()
        compose.onNode(hasScrollAction()).performTouchInput {
            swipeUp(startY = height * 0.8f, endY = height * 0.5f, durationMillis = 60)
        }
        compose.waitForIdle()
        compose.runOnIdle {
            val last = state.layoutInfo.visibleItemsInfo.first { it.index == items.lastIndex }
            assertEquals("Fling must settle the last row at the selection position", 0f, last.offset.toFloat(), 1f)
        }
    }

    @Test
    fun lastItemAlignsWithSelectionOnShortWheel() {
        lateinit var state: LazyListState
        val items = List(20) { BodyItem("ITEM_$it", BodyRegion.ARM, false, "Item $it", "Item $it") }
        compose.setContent {
            state = rememberLazyListState()
            Box(Modifier.width(300.dp).height(163.dp)) {
                BodyWheelPicker(
                    items = items,
                    selectedCode = items.last().code,
                    listState = state,
                    onSelectedCodeChange = {}
                )
            }
        }
        compose.waitForIdle()
        compose.runOnIdle {
            val last = state.layoutInfo.visibleItemsInfo.first { it.index == items.lastIndex }
            assertEquals("Last row must start at the selection position", 0f, last.offset.toFloat(), 1f)
        }
    }
}
