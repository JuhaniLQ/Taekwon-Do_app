package fi.tkd.itfun.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pattern_video_details")
data class PatternVideoDetailsEntity(
    @PrimaryKey val videoKey: String,   // e.g. "chonji", same as videoKey/drawable
    val timecodesJson: String,
    val shortJson: String,
    val fullJson: String
)