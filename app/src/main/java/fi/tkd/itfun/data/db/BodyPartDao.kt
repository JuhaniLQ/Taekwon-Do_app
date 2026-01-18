package fi.tkd.itfun.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyPartDao {

    @Query("SELECT * FROM body_parts ORDER BY region, nameEn")
    fun getAll(): Flow<List<BodyPartEntity>>

    @Query("SELECT * FROM body_parts")
    suspend fun getAllNow(): List<BodyPartEntity>

    @Query("SELECT * FROM body_parts WHERE code = :code LIMIT 1")
    suspend fun getByCode(code: String): BodyPartEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<BodyPartEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: BodyPartEntity)

    @Query("DELETE FROM body_parts")
    suspend fun deleteAll()
}
