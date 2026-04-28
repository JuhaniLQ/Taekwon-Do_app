package fi.tkd.itfun.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "compendium_sections")
data class CompendiumSectionEntity(
    @PrimaryKey val sectionKey: String,   // "stances", "tenets"
    val groupKey: String,                 // "compendium"
    val titleEn: String,
    val titleFi: String,
    val sortOrder: Int = 0
)