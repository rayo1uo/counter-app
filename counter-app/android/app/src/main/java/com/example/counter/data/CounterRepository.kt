package com.example.counter.data

import com.example.counter.domain.TimeUtils
import kotlinx.coroutines.flow.first
import java.util.UUID

class CounterRepository(
    private val dao: CounterDao,
    private val settings: SettingsRepository
) {

    suspend fun seedIfEmpty() {
        if (dao.counterCount() > 0) return
        val now = System.currentTimeMillis()
        val truck = CounterEntity(
            id = "seed-truck", name = "大型车", color = 0xFF9B9B9B,
            category = "工作", createdAt = now
        )
        val check = CounterEntity(
            id = "seed-check", name = "检车", color = 0xFFF5921E,
            category = "工作", createdAt = now + 1
        )
        dao.upsertCounter(truck)
        dao.upsertCounter(check)
        dao.addCategory(CategoryEntity("工作", 0))
        dao.addCategory(CategoryEntity("生活", 1))

        val t2 = now - 60_000L
        val t1 = t2 - 61 * 60_000L
        dao.upsertEntry(
            EntryEntity(
                id = "seed-e1", counterId = check.id, delta = 1.0, valueAfter = 1.0,
                occurredAt = t1, localDate = TimeUtils.ymdOfEpoch(t1), createdAt = t1
            )
        )
        dao.upsertEntry(
            EntryEntity(
                id = "seed-e2", counterId = check.id, delta = 12.0, valueAfter = 13.0,
                occurredAt = t2, localDate = TimeUtils.ymdOfEpoch(t2), createdAt = t2
            )
        )
    }

    suspend fun addEntry(counterId: String, delta: Double, occurredAt: Long, note: String = "") {
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

    suspend fun recalcAll() {
        dao.allCounters().forEach { recalc(it.id) }
    }

    suspend fun updateEntry(entry: EntryEntity) {
        dao.upsertEntry(entry)
        recalc(entry.counterId)
    }

    suspend fun deleteEntry(entry: EntryEntity) {
        dao.deleteEntry(entry)
        recalc(entry.counterId)
    }

    suspend fun undoLast() {
        val last = dao.allEntries().maxByOrNull { it.createdAt } ?: return
        deleteEntry(last)
    }

    suspend fun upsertCounter(counter: CounterEntity) {
        dao.upsertCounter(counter)
        recalc(counter.id)
    }

    suspend fun deleteCounter(id: String) {
        dao.deleteCounterById(id)
    }

    suspend fun addCategory(name: String) {
        dao.addCategory(CategoryEntity(name = name, sortOrder = 999))
    }

    suspend fun clearAll() {
        dao.clearEntries()
        dao.clearCounters()
        dao.clearCategories()
    }

    suspend fun replaceAll(
        counters: List<CounterEntity>,
        entries: List<EntryEntity>,
        categories: List<CategoryEntity>
    ) {
        dao.clearEntries()
        dao.clearCounters()
        dao.clearCategories()
        counters.forEach { dao.upsertCounter(it) }
        categories.forEach { dao.addCategory(it) }
        dao.upsertEntries(entries)
        recalcAll()
    }
}
