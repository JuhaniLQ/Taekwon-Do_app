package fi.tkd.itfun.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "compendium_entries",
    foreignKeys = [
        ForeignKey(
            entity = CompendiumSectionEntity::class,
            parentColumns = ["sectionKey"],
            childColumns = ["sectionKey"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sectionKey")]
)
data class CompendiumEntryEntity(
    @PrimaryKey val entryKey: String,     // "attentionstance", "main"
    val sectionKey: String,               // "stances", "tenets"

    val titleEn: String?,
    val titleFi: String?,

    val descriptionEn: String,
    val descriptionFi: String,

    val sortOrder: Int = 0
)