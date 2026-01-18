package fi.tkd.itfun.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "items",
    indices = [
        Index(value = ["techniqueCode"], unique = true)  // stable external id
    ]
)
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,   // internal DB id
    val techniqueCode: Int,                              // 0–1000, from JSON
    val name: String,
    val koreanName: String,

    val videoKey: String,
    val remoteVideoVersion: Int,
    val localVideoPath: String? = null,
    val localVideoVersion: Int? = null,

    val categoryId: Long,
    val beltLevel: Int
)
