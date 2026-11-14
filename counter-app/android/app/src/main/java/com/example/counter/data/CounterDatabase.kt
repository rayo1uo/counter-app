package com.example.counter.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CounterEntity::class, EntryEntity::class, CategoryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class CounterDatabase : RoomDatabase() {

    abstract fun dao(): CounterDao

    companion object {
        fun build(context: Context): CounterDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                CounterDatabase::class.java,
                "counter.db"
            ).addMigrations(object : androidx.room.migration.Migration(1, 2) {
                override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE counters ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
                }
            }).build()
    }
}
