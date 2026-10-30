package fi.tkd.itfun

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HoveringTechniqueCardTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun specialIgnoresTechniqueFiltersAndBeltAndSupportsPinning() {
        val tab = mutableStateOf("All")
        val pattern = mutableStateOf("")
        val filters = mutableStateOf(setOf("DEFENSIVE"))
        val belt = mutableStateOf(9)
        val items = listOf(
            TKDItem(1, "Ordinary kick", "Kick", TKDItemCategory.KICKS,
                emptyList(), 9, "kick", null),
            TKDItem(500, "Flying High Kick", "Twimyo Nopi Chagi", TKDItemCategory.SPECIAL,
                emptyList(), 0, "flyinghighkick", null)
        )
        compose.setContent {
            Techniques(items, tab, pattern, filters, "Pattern", belt.value, {})
        }
        compose.onNodeWithText("Search").performTextInput("does not match")
        compose.onNodeWithContentDescription("Special").performClick()
        compose.onNodeWithText("Flying High Kick").assertIsDisplayed()
        compose.onNodeWithText("Ordinary kick").assertDoesNotExist()
        compose.onNodeWithText("Search").assertDoesNotExist()
        compose.onNodeWithContentDescription("KICKS").assertDoesNotExist()
        compose.runOnIdle { belt.value = 1 }
        compose.onNodeWithText("Flying High Kick").assertIsDisplayed()
        compose.onNodeWithContentDescription("Hover technique").performClick()
        compose.onNodeWithContentDescription("Return to list").assertIsDisplayed()
        compose.onNodeWithContentDescription("Crushing").performClick()
        compose.onNodeWithContentDescription("Return to list").assertDoesNotExist()
        compose.runOnIdle { filters.value = emptySet() }
        compose.onNodeWithContentDescription("All techniques").performClick()
        compose.onNodeWithText("Flying High Kick").assertDoesNotExist()
    }

    @Test
    fun crushingIgnoresTechniqueFiltersAndBeltAndSupportsPinning() {
        val tab = mutableStateOf("All")
        val pattern = mutableStateOf("")
        val filters = mutableStateOf(setOf("DEFENSIVE"))
        val belt = mutableStateOf(9)
        val items = listOf(
            TKDItem(1, "Ordinary kick", "Kick", TKDItemCategory.KICKS,
                emptyList(), 9, "kick", null),
            TKDItem(550, "Forefist Punch", "Ap Joomuk Jirugi", TKDItemCategory.CRUSHING,
                emptyList(), 0, "crushingforefistpunch", null)
        )
        compose.setContent {
            Techniques(items, tab, pattern, filters, "Pattern", belt.value, {})
        }
        compose.onNodeWithText("Search").performTextInput("does not match")
        compose.onNodeWithContentDescription("Crushing").performClick()
        compose.onNodeWithText("Forefist Punch").assertIsDisplayed()
        compose.onNodeWithText("Ordinary kick").assertDoesNotExist()
        compose.onNodeWithText("Search").assertDoesNotExist()
        compose.onNodeWithContentDescription("KICKS").assertDoesNotExist()
        compose.runOnIdle { belt.value = 1 }
        compose.onNodeWithText("Forefist Punch").assertIsDisplayed()
        compose.onNodeWithContentDescription("Hover technique").performClick()
        compose.onNodeWithContentDescription("Return to list").assertIsDisplayed()
        compose.onNodeWithContentDescription("Special").performClick()
        compose.onNodeWithContentDescription("Return to list").assertDoesNotExist()
        compose.runOnIdle { filters.value = emptySet() }
        compose.onNodeWithContentDescription("All techniques").performClick()
        compose.onNodeWithText("Forefist Punch").assertDoesNotExist()
    }

    @Test
    fun newSectionsAreEmptyHideTechniqueControlsAndClearHoveringCard() {
        val tab = mutableStateOf("All")
        val pattern = mutableStateOf("")
        val filters = mutableStateOf(emptySet<String>())
        val items = listOf(
            TKDItem(1, "Test technique", "Test Korean", TKDItemCategory.KICKS,
                emptyList(), 9, "video", null)
        )
        compose.setContent {
            Techniques(items, tab, pattern, filters, "Pattern", 9, {})
        }
        compose.onNodeWithContentDescription("Hover technique").performClick()
        compose.onNodeWithContentDescription("Return to list").assertIsDisplayed()
        for (section in listOf("Sparring", "Crushing", "Special")) {
            compose.onNodeWithContentDescription(section).performClick()
            compose.onNodeWithContentDescription("Return to list").assertDoesNotExist()
            compose.onNodeWithText("Test technique").assertDoesNotExist()
            compose.onNodeWithText("Search").assertDoesNotExist()
            compose.onNodeWithContentDescription("KICKS").assertDoesNotExist()
        }
        compose.onNodeWithContentDescription("All techniques").performClick()
        compose.onNodeWithText("Test technique").assertIsDisplayed()
        compose.onNodeWithText("Search").assertIsDisplayed()
        compose.onNodeWithContentDescription("KICKS").assertIsDisplayed()
        compose.onNodeWithContentDescription("Return to list").assertDoesNotExist()
    }

    @Test
    fun pinnedCardStaysInPlaceWhileListScrollsAndCanBeReturned() {
        val items = List(25) {
            TKDItem(it, "Technique $it", "Korean $it", TKDItemCategory.KICKS,
                emptyList(), 9, "video_$it", null)
        }
        compose.setContent {
            Techniques(
                tkdItemsRoom = items,
                selectedTab = mutableStateOf("All"),
                selectedPattern = mutableStateOf(""),
                selectedFilters = mutableStateOf(emptySet()),
                patternStr = "Pattern",
                beltGup = 9,
                onOpenCompendiumEntry = {}
            )
        }
        compose.onAllNodesWithContentDescription("Hover technique")[0].performClick()
        compose.waitForIdle()
        val originalBounds = compose.onNodeWithContentDescription("Return to list").fetchSemanticsNode().boundsInRoot
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
            .performScrollToNode(hasText("Technique 24"))
        compose.onNodeWithContentDescription("Return to list").assertIsDisplayed()
        val scrolledBounds = compose.onNodeWithContentDescription("Return to list").fetchSemanticsNode().boundsInRoot
        assertEquals(originalBounds, scrolledBounds)
        compose.onNodeWithContentDescription("Return to list").performClick()
        compose.onNodeWithContentDescription("Return to list").assertDoesNotExist()
    }
}
