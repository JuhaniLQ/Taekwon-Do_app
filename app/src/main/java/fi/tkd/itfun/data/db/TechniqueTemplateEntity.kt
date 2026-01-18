package fi.tkd.itfun.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "technique_templates")
data class TechniqueTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String // display name, e.g. "Knife Hand Strike Side"
)
