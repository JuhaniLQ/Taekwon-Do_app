package fi.tkd.itfun.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import kotlin.math.roundToInt
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fi.tkd.itfun.ui.theme.Green
import fi.tkd.itfun.ui.theme.Navy1
import fi.tkd.itfun.ui.theme.Red
import fi.tkd.itfun.ui.theme.Yellow
import fi.tkd.itfun.ContentInventoryUiState
import fi.tkd.itfun.ContentUpdateUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    isOpen: Boolean,
    beltGup: Int,
    updateState: ContentUpdateUiState,
    inventoryState: ContentInventoryUiState,
    onRefreshInventory: () -> Unit,
    onCheckAvailableContent: () -> Unit,
    onDismiss: () -> Unit,
    onSelectBeltLevel: (Int) -> Unit,
    onUpdateClick: () -> Unit,
    blackBeltsEnabled: Boolean = false
) {
    var sheetPage by rememberSaveable { mutableIntStateOf(0) }
    val expansionLocked by rememberUpdatedState(
        sheetPage == 1 || (sheetPage == 0 && !blackBeltsEnabled)
    )
    val confirmSheetChange: (SheetValue) -> Boolean = remember {
        { value -> !expansionLocked || value != SheetValue.Expanded }
    }
    val sheetState = rememberModalBottomSheetState(confirmValueChange = confirmSheetChange)

    LaunchedEffect(isOpen) {
        if (isOpen && !updateState.isUpdating) {
            onCheckAvailableContent()
        }
    }

    LaunchedEffect(isOpen, sheetPage, updateState.isUpdating) {
        if (isOpen && sheetPage == 2 && !updateState.isUpdating) {
            onRefreshInventory()
        }
    }

    if (isOpen) {
        ModalBottomSheet(
            sheetState = sheetState,
            modifier = Modifier.layout { measurable, constraints ->
                val sheet = measurable.measure(constraints)
                // Keep the original size and resting position; only limit upward placement.
                val restingTop = maxOf(constraints.maxHeight / 2f, (constraints.maxHeight - sheet.height).toFloat())
                layout(sheet.width, sheet.height) {
                    val correction = if (expansionLocked) {
                        (restingTop - sheetState.requireOffset()).coerceAtLeast(0f).roundToInt()
                    } else 0
                    sheet.placeRelative(0, correction)
                }
            },
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()

            ) {

                // --- Page selector row ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Middle tab: always centered
                    SheetTab(
                        text = "Color picker",
                        selected = sheetPage == 1,
                        onClick = { sheetPage = 1 }
                    )

                    // Left tab: pinned to start
                    Row(
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        SheetTab(
                            text = "Belt level",
                            selected = sheetPage == 0,
                            onClick = { sheetPage = 0 }
                        )
                    }

                    // Right tab: pinned to end
                    Row(
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        SheetTab(
                            text = "Content",
                            selected = sheetPage == 2,
                            onClick = { sheetPage = 2 }
                        )
                    }
                }

                // Positive levels are gup; negative levels are dan. Zero is unused.
                val selectedUiLevel = if (beltGup > 0) 10 - beltGup else 9 - beltGup

                // --- Page content ---
                when (sheetPage) {
                    0 -> BeltLevelSettings(
                        selectedLevel = selectedUiLevel,
                        blackBeltsEnabled = blackBeltsEnabled,
                        onSelectLevel = onSelectBeltLevel
                    )
                    1 -> ColorPicker()
                    2 -> ContentSettings(
                        inventoryState = inventoryState,
                        statusText = updateState.statusText,
                        progress = updateState.progress,
                        showBar = updateState.showBar,
                        isUpdating = updateState.isUpdating,
                        onUpdateClick = onUpdateClick
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SheetTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onSurface,
            containerColor = Color.Transparent,
            labelColor = MaterialTheme.colorScheme.onSurface,

        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = MaterialTheme.colorScheme.primary,
            selectedBorderColor = MaterialTheme.colorScheme.primary,
            borderWidth = 1.dp,
            selectedBorderWidth = 1.dp
        )

    )
}


@Composable
fun BeltLevelSettings(
    selectedLevel: Int,
    onSelectLevel: (Int) -> Unit,
    blackBeltsEnabled: Boolean = false
) {
    BeltSelectionGrid(
        isLevelSelected = { it <= selectedLevel },
        onSelectLevel = onSelectLevel,
        blackBeltsEnabled = blackBeltsEnabled,
        modifier = Modifier.verticalScroll(rememberScrollState())
    )
}

@Composable
internal fun BeltSelectionGrid(
    isLevelSelected: (Int) -> Boolean,
    onSelectLevel: (Int) -> Unit,
    blackBeltsEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top=37.dp, bottom = 37.dp),
        verticalArrangement = Arrangement.spacedBy(40.dp),
    ) {
        val boxSize = 64.dp
        val corner = RoundedCornerShape(12.dp)

        val beltColors = listOf(
            Color.White, // 1
            Yellow,      // 2
            Yellow,      // 3
            Green,       // 4
            Green,       // 5
            Navy1,       // 6
            Navy1,       // 7
            Red,         // 8
            Red          // 9
        ) + if (blackBeltsEnabled) List(9) { Color.Black } else emptyList()

        val stripeColors = mapOf(
            2 to Green,        // 3rd box gets a green stripe
            4 to Navy1,        // 5th box gets a blue stripe
            6 to Red,          // 7th box gets a red stripe
            8 to Color.Black   // 9th box gets a black stripe
        )

        var index = 0
        repeat(beltColors.size / 3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(3) {
                    val level = index + 1
                    val isSelected = isLevelSelected(level)
                    val contentAlpha = if (isSelected) 0f else 45f

                    val backgroundColor = beltColors[index].darker(contentAlpha)
                    val borderColor = if (isSelected) MaterialTheme.colorScheme.onSurface else backgroundColor

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .size(boxSize)
                            .clip(corner)
                            .background(backgroundColor)
                            .border(2.dp, borderColor, corner)
                            .clickable { onSelectLevel(level) }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(corner)
                                .background(backgroundColor, corner)
                        ) {
                            stripeColors[index]?.let { stripe ->
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .fillMaxWidth()
                                        .fillMaxHeight(0.2f)
                                        .background(stripe)//.opacity(contentAlpha)
                                )
                            }
                            if (level > 9) {
                                Text(
                                    text = listOf("I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX")[level - 10],
                                    color = Color.White.opacity(contentAlpha),
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    }
                    index++
                }
            }
        }

    }
}

@Composable
fun ContentSettings(
    inventoryState: ContentInventoryUiState,
    statusText: String,
    progress: Float,
    showBar: Boolean,
    isUpdating: Boolean,
    onUpdateClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp, vertical = 5.dp)
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Button(
                onClick = onUpdateClick,
                enabled = !isUpdating) {
                Text("Update content",
                    color = MaterialTheme.colorScheme.surface)
            }
        }

        Spacer(Modifier.height(8.dp))

        if (showBar) {
            UpdateProgressBar(
                statusText = statusText,
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp, vertical = 5.dp)
            )
        }

        ContentInventoryContent(inventoryState)
    }
}


@Composable
fun UpdateProgressBar(
    statusText: String,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val clamped = progress.coerceIn(0f, 1f)
    val trackColor = MaterialTheme.colorScheme.primary
    val fillColor = MaterialTheme.colorScheme.primary.darker()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(trackColor)
    ) {
        // Filled part
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(clamped)
                .clip(RoundedCornerShape(20.dp))
                .background(fillColor)
        )

        // Centered text
        Text(
            text = statusText,
            modifier = Modifier.align(Alignment.Center),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}
