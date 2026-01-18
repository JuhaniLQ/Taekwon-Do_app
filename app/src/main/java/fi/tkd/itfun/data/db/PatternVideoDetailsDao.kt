package fi.tkd.itfun.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PatternVideoDetailsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(details: PatternVideoDetailsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(details: List<PatternVideoDetailsEntity>)

    @Query("SELECT * FROM pattern_video_details WHERE videoKey = :videoKey LIMIT 1")
    suspend fun getByVideoKey(videoKey: String): PatternVideoDetailsEntity?

    @Query("DELETE FROM pattern_video_details")
    suspend fun deleteAll()
}