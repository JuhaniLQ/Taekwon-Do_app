package fi.tkd.itfun.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fi.tkd.itfun.ContentInventoryUiState

@Composable
fun ContentInventoryContent(state: ContentInventoryUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height((LocalConfiguration.current.screenHeightDp * 0.6f).dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (state.error != null) {
            Text(state.error)
        }
        if (state.updateCheckFailed) {
            Text("Couldn't check for new content.",
                style = MaterialTheme.typography.bodySmall)
        }
        when {
            state.inventory == null && state.error == null -> Text("Checking installed content...")
            state.inventory != null -> {
                val inventory = state.inventory
                if (inventory.hasNewContent) {
                    Text("Update for new content! ↑", fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 40.dp, top = 20.dp))
                }
                Text("Installed content", fontWeight = FontWeight.Bold)
                Text(
                    "All content, regardless of belt or filters.",
                    style = MaterialTheme.typography.bodySmall
                )

                InventoryHeading("Techniques")
                InventoryRow("Category", "Cards", "Videos", heading = true)
                inventory.techniques.forEach { row ->
                    InventoryRow(
                        row.name, row.cards.toString(), row.videos.toString(),
                        incomplete = row.videos < row.cards,
                        additions = row.additions
                    )
                }

                InventoryHeading("Body parts")
                InventoryRow("Region", "Areas", heading = true)
                inventory.bodyAreas.forEach { row ->
                    InventoryRow(row.name, row.areas.toString(), additions = row.additions)
                }

                InventoryHeading("Compendium")
                InventoryRow("Section", "Entries", "Images", heading = true)
                inventory.compendium.forEach { row ->
                    InventoryRow(
                        row.name,
                        row.entries.toString(),
                        if (row.expectedImages == 0) "—" else "${row.images}/${row.expectedImages}",
                        incomplete = row.images < row.expectedImages,
                        additions = row.additions,
                        additionalFiles = row.additionalImages
                    )
                }
                Text(
                    "Videos count cards with a downloaded file. Images show downloaded/expected. " +
                        "Missing files are marked in bold with an exclamation mark. " +
                        "Compendium entries are descriptions, not additional videos.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun InventoryHeading(text: String) {
    Text(
        text,
        modifier = Modifier.padding(top = 12.dp),
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun InventoryRow(
    name: String,
    count: String,
    files: String? = null,
    heading: Boolean = false,
    incomplete: Boolean = false,
    additions: Int = 0,
    additionalFiles: Int = 0
) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            name,
            modifier = Modifier.weight(2f),
            fontWeight = if (heading) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            inventoryCount(count, additions),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            fontWeight = if (heading) FontWeight.Bold else FontWeight.Normal
        )
        if (files != null) {
            Text(
                inventoryCount(if (incomplete) "$files !" else files, additionalFiles),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End,
                fontWeight = if (heading || incomplete) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

private fun inventoryCount(value: String, additions: Int) = buildAnnotatedString {
    append(value)
    if (additions > 0) {
        withStyle(SpanStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold)) {
            append(" (+$additions)")
        }
    }
}
