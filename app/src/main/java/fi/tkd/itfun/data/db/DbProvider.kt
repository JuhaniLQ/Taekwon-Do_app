package fi.tkd.itfun.data.db

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.withTransaction
import fi.tkd.itfun.TKDItemCategory
import fi.tkd.itfun.TKDPatterns
import fi.tkd.itfun.data.db.DbProvider.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URL

object DbProvider {

    @Volatile private var instance: AppDatabase? = null

    fun get(context: Context): AppDatabase =
        instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "tkd.db"
            )
                .fallbackToDestructiveMigration()
                .build()
                .also { instance = it }
        }
    private const val TECHNIQUES_DATA_VERSION = 1
    private const val TECHNIQUES_VERSION_KEY = "techniques_version"
    private const val JSON_MANIFEST_VERSION_KEY = "json_manifest_version"


    suspend fun ensureSeeded(context: Context) {
        val db = get(context)
        val metaDao = db.contentMetaDao()
        val current = metaDao.getValue(TECHNIQUES_VERSION_KEY) ?: 0
        if (current >= TECHNIQUES_DATA_VERSION) return

        db.withTransaction {
            val patternDao = db.patternDao()
            val categoryDao = db.categoryDao()
            val itemDao = db.itemDao()

            val compendiumSectionDao = db.compendiumSectionDao()
            val compendiumEntryDao = db.compendiumEntryDao()
            val compendiumEntryImageDao = db.compendiumEntryImageDao()

            // Grab existing items BEFORE wipe
            val oldItemsByCode = itemDao.getAllNow()
                .associateBy { it.techniqueCode }

            categoryDao.deleteAll()
            patternDao.deleteAll()
            itemDao.deleteAllLinks()
            itemDao.deleteAll()

            compendiumEntryImageDao.deleteAll()
            compendiumEntryDao.deleteAll()
            compendiumSectionDao.deleteAll()

            categoryDao.insertAll(TKDItemCategory.entries.map { CategoryEntity(name = it.name) })
            patternDao.insertAll(TKDPatterns.all.map { PatternEntity(name = it) })
            val root = loadManifestJson(context)
            seedItemsFromJson(context, db, oldItemsByCode, root)
            metaDao.put(ContentMetaEntity(TECHNIQUES_VERSION_KEY, TECHNIQUES_DATA_VERSION))
        }
    }

    sealed class UpdateResult {
        object NetworkError : UpdateResult()
        object NoUpdate : UpdateResult()
        object Updated : UpdateResult()
    }

    suspend fun updateSeeding(context: Context): UpdateResult = withContext(Dispatchers.IO) {
        val db = get(context)
        val metaDao = db.contentMetaDao()
        val root = loadManifestJsonNoFallback() ?: return@withContext UpdateResult.NetworkError
        val remoteVersion = root.optInt("version", 1)
        val current = metaDao.getValue(JSON_MANIFEST_VERSION_KEY) ?: 0

        if (current >= remoteVersion) {
            return@withContext UpdateResult.NoUpdate
        }

        db.withTransaction {
            val patternDao = db.patternDao()
            val categoryDao = db.categoryDao()
            val itemDao = db.itemDao()

            val compendiumSectionDao = db.compendiumSectionDao()
            val compendiumEntryDao = db.compendiumEntryDao()
            val compendiumEntryImageDao = db.compendiumEntryImageDao()


            // Grab existing items BEFORE wipe
            val oldItemsByCode = itemDao.getAllNow()
                .associateBy { it.techniqueCode }

            categoryDao.deleteAll()
            patternDao.deleteAll()
            itemDao.deleteAllLinks()
            itemDao.deleteAll()

            compendiumEntryImageDao.deleteAll()
            compendiumEntryDao.deleteAll()
            compendiumSectionDao.deleteAll()

            categoryDao.insertAll(TKDItemCategory.entries.map { CategoryEntity(name = it.name) })
            patternDao.insertAll(TKDPatterns.all.map { PatternEntity(name = it) })

            seedItemsFromJson(context, db, oldItemsByCode, root)
        }
        return@withContext UpdateResult.Updated
    }

    private suspend fun seedItemsFromJson(
        context: Context,
        db: AppDatabase,
        oldItemsByCode: Map<Int, ItemEntity>,
        root: JSONObject
    ) {
        val bodyPartsArray = root.getJSONArray("bodyParts")
        val patternDetailsObject = root.getJSONObject("patternvideodetails")
        val techniquesArray = root.getJSONArray("techniques")

        seedBodyPartsFromArray(db, bodyPartsArray)
        seedTechniquesFromArray(context, db, techniquesArray, oldItemsByCode, termKeyToNameMap(root))
        seedPatternVideoDetails(db, patternDetailsObject)
        seedCompendiumFromJson(db, root)

        val manifestVersion = root.optInt("version", 1)
        db.contentMetaDao().put(
            ContentMetaEntity(JSON_MANIFEST_VERSION_KEY, manifestVersion)
        )
    }
}

private fun termKeyToNameMap(root: JSONObject): Map<String, String> {
    val obj = root.getJSONObject("terms")
    val result = mutableMapOf<String, String>()
    val keys = obj.keys()
    while (keys.hasNext()) {
        val key = keys.next()
        result[key] = obj.getString(key)
    }
    return result
}


private fun composeKorean(sortedTermNames: List<String>) =
    sortedTermNames.joinToString(" ")


private suspend fun seedBodyPartsFromArray(db: AppDatabase, bodyPartsArray: org.json.JSONArray) {
    val bodyPartDao = db.bodyPartDao()
    bodyPartDao.deleteAll()

    val parts = ArrayList<BodyPartEntity>(bodyPartsArray.length())
    for (i in 0 until bodyPartsArray.length()) {
        val o = bodyPartsArray.getJSONObject(i)
        val names = o.getJSONObject("names")

        parts += BodyPartEntity(
            code = o.getString("code").trim(),
            region = o.getString("region").trim(),
            canBeVitalPoint = o.optBoolean("canBeVitalPoint", false),
            nameEn = names.getString("en").trim(),
            nameKo = names.getString("ko").trim()
        )
    }

    if (parts.isNotEmpty()) bodyPartDao.insertAll(parts)
}

private suspend fun seedCompendiumFromJson(
    db: AppDatabase,
    root: JSONObject
) {
    val sectionDao = db.compendiumSectionDao()
    val entryDao = db.compendiumEntryDao()
    val imageDao = db.compendiumEntryImageDao()

    val sections = mutableListOf<CompendiumSectionEntity>()
    val entries = mutableListOf<CompendiumEntryEntity>()
    val images = mutableListOf<CompendiumEntryImageEntity>()

    var sectionOrder = 0

    val rootKeys = root.keys()
    while (rootKeys.hasNext()) {
        val sectionKey = rootKeys.next()
        val sectionObj = root.optJSONObject(sectionKey) ?: continue

        if (sectionObj.optString("section") != "compendium") continue

        val titleObj = sectionObj.optJSONObject("title")
        val titleEn = titleObj?.optString("en").orEmpty()
        val titleFi = titleObj?.optString("fi").orEmpty()

        sections += CompendiumSectionEntity(
            sectionKey = sectionKey,
            groupKey = "compendium",
            titleEn = titleEn,
            titleFi = titleFi,
            sortOrder = sectionOrder++
        )

        val entriesObj = sectionObj.optJSONObject("entries") ?: continue
        val entryKeys = entriesObj.keys()

        var entryOrder = 0
        while (entryKeys.hasNext()) {
            val entryKey = entryKeys.next()
            val entryObj = entriesObj.optJSONObject(entryKey) ?: continue

            val entryTitleObj = entryObj.optJSONObject("title")
            val descriptionObj = entryObj.optJSONObject("description")

            entries += CompendiumEntryEntity(
                entryKey = entryKey,
                sectionKey = sectionKey,
                titleEn = entryTitleObj?.optString("en"),
                titleFi = entryTitleObj?.optString("fi"),
                descriptionEn = descriptionObj?.optString("en").orEmpty(),
                descriptionFi = descriptionObj?.optString("fi").orEmpty(),
                sortOrder = entryOrder++
            )

            val imagesArray = entryObj
                .optJSONObject("resources")
                ?.optJSONArray("images")

            if (imagesArray != null) {
                for (i in 0 until imagesArray.length()) {
                    val remoteKey = imagesArray.optString(i).takeIf { it.isNotBlank() } ?: continue

                    images += CompendiumEntryImageEntity(
                        entryKey = entryKey,
                        remoteKey = remoteKey,
                        localPath = null,
                        localVersion = 0,
                        remoteVersion = root.optInt("version", 1),
                        sortOrder = i
                    )
                }
            }
        }
    }

    sectionDao.insertAll(sections)
    entryDao.insertAll(entries)
    imageDao.insertAll(images)
}

private suspend fun seedTechniquesFromArray(
    context: Context,
    db: AppDatabase,
    array: org.json.JSONArray,
    oldItemsByCode: Map<Int, ItemEntity>,
    termKeyToName : Map<String, String>
) {
    val catIdByName = db.categoryDao().getAllNow().associate { it.name to it.id }
    val patIdByName = db.patternDao().getAllNow().associate { it.name to it.id }

    val itemDao = db.itemDao()
    val linkBuffer = mutableListOf<ItemPatternCrossRef>()

    for (i in 0 until array.length()) {
        val o = array.getJSONObject(i)

        val techniqueCode = o.getInt("techniqueCode")
        val name = o.getString("name").trim()
        val drawableName = o.getString("drawable").trim()
        val categoryName = o.getString("category").trim()
        val patterns = o.getJSONArray("patterns")
        val koreanKeys = o.getJSONArray("koreanTerms")
        val beltLevel = o.getInt("beltLevel")

        val remoteVideoVersion = o.optInt("videoVersion", 1)

        val categoryId = catIdByName[categoryName] ?: continue

        val termNames = (0 until koreanKeys.length())
            .map { idx -> termKeyToName[koreanKeys.getString(idx)] ?: "" }
            .filter { it.isNotBlank() }

        val korean = composeKorean(termNames)

        val old = oldItemsByCode[techniqueCode]

        val expectedFile = File(
            context.getExternalFilesDir("videos"),
            "$drawableName.mp4"
        )

        val recoveredPath =
            if (expectedFile.exists()) expectedFile.absolutePath else null

        val (localPath, localVersion) =
            if (
                old != null &&
                !old.localVideoPath.isNullOrBlank() &&
                File(old.localVideoPath).exists() &&
                old.localVideoVersion == remoteVideoVersion
            ) {
                old.localVideoPath to old.localVideoVersion
            } else if (recoveredPath != null) {
                recoveredPath to remoteVideoVersion
            } else {
                null to null
            }
        val entity = ItemEntity(
            techniqueCode = techniqueCode,
            name = name,
            koreanName = korean,
            videoKey = drawableName,
            remoteVideoVersion = remoteVideoVersion,
            localVideoPath = localPath,
            localVideoVersion = localVersion,
            categoryId = categoryId,
            beltLevel = beltLevel
        )

        val itemId = itemDao.upsert(entity)


        for (p in 0 until patterns.length()) {
            val pName = patterns.getString(p).trim()
            val pId = patIdByName[pName] ?: continue
            linkBuffer += ItemPatternCrossRef(itemId = itemId, patternId = pId)
        }
    }

    if (linkBuffer.isNotEmpty()) itemDao.insertLinks(linkBuffer)
}

private suspend fun seedPatternVideoDetails(
    db: AppDatabase,
    obj: JSONObject
) {
    val dao = db.patternVideoDetailsDao()

    val list = mutableListOf<PatternVideoDetailsEntity>()

    val keys = obj.keys()
    while (keys.hasNext()) {
        val videoKey = keys.next()             // e.g. "chonji"
        val entry = obj.getJSONObject(videoKey)

        val timecodes = entry.getJSONArray("timecodesMs").toString()
        val short = entry.getJSONArray("short").toString()
        val full = entry.getJSONArray("full").toString()

        list += PatternVideoDetailsEntity(
            videoKey = videoKey,
            timecodesJson = timecodes,
            shortJson = short,
            fullJson = full
        )
    }

    dao.deleteAll()
    dao.insertAll(list)
}

private const val REMOTE_MANIFEST_URL = //please do not misuse this web address, so I can continue to offer the contents of this app
    "https://pub-ac21b0d2bd5842ec912448a452ab1b38.r2.dev/manifest/tkd_items.json"

private suspend fun loadManifestJson(context: Context): JSONObject =
    withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(REMOTE_MANIFEST_URL)
            val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
            }
            conn.inputStream.bufferedReader().use { reader ->
                val text = reader.readText()
                JSONObject(text)
            }
        }.getOrElse { e ->
            Log.w("DBDEBUG", "Remote manifest fetch failed, falling back to assets", e)
            val fallbackText =
                context.assets.open("tkd_items.json").bufferedReader().use { it.readText() }
            JSONObject(fallbackText)
        }
    }

suspend fun loadManifestJsonNoFallback(): JSONObject? =
    withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(REMOTE_MANIFEST_URL)
            val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
            }
            conn.inputStream.bufferedReader().use { reader ->
                JSONObject(reader.readText())
            }
        }.getOrElse { e ->
            Log.w("DBDEBUG", "Remote manifest fetch failed", e)
            null
        }
    }

private const val CDN_BASE = "https://pub-ac21b0d2bd5842ec912448a452ab1b38.r2.dev/videos"

suspend fun downloadVideo(context: Context, videoKey: String): String? =
    withContext(Dispatchers.IO) {
        val url = URL("$CDN_BASE/$videoKey.mp4")

        val videosDir = context.getExternalFilesDir("videos") ?: return@withContext null
        if (!videosDir.exists()) videosDir.mkdirs()

        val outFile = File(videosDir, "$videoKey.mp4")

        try {
            url.openStream().use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
            return@withContext outFile.absolutePath
        } catch (e: Exception) {
            Log.w("DBDEBUG", "Failed to download $videoKey", e)
            return@withContext null
        }
    }

suspend fun downloadAllVideos(context: Context, progress: (Float) -> Unit) =
    withContext(Dispatchers.IO) {// if the same video is already downloaded it's skipped
        val db = get(context)
        val itemDao = db.itemDao()
        val items = itemDao.getAllNow()
        val total = items.size.coerceAtLeast(1) // avoid division by zero
        var done = 0

        for (item in items) {
            val path = item.localVideoPath
            val fileMissing = path != null && !File(path).exists()

            val hasFreshLocal =
                path != null &&
                        !fileMissing &&
                        item.localVideoVersion == item.remoteVideoVersion

            if (!hasFreshLocal) {
                // if DB thinks there is a file but it's gone, clear the path
                if (fileMissing) {
                    itemDao.updateLocalVideoPathAndVersion(
                        techniqueCode = item.techniqueCode,
                        path = null,
                        localVersion = item.localVideoVersion
                    )
                }

                val newPath = downloadVideo(context, item.videoKey)
                if (newPath != null) {
                    itemDao.updateLocalVideoPathAndVersion(
                        techniqueCode = item.techniqueCode,
                        path = newPath,
                        localVersion = item.remoteVideoVersion
                    )
                }
            }
            done++
            withContext(Dispatchers.Main) {
                progress(done.toFloat() / total)
            }
        }
    }

private const val CDN_IMAGE_BASE = "https://pub-ac21b0d2bd5842ec912448a452ab1b38.r2.dev/images"

suspend fun downloadCompendiumImage(
    context: Context,
    remoteKey: String
): String? = withContext(Dispatchers.IO) {
    val url = URL("$CDN_IMAGE_BASE/$remoteKey.png")

    val imagesDir = context.getExternalFilesDir("images") ?: return@withContext null
    if (!imagesDir.exists()) imagesDir.mkdirs()

    val outFile = File(imagesDir, "$remoteKey.png")

    try {
        url.openStream().use { input ->
            FileOutputStream(outFile).use { output ->
                input.copyTo(output)
            }
        }
        return@withContext outFile.absolutePath
    } catch (e: Exception) {
        Log.w("DBDEBUG", "Failed to download image $remoteKey", e)
        return@withContext null
    }
}

suspend fun downloadAllCompendiumImages(
    context: Context,
    progress: (Float) -> Unit
) = withContext(Dispatchers.IO) {
    val db = get(context)
    val imageDao = db.compendiumEntryImageDao()
    val images = imageDao.getAllNow()

    val total = images.size.coerceAtLeast(1)
    var done = 0

    for (image in images) {
        val path = image.localPath
        val fileMissing = path != null && !File(path).exists()

        val hasFreshLocal =
            path != null &&
                    !fileMissing &&
                    image.localVersion == image.remoteVersion

        if (!hasFreshLocal) {
            if (fileMissing) {
                imageDao.updateLocalPathAndVersion(
                    id = image.id,
                    path = null,
                    localVersion = image.localVersion
                )
            }

            val newPath = downloadCompendiumImage(context, image.remoteKey)

            if (newPath != null) {
                imageDao.updateLocalPathAndVersion(
                    id = image.id,
                    path = newPath,
                    localVersion = image.remoteVersion
                )
            }
        }

        done++
        withContext(Dispatchers.Main) {
            progress(done.toFloat() / total)
        }
    }
}