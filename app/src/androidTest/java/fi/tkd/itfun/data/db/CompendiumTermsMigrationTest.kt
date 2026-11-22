package fi.tkd.itfun.data.db

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompendiumTermsMigrationTest {
    @Test
    fun addingTermsPreservesExistingCompendiumEntries() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("""CREATE TABLE compendium_entries (
                        entryKey TEXT NOT NULL PRIMARY KEY, sectionKey TEXT NOT NULL,
                        titleEn TEXT, titleFi TEXT, descriptionEn TEXT NOT NULL,
                        descriptionFi TEXT NOT NULL, sortOrder INTEGER NOT NULL)
                    """)
                    db.execSQL("""INSERT INTO compendium_entries VALUES
                        ('existing', 'history', 'History', NULL, 'Keep this content', '', 3)
                    """)
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            }).build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
        try {
            val db = helper.writableDatabase
            DbProvider.MIGRATION_2_3.migrate(db)
            db.query("SELECT descriptionEn, sortOrder, termsJson FROM compendium_entries WHERE entryKey = 'existing'").use {
                assertTrue(it.moveToFirst())
                assertEquals("Keep this content", it.getString(0))
                assertEquals(3, it.getInt(1))
                assertTrue(it.isNull(2))
                assertEquals(1, it.count)
            }
        } finally {
            helper.close()
        }
    }
}
