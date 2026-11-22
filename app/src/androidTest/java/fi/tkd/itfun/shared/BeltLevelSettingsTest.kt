package fi.tkd.itfun.shared

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BeltLevelSettingsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun disabledFlagHidesBlackBelts() {
        compose.setContent { BeltLevelSettings(selectedLevel = 1, onSelectLevel = {}) }
        compose.onNodeWithText("I").assertDoesNotExist()
        compose.onNodeWithText("IX").assertDoesNotExist()
    }

    @Test
    fun enabledFlagAllowsScrollingToAndSelectingNinthDan() {
        var selected = 1
        compose.setContent {
            BeltLevelSettings(selectedLevel = selected, onSelectLevel = { selected = it }, blackBeltsEnabled = true)
        }
        compose.onNodeWithText("IX").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(18, selected) }
    }
}
