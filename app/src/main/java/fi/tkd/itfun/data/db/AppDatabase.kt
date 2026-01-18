package fi.tkd.itfun.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        PatternEntity::class,
        ItemEntity::class,
        ItemPatternCrossRef::class,
        CategoryEntity::class,
        ContentMetaEntity::class,
        BodyPartEntity::class,
        PatternVideoDetailsEntity::class
    ],
    version = 1,   // <-- bump by 1
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun patternDao(): PatternDao
    abstract fun itemDao(): ItemDao
    abstract fun categoryDao(): CategoryDao
    abstract fun contentMetaDao(): ContentMetaDao
    abstract fun bodyPartDao(): BodyPartDao
    abstract fun patternVideoDetailsDao(): PatternVideoDetailsDao

}
