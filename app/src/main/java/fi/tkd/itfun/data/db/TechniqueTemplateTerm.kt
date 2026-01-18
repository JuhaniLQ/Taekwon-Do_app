package fi.tkd.itfun.data.db

import androidx.room.Entity

@Entity(
    tableName = "technique_template_term",
    primaryKeys = ["templateId", "position", "termId"]
)
data class TechniqueTemplateTerm(
    val templateId: Long,
    val position: Int,   // 0,1,2...
    val termId: Long
)
