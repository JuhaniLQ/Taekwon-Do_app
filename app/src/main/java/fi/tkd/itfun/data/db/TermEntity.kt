package fi.tkd.itfun.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "terms",
    indices = [Index(value = ["name"], unique = true)]
)
data class TermEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String // e.g., "stance", "block", "attack", "hand", "direction", "movement"
)
