package fi.tkd.itfun.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VideoSeedingUpgradeTest {
    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private val files = mutableListOf<File>()
    private val prefix = "upgrade_test_${UUID.randomUUID()}"

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        db.categoryDao().insertAll(listOf(CategoryEntity(id = 1, name = "DEFENSIVE")))
    }

    @After
    fun tearDown() {
        db.close()
        files.forEach { it.delete() }
    }

    private fun video(key: String, content: String = "existing video"): File =
        File(context.getExternalFilesDir("videos"), "$key.mp4").also {
            it.parentFile!!.mkdirs()
            it.writeText(content)
            files += it
        }

    private fun oldItem(code: Int, key: String, path: String) = ItemEntity(
        techniqueCode = code, name = "Old name", koreanName = "Old Korean name",
        videoKey = key, remoteVideoVersion = 1, localVideoPath = path,
        localVideoVersion = 1, categoryId = 1, beltLevel = 9
    )

    private suspend fun import(code: Int, key: String, old: List<ItemEntity>, version: Int = 1): ItemEntity {
        val item = JSONObject().put("techniqueCode", code).put("name", "New name")
            .put("drawable", key).put("category", "DEFENSIVE").put("patterns", JSONArray())
            .put("koreanTerms", JSONArray().put("NEW")).put("beltLevel", 9).put("videoVersion", version)
        seedTechniquesFromArray(context, db, JSONArray().put(item), old.associateBy { it.videoKey },
            mapOf("NEW" to "New Korean name"))
        return db.itemDao().getAllNow().single()
    }

    @Test
    fun reusedCodeCannotInheritAnotherVideo() = runBlocking {
        val oldKey = "${prefix}_old"
        val old = oldItem(52, oldKey, video(oldKey).absolutePath)
        val result = import(52, "${prefix}_replacement", listOf(old))
        assertEquals("New name", result.name)
        assertEquals("New Korean name", result.koreanName)
        assertNull(result.localVideoPath)
        assertNull(result.localVideoVersion)
    }

    @Test
    fun renumberedTechniqueKeepsItsMatchingDownload() = runBlocking {
        val key = "${prefix}_same"
        val file = video(key)
        val result = import(99, key, listOf(oldItem(52, key, file.absolutePath)))
        assertEquals(99, result.techniqueCode)
        assertEquals(file.absolutePath, result.localVideoPath)
        assertEquals(1, result.localVideoVersion)
    }

    @Test
    fun previouslyMismatchedPathIsNotPreserved() = runBlocking {
        val key = "${prefix}_correct"
        val wrong = video("${prefix}_wrong")
        val result = import(52, key, listOf(oldItem(52, key, wrong.absolutePath)))
        assertNull(result.localVideoPath)
    }

    @Test
    fun changedVideoVersionRequiresDownloadEvenWhenFileExists() = runBlocking {
        val key = "${prefix}_versioned"
        val file = video(key)
        val result = import(52, key, listOf(oldItem(52, key, file.absolutePath)), version = 2)
        assertNull(result.localVideoPath)
        assertEquals(2, result.remoteVideoVersion)
    }

    @Test
    fun matchingFileCanBeRecoveredAfterDatabaseReset() = runBlocking {
        val key = "${prefix}_recovered"
        val file = video(key)
        assertEquals(file.absolutePath, import(52, key, emptyList()).localVideoPath)
    }

    @Test
    fun emptyFileIsNotRecoveredAsAUsableVideo() = runBlocking {
        val key = "${prefix}_empty"
        video(key, "")
        assertNull(import(52, key, emptyList()).localVideoPath)
    }
}
