package fi.tkd.itfun.data.db

import androidx.room.Entity

@Entity(
    tableName = "item_pattern",
    primaryKeys = ["itemId", "patternId"]
)
data class ItemPatternCrossRef(
    val itemId: Long,
    val patternId: Long
)
