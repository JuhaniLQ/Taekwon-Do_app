package fi.tkd.itfun.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CompendiumEntryDao {

    @Query("SELECT * FROM compendium_entries ORDER BY sortOrder ASC")
    suspend fun getAllNow(): List<CompendiumEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CompendiumEntryEntity>)

    @Query("DELETE FROM compendium_entries")
    suspend fun deleteAll()

    @Query("""
    SELECT * FROM compendium_entries
    WHERE sectionKey = :sectionKey
    ORDER BY sortOrder ASC
""")
    fun getEntriesForSection(sectionKey: String): Flow<List<CompendiumEntryEntity>>
}
