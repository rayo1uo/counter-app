package com.example.counter.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CounterDao {

    @Query("SELECT * FROM counters WHERE archived = 0 ORDER BY pinned DESC, sortOrder ASC, createdAt ASC")
    fun observeCounters(): Flow<List<CounterEntity>>

    @Query("SELECT * FROM entries ORDER BY occurredAt DESC")
    fun observeEntries(): Flow<List<EntryEntity>>

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, name ASC")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM counters")
    suspend fun allCounters(): List<CounterEntity>

    @Query("SELECT * FROM entries")
    suspend fun allEntries(): List<EntryEntity>

    // Must match the order used by history/CSV, otherwise same-minute entries get
    // different result values depending on their random UUID.
    @Query(
        "SELECT * FROM entries WHERE counterId = :counterId " +
            "ORDER BY localDate ASC, occurredAt ASC, createdAt ASC, id ASC"
    )
    suspend fun entriesOf(counterId: String): List<EntryEntity>

    @Query("SELECT COUNT(*) FROM counters")
    suspend fun counterCount(): Int

    @androidx.room.Upsert
    suspend fun upsertCounter(counter: CounterEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntry(entry: EntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntries(entries: List<EntryEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteEntry(entry: EntryEntity)

    @Query("DELETE FROM counters WHERE id = :id")
    suspend fun deleteCounterById(id: String)

    @Query("UPDATE counters SET category = :target WHERE category = :name")
    suspend fun moveCountersToCategory(name: String, target: String)

    @Query("DELETE FROM categories WHERE name = :name")
    suspend fun deleteCategoryByName(name: String)

    @Query("DELETE FROM entries")
    suspend fun clearEntries()

    @Query("DELETE FROM counters")
    suspend fun clearCounters()

    @Query("DELETE FROM categories")
    suspend fun clearCategories()
}
