package fi.tkd.itfun.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "compendium_entry_images",
    foreignKeys = [
        ForeignKey(
            entity = CompendiumEntryEntity::class,
            parentColumns = ["entryKey"],
            childColumns = ["entryKey"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("entryKey")]
)
data class CompendiumEntryImageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    val entryKey: String,          // link to entry

    val remoteKey: String,         // e.g. "attentionstance"
    val localPath: String?,        // downloaded file path

    val localVersion: Int,         // what we currently have
    val remoteVersion: Int,        // from JSON

    val sortOrder: Int = 0
)