package fi.tkd.itfun.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TechniqueTemplateDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTemplate(entity: TechniqueTemplateEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTerms(rows: List<TechniqueTemplateTerm>)

    @Query("SELECT COUNT(*) FROM technique_templates")
    suspend fun count(): Int
}
