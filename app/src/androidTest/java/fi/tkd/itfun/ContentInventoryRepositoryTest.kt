package fi.tkd.itfun

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import fi.tkd.itfun.data.db.AppDatabase
import fi.tkd.itfun.data.db.BodyPartEntity
import fi.tkd.itfun.data.db.CategoryEntity
import fi.tkd.itfun.data.db.CompendiumEntryEntity
import fi.tkd.itfun.data.db.CompendiumEntryImageEntity
import fi.tkd.itfun.data.db.CompendiumSectionEntity
import fi.tkd.itfun.data.db.ItemEntity
import fi.tkd.itfun.data.db.ContentMetaEntity
import fi.tkd.itfun.data.db.DbProvider
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.json.JSONObject

class ContentInventoryRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var directory: File
    private lateinit var repository: ContentInventoryRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        directory = Files.createTempDirectory(context.cacheDir.toPath(), "inventory-test").toFile()
        repository = ContentInventoryRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
        directory.deleteRecursively()
    }

    @Test
    fun inventoryCountsAllEntriesAndChecksFilesAgainOnRefresh() = runBlocking {
        val downloaded = File(directory, "downloaded.mp4").apply { writeBytes(byteArrayOf(1)) }
        val empty = File(directory, "empty.mp4").apply { createNewFile() }
        val missing = File(directory, "missing.mp4")
        db.categoryDao().insertAll(listOf(CategoryEntity(id = 1, name = "FUTURE_CATEGORY")))
        listOf(downloaded.path, empty.path, missing.path, directory.path, null).forEachIndexed { index, path ->
            db.itemDao().insert(
                ItemEntity(
                    techniqueCode = index, name = "Card $index", koreanName = "Name $index",
                    videoKey = "video$index", remoteVideoVersion = 1,
                    localVideoPath = path, categoryId = 1, beltLevel = if (index == 0) 1 else 9
                )
            )
        }
        db.bodyPartDao().insertAll(
            listOf(BodyPartEntity("TEST", "NEW_REGION", false, "Test area", "Test"))
        )
        db.compendiumSectionDao().insertAll(
            listOf(CompendiumSectionEntity("future", "compendium", "New section", ""))
        )
        db.compendiumEntryDao().insertAll(
            listOf(CompendiumEntryEntity("entry", "future", "Entry", null, "Description", ""))
        )
        db.compendiumEntryImageDao().insertAll(
            listOf(
                CompendiumEntryImageEntity(1, "entry", "present", downloaded.path, 1, 1),
                CompendiumEntryImageEntity(2, "entry", "missing", missing.path, 1, 1)
            )
        )

        val inventory = repository.getInventory()
        assertEquals(listOf(TechniqueInventory("Future category", 5, 1)), inventory.techniques)
        assertEquals(listOf(BodyAreaInventory("New region", 1)), inventory.bodyAreas)
        assertEquals(listOf(CompendiumInventory("New section", 1, 1, 2)), inventory.compendium)

        downloaded.delete()
        val refreshed = repository.getInventory()
        assertEquals(5, refreshed.techniques.single().cards)
        assertEquals(0, refreshed.techniques.single().videos)
        assertEquals(0, refreshed.compendium.single().images)
    }

    @Test
    fun emptyDatabaseHasNoInventedContent() = runBlocking {
        assertEquals(ContentInventory(emptyList(), emptyList(), emptyList()), repository.getInventory())
    }

    @Test
    fun previewFindsNewIdsEvenWhenTotalsAreUnchangedAndNeverImportsThem() = runBlocking {
        db.contentMetaDao().put(ContentMetaEntity(DbProvider.JSON_MANIFEST_VERSION_KEY, 1))
        db.categoryDao().insertAll(listOf(CategoryEntity(1, "DEFENSIVE"), CategoryEntity(2, "STANCES")))
        db.itemDao().insert(ItemEntity(
            techniqueCode = 1, name = "Old block", koreanName = "Old block", videoKey = "old",
            remoteVideoVersion = 1, categoryId = 1, beltLevel = 9
        ))
        db.itemDao().insert(ItemEntity(
            techniqueCode = 99, name = "Legacy stance", koreanName = "Stance", videoKey = "stance",
            remoteVideoVersion = 1, categoryId = 2, beltLevel = 9
        ))
        val remote = JSONObject("""{
            "version":2,
            "techniques":[
                {"techniqueCode":2,"category":"DEFENSIVE"},
                {"techniqueCode":500,"category":"SPECIAL"},
                {"techniqueCode":100,"category":"STANCES"}
            ],
            "bodyParts":[{"code":"NEW","region":"HEAD"}],
            "newSection":{"section":"compendium","title":{"en":"New section"},
                "entries":{"newEntry":{"resources":{"images":["imageA","imageB"]}}}}
        }""")

        val preview = repository.getInventory(remote)
        assertTrue(preview.hasNewContent)
        assertEquals(listOf(
            TechniqueInventory("Blocks", 1, 0, additions = 1),
            TechniqueInventory("Special", 0, 0, additions = 1)
        ), preview.techniques)
        assertEquals(listOf(BodyAreaInventory("Head", 0, additions = 1)), preview.bodyAreas)
        assertEquals(listOf(CompendiumInventory("New section", 0, 0, 0, 1, 2)), preview.compendium)
        assertEquals(2, db.itemDao().count())
        assertEquals(emptyList<CompendiumSectionEntity>(), db.compendiumSectionDao().getAllNow())

        db.contentMetaDao().put(ContentMetaEntity(DbProvider.JSON_MANIFEST_VERSION_KEY, 2))
        val current = repository.getInventory(remote)
        assertFalse(current.hasNewContent)
        assertEquals(listOf(TechniqueInventory("Blocks", 1, 0)), current.techniques)
        assertTrue(current.bodyAreas.isEmpty())
        assertTrue(current.compendium.isEmpty())
    }
}
