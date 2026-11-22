package fi.tkd.itfun.techniques

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PatternDropdownTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun singleItemMenuHasNoExtraHeightAndItsTopEdgeSelectsTheItem() {
        val selection = showMenu(listOf("Chon-Ji"))
        compose.onNode(isPopup()).assertHeightIsEqualTo(48.dp)
        compose.onNode(isPopup()).performTouchInput {
            click(Offset(center.x, 1f))
        }
        compose.runOnIdle { assertEquals("Chon-Ji", selection.value) }
    }

    @Test
    fun bottomEdgeSelectsTheLastItem() {
        val selection = showMenu(listOf("Chon-Ji", "Dan-Gun"))
        compose.onNode(isPopup()).assertHeightIsEqualTo(97.dp)
        compose.onNode(isPopup()).performTouchInput {
            click(Offset(center.x, height - 2f))
        }
        compose.runOnIdle { assertEquals("Dan-Gun", selection.value) }
    }

    private fun showMenu(names: List<String>): androidx.compose.runtime.MutableState<String> {
        val section = mutableStateOf("All")
        val selection = mutableStateOf("")
        val filters = mutableStateOf(emptySet<String>())
        val items = names.mapIndexed { index, name ->
            TKDItem(
                techniqueCode = index, name = name, koreanName = name,
                category = TKDItemCategory.ALL_PATTERNS, pattern = listOf(name),
                beltLevel = 9, videoKey = "pattern$index", localVideoPath = null
            )
        }
        compose.setContent {
            Techniques(items, section, selection, filters, "Pattern", 9, {})
        }
        compose.onNodeWithContentDescription("Patterns").performClick()
        compose.waitForIdle()
        return selection
    }
}
