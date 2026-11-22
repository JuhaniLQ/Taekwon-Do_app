package fi.tkd.itfun.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import fi.tkd.itfun.techniques.TkdPattern
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PatternSeedingTest {
    private lateinit var context: Context
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun newNamesAreSeededAndLinkedWithoutAConstantOrDiagram() = runBlocking {
        db.categoryDao().insertAll(listOf(
            CategoryEntity(id = 1, name = "ALL_PATTERNS"),
            CategoryEntity(id = 2, name = "DEFENSIVE")
        ))
        val techniques = JSONArray("""[
            {"techniqueCode":900,"name":"New pattern","drawable":"seeding_test_pattern",
             "category":"ALL_PATTERNS","patterns":[" New Pattern ","New Pattern",""],
             "koreanTerms":["TEST_NAME"],"beltLevel":4},
            {"techniqueCode":901,"name":"Example block","drawable":"seeding_test_block",
             "category":"DEFENSIVE","patterns":["New Pattern","Another Pattern"],
             "koreanTerms":["TEST_NAME"],"beltLevel":9}
        ]""")

        seedTechniquesFromArray(context, db, techniques, emptyMap(), mapOf("TEST_NAME" to "Test"))

        assertEquals(setOf("New Pattern", "Another Pattern"), db.patternDao().getAllNow().map { it.name }.toSet())
        val newPatternId = db.patternDao().getAllNow().first { it.name == "New Pattern" }.id
        assertEquals(
            setOf("New pattern", "Example block"),
            db.itemDao().getItemsByPattern(newPatternId).first().map { it.name }.toSet()
        )
        assertEquals(3, db.itemDao().getItemPatternNames().first().size)
        assertEquals(TkdPattern.UNKNOWN, TkdPattern.fromString("New Pattern"))
    }

    @Test
    fun subsequentImportUsesTheReplacementNames() = runBlocking {
        db.categoryDao().insertAll(listOf(CategoryEntity(id = 1, name = "ALL_PATTERNS")))
        fun techniques(name: String) = JSONArray("""[
            {"techniqueCode":900,"name":"Pattern","drawable":"seeding_test_pattern",
             "category":"ALL_PATTERNS","patterns":["$name"],"koreanTerms":[],"beltLevel":4}
        ]""")
        seedTechniquesFromArray(context, db, techniques("First Pattern"), emptyMap(), emptyMap())

        // These are cleared by both the initial import and Update before seeding.
        db.itemDao().deleteAllLinks()
        db.itemDao().deleteAll()
        db.patternDao().deleteAll()
        seedTechniquesFromArray(context, db, techniques("Later Pattern"), emptyMap(), emptyMap())

        assertEquals(listOf("Later Pattern"), db.patternDao().getAllNow().map { it.name })
        assertEquals(listOf("Later Pattern"), db.itemDao().getItemPatternNames().first().map { it.patternName })
    }
}
