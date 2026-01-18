package fi.tkd.itfun.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TermDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(terms: List<TermEntity>)

    @Query("SELECT COUNT(*) FROM terms")
    suspend fun count(): Int

    @Query("SELECT * FROM terms WHERE type = :type ORDER BY name")
    fun getByType(type: String): Flow<List<TermEntity>>

    @Query("SELECT * FROM terms")
    suspend fun getAllNow(): List<TermEntity>

}
