package fi.tkd.itfun.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CompendiumSectionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CompendiumSectionEntity>)

    @Query("DELETE FROM compendium_sections")
    suspend fun deleteAll()

    @Query("""
    SELECT * FROM compendium_sections
    WHERE groupKey = :groupKey
    ORDER BY sortOrder ASC
""")
    fun getAll(
        groupKey: String = "compendium"
    ): Flow<List<CompendiumSectionEntity>>
}