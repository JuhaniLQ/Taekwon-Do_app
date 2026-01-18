package fi.tkd.itfun.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PatternDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entities: List<PatternEntity>)

    @Query("DELETE FROM patterns")
    suspend fun deleteAll()

    @Query("SELECT * FROM patterns ORDER BY id")
    fun getAll(): Flow<List<PatternEntity>>

    @Query("SELECT COUNT(*) FROM patterns")
    suspend fun count(): Int

    @Query("SELECT * FROM patterns")
    suspend fun getAllNow(): List<PatternEntity>

}