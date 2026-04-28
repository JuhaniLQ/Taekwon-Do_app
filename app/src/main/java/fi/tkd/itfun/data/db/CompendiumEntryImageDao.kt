package fi.tkd.itfun.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CompendiumEntryImageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CompendiumEntryImageEntity>)

    @Query("DELETE FROM compendium_entry_images")
    suspend fun deleteAll()

    @Query("SELECT * FROM compendium_entry_images")
    suspend fun getAllNow(): List<CompendiumEntryImageEntity>

    @Query("""
    UPDATE compendium_entry_images
    SET localPath = :path,
        localVersion = :localVersion
    WHERE id = :id
""")
    suspend fun updateLocalPathAndVersion(
        id: Int,
        path: String?,
        localVersion: Int
    )

    @Query("""
    SELECT * FROM compendium_entry_images
    WHERE entryKey = :entryKey
    ORDER BY sortOrder ASC
""")
    fun getImagesForEntry(entryKey: String): Flow<List<CompendiumEntryImageEntity>>
}