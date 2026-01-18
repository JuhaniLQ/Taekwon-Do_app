package fi.tkd.itfun.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query("DELETE FROM item_pattern")
    suspend fun deleteAllLinks()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: ItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLinks(links: List<ItemPatternCrossRef>)

    @Transaction
    @Query("""
        SELECT * FROM items 
        WHERE id IN (
            SELECT itemId FROM item_pattern 
            WHERE patternId = :patternId
        )
        ORDER BY name
    """)
    fun getItemsByPattern(patternId: Long): Flow<List<ItemEntity>>

    @Query("SELECT id FROM items WHERE name = :name LIMIT 1")
    suspend fun getIdByName(name: String): Long?

    @Query("SELECT COUNT(*) FROM items")
    suspend fun count(): Int

    @Query("SELECT * FROM items ORDER BY techniqueCode")
    fun getAll(): Flow<List<ItemEntity>>


    data class ItemPatternName(
        val itemId: Long,
        val patternName: String
    )

    @Query("""
    SELECT ip.itemId AS itemId, p.name AS patternName
    FROM item_pattern ip
    JOIN patterns p ON p.id = ip.patternId
    """)
    fun getItemPatternNames(): Flow<List<ItemPatternName>>


    @Query("DELETE FROM items")
    suspend fun deleteAll()

    @Upsert
    suspend fun upsert(item: ItemEntity): Long

    @Query("SELECT * FROM items")
    suspend fun getAllNow(): List<ItemEntity>

    @Query("""
    UPDATE items 
    SET localVideoPath = :path, localVideoVersion = :localVersion
    WHERE techniqueCode = :techniqueCode
""")
    suspend fun updateLocalVideoPathAndVersion(
        techniqueCode: Int,
        path: String?,
        localVersion: Int?
    )


}


