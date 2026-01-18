package fi.tkd.itfun.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "content_meta")
data class ContentMetaEntity(
    @PrimaryKey val key: String,
    val value: Int
)
