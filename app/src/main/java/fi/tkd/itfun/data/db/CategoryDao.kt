package fi.tkd.itfun.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Query("SELECT * FROM categories")
    suspend fun getAllNow(): List<CategoryEntity>

    @Query("SELECT * FROM categories ORDER BY id")
    fun getAll(): Flow<List<CategoryEntity>>

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}
