package com.example.counter.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CounterEntity::class, EntryEntity::class, CategoryEntity::class],
    version = 1,
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
            ).fallbackToDestructiveMigration().build()
    }
}
