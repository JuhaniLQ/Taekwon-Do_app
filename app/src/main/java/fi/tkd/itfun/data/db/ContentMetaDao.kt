package fi.tkd.itfun.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ContentMetaDao {

    @Query("SELECT value FROM content_meta WHERE `key` = :key LIMIT 1")
    suspend fun getValue(key: String): Int?

    @Query("SELECT value FROM content_meta WHERE `key` = :key LIMIT 1")
    fun observeValue(key: String): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: ContentMetaEntity)
}
