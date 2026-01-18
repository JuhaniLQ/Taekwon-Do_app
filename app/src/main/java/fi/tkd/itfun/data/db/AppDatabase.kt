package fi.tkd.itfun.data.db

import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import androidx.room.RoomDatabase

@Database(
    entities = [
        PatternEntity::class,
        ItemEntity::class,
        ItemPatternCrossRef::class,
        CategoryEntity::class,
        TermEntity::class,
        TechniqueTemplateEntity::class,
        TechniqueTemplateTerm::class,
        ContentMetaEntity::class,
        BodyPartEntity::class
    ],
    version = 1,   // <-- bump by 1
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun patternDao(): PatternDao
    abstract fun itemDao(): ItemDao
    abstract fun categoryDao(): CategoryDao
    abstract fun termDao(): TermDao
    abstract fun techniqueTemplateDao(): TechniqueTemplateDao
    abstract fun contentMetaDao(): ContentMetaDao
    abstract fun bodyPartDao(): BodyPartDao

}

data class ItemWithPatterns(
    @Embedded val item: ItemEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ItemPatternCrossRef::class,
            parentColumn = "itemId",
            entityColumn = "patternId"
        )
    )
    val patterns: List<PatternEntity>
)

