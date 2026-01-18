package fi.tkd.itfun.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "body_parts")
data class BodyPartEntity(
    @PrimaryKey val code: String,          // "SOLAR_PLEXUS"
    val region: String,                    // HEAD / TORSO / ARM / LEG
    val canBeVitalPoint: Boolean,

    // names
    val nameEn: String,
    val nameKo: String
)
