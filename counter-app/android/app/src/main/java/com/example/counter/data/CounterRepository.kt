package com.example.counter.data

import com.example.counter.domain.TimeUtils
import kotlinx.coroutines.flow.first
import java.util.UUID
import androidx.room.withTransaction
import android.content.Context
import java.io.File

class CounterRepository(
    private val dao: CounterDao,
    private val settings: SettingsRepository,
    private val database: CounterDatabase,
    private val context: Context
) {

    suspend fun addEntry(counterId: String, delta: Double, occurredAt: Long, note: String = ""): EntryEntity = mutate {
        require(delta.isFinite() && delta != 0.0)
        val entry = EntryEntity(
            id = UUID.randomUUID().toString(),
            counterId = counterId,
            delta = delta,
            occurredAt = occurredAt,
            localDate = TimeUtils.ymdOfEpoch(occurredAt),
            tzOffsetSeconds = TimeUtils.offsetSeconds(occurredAt),
            note = note,
            createdAt = System.currentTimeMillis()
        )
        dao.upsertEntry(entry)
        recalc(counterId)
        entry
    }

    /** Rebuilds every result value from the immutable log, so edits and backfills stay consistent. */
    suspend fun recalc(counterId: String) {
        val mode = settings.settings.first().countMode
        val counter = dao.allCounters().firstOrNull { it.id == counterId } ?: return
        val entries = dao.entriesOf(counterId)
        var acc = if (mode == CountMode.TOTAL) counter.initialValue else 0.0
        var lastDate: String? = null
        val updated = entries.map { entry ->
            if (mode == CountMode.DAILY && entry.localDate != lastDate) {
                acc = 0.0
                lastDate = entry.localDate
            }
            acc += entry.delta
            entry.copy(valueAfter = acc)
        }
        dao.upsertEntries(updated)
    }

    suspend fun recalcAll() = database.withTransaction {
        dao.allCounters().forEach { recalc(it.id) }
    }

    suspend fun updateEntry(entry: EntryEntity) = mutate {
        dao.upsertEntry(entry)
        recalc(entry.counterId)
    }

    suspend fun deleteEntry(entry: EntryEntity) = mutate {
        dao.deleteEntry(entry)
        recalc(entry.counterId)
    }

    suspend fun undoLast() {
        val last = dao.allEntries().maxByOrNull { it.createdAt } ?: return
        deleteEntry(last)
    }

    suspend fun upsertCounter(counter: CounterEntity) = mutate {
        require(counter.name.isNotBlank() && counter.step.isFinite() && counter.step > 0 && counter.initialValue.isFinite() && counter.target.isFinite() && counter.target >= 0)
        dao.upsertCounter(counter)
        dao.addCategory(CategoryEntity(counter.category))
        recalc(counter.id)
    }

    suspend fun deleteCounter(id: String) = mutate {
        dao.deleteCounterById(id)
    }

    suspend fun addCategory(name: String) {
        dao.addCategory(CategoryEntity(name = name, sortOrder = 999))
    }

    /** Deleting a category must not orphan its counters; move them to the default one. */
    suspend fun deleteCategory(name: String) = mutate {
        dao.moveCountersToCategory(name, "默认")
        dao.deleteCategoryByName(name)
        dao.addCategory(CategoryEntity(name = "默认", sortOrder = 0))
    }

    suspend fun clearAll() = mutate {
        dao.clearEntries()
        dao.clearCounters()
        dao.clearCategories()
    }

    suspend fun replaceAll(
        counters: List<CounterEntity>,
        entries: List<EntryEntity>,
        categories: List<CategoryEntity>
    ) = mutate {
        dao.clearEntries()
        dao.clearCounters()
        dao.clearCategories()
        counters.forEach { dao.upsertCounter(it) }
        categories.forEach { dao.addCategory(it) }
        dao.upsertEntries(entries)
        recalcAll()
    }
    private suspend fun <T> mutate(block: suspend () -> T): T {
        val result = database.withTransaction { block() }
        // Keep the last successful local snapshot; AtomicFile preserves it on interruption.
        runCatching {
            database.withTransaction {
                val file = android.util.AtomicFile(File(context.filesDir, "automatic-backup.json"))
                val stream = file.startWrite()
                try {
                    stream.write(BackupCodec.encode(dao.allCounters(), dao.allEntries(),
                        dao.observeCategories().first()).toByteArray(Charsets.UTF_8))
                    file.finishWrite(stream)
                } catch (e: Exception) { file.failWrite(stream); throw e }
            }
        }
        return result
    }

    suspend fun restoreAutomatic(): Boolean {
        val file = File(context.filesDir, "automatic-backup.json")
        if (!file.exists()) return false
        val backup = BackupCodec.decode(file.readText())
        replaceAll(backup.counters, backup.entries, backup.categories)
        return true
    }
}
