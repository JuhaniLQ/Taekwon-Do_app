package fi.tkd.itfun.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class BeltSettingsTest {
    @Test
    fun manifestFlagIsStoredAndDefaultsToDisabledWhenMissing() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val flag = db.contentMetaDao().observeValue(DbProvider.BLACK_BELTS_ENABLED_KEY)
            assertEquals(null, flag.first())
            seedBeltSettings(db, JSONObject().put("black belts enabled", true))
            assertEquals(1, flag.first())
            seedBeltSettings(db, JSONObject().put("black belts enabled", false))
            assertEquals(0, flag.first())
            seedBeltSettings(db, JSONObject().put("black belts enabled", true))
            seedBeltSettings(db, JSONObject())
            assertEquals(0, flag.first())
        } finally {
            db.close()
        }
    }
}
