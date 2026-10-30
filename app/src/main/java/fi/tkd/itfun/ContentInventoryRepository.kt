package fi.tkd.itfun

import android.content.Context
import androidx.room.withTransaction
import fi.tkd.itfun.data.db.AppDatabase
import fi.tkd.itfun.data.db.DbProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class TechniqueInventory(
    val name: String,
    val cards: Int,
    val videos: Int,
    val additions: Int = 0
)

data class BodyAreaInventory(val name: String, val areas: Int, val additions: Int = 0)

data class CompendiumInventory(
    val name: String,
    val entries: Int,
    val images: Int,
    val expectedImages: Int,
    val additions: Int = 0,
    val additionalImages: Int = 0
)

data class ContentInventory(
    val techniques: List<TechniqueInventory>,
    val bodyAreas: List<BodyAreaInventory>,
    val compendium: List<CompendiumInventory>,
    val hasNewContent: Boolean = false
)

class ContentInventoryRepository(private val db: AppDatabase) {
    constructor(context: Context) : this(DbProvider.get(context.applicationContext))

    suspend fun getInventory(remoteManifest: JSONObject? = null): ContentInventory = withContext(Dispatchers.IO) {
        // Read a consistent snapshot if an update is importing content at the same time.
        db.withTransaction {
            val installedVersion = db.contentMetaDao().getValue(DbProvider.JSON_MANIFEST_VERSION_KEY) ?: 0
            val newerManifest = remoteManifest?.takeIf { it.getInt("version") > installedVersion }
            val items = db.itemDao().getAllNow().groupBy { it.categoryId }
            val bodyParts = db.bodyPartDao().getAllNow().groupBy { it.region }
            val entries = db.compendiumEntryDao().getAllNow().groupBy { it.sectionKey }
            val images = db.compendiumEntryImageDao().getAllNow().groupBy { it.entryKey }
            val categories = db.categoryDao().getAllNow().associate { it.name to it.id }
            val sections = db.compendiumSectionDao().getAllNow().associateBy { it.sectionKey }
            val remoteItems = newerManifest?.getJSONArray("techniques").objects()
                .filter { it.getString("category").trim() in TKDItemCategory.entries.map { category -> category.name } }
                .groupBy { it.getString("category").trim() }
            val remoteBodyParts = newerManifest?.getJSONArray("bodyParts").objects()
                .groupBy { it.getString("region").trim() }
            val remoteSections = newerManifest?.let { root ->
                root.keys().asSequence().mapNotNull { key ->
                    root.optJSONObject(key)?.takeIf { it.optString("section") == "compendium" }
                        ?.let { key to it }
                }.toMap()
            }.orEmpty()

            ContentInventory(
                techniques = (categories.keys + remoteItems.keys).mapNotNull { category ->
                    if (category == TKDItemCategory.STANCES.name) return@mapNotNull null
                    val cards = items[categories[category]].orEmpty()
                    val installedCodes = cards.map { it.techniqueCode }.toSet()
                    val additions = remoteItems[category].orEmpty()
                        .count { it.getInt("techniqueCode") !in installedCodes }
                    if (cards.isEmpty() && additions == 0) return@mapNotNull null
                    TechniqueInventory(
                        name = when (category) {
                            "DEFENSIVE" -> "Blocks"
                            "OFFENSIVE" -> "Attacks"
                            "ALL_PATTERNS" -> "Patterns"
                            else -> category.displayName()
                        },
                        cards = cards.size,
                        videos = cards.count { isDownloaded(it.localVideoPath) },
                        additions = additions
                    )
                },
                bodyAreas = (bodyParts.keys + remoteBodyParts.keys).map { region ->
                    val parts = bodyParts[region].orEmpty()
                    val installedCodes = parts.map { it.code }.toSet()
                    BodyAreaInventory(
                        region.displayName(), parts.size,
                        additions = remoteBodyParts[region].orEmpty()
                            .count { it.getString("code").trim() !in installedCodes }
                    )
                },
                compendium = (sections.keys + remoteSections.keys).map { key ->
                    val sectionEntries = entries[key].orEmpty()
                    val sectionImages = sectionEntries.flatMap { images[it.entryKey].orEmpty() }
                    val installedEntryKeys = sectionEntries.map { it.entryKey }.toSet()
                    val installedImageKeys = sectionImages.map { it.entryKey to it.remoteKey }.toSet()
                    val remoteSection = remoteSections[key]
                    val remoteEntries = remoteSection?.optJSONObject("entries")
                    val remoteEntryKeys = remoteEntries?.keys()?.asSequence()?.toList().orEmpty()
                    val newImageKeys = remoteEntryKeys.flatMap { entryKey ->
                        val imageArray = remoteEntries?.getJSONObject(entryKey)
                            ?.optJSONObject("resources")?.optJSONArray("images")
                        (0 until (imageArray?.length() ?: 0)).mapNotNull { index ->
                            imageArray?.optString(index)?.takeIf { it.isNotBlank() }?.let { entryKey to it }
                        }
                    }
                    CompendiumInventory(
                        name = sections[key]?.titleEn
                            ?: remoteSection?.optJSONObject("title")?.optString("en").orEmpty(),
                        entries = sectionEntries.size,
                        images = sectionImages.count { isDownloaded(it.localPath) },
                        expectedImages = sectionImages.size,
                        additions = remoteEntryKeys.count { it !in installedEntryKeys },
                        additionalImages = newImageKeys.count { it !in installedImageKeys }
                    )
                },
                hasNewContent = newerManifest != null
            )
        }
    }

    private fun isDownloaded(path: String?): Boolean =
        !path.isNullOrBlank() && File(path).let { it.isFile && it.length() > 0 }

    private fun String.displayName(): String =
        lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

    private fun JSONArray?.objects(): List<JSONObject> =
        (0 until (this?.length() ?: 0)).map { requireNotNull(this).getJSONObject(it) }
}
