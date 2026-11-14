package com.example.counter

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.room.Room
import com.example.counter.data.*
import com.example.counter.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CounterAcceptanceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: CounterDatabase
    private lateinit var repo: CounterRepository
    private lateinit var settings: SettingsRepository
    private val day = java.time.LocalDate.of(2026, 11, 14)
    private fun at(offset: Long = 0) = TimeUtils.toEpoch(day.plusDays(offset), 12, 0)
    @Before fun setup() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(context, CounterDatabase::class.java).build()
        settings = SettingsRepository(context)
        settings.setCountMode(CountMode.DAILY)
        repo = CounterRepository(db.dao(), settings, db, context)
        repo.upsertCounter(CounterEntity("c", "检车", 0xFFF5921E, initialValue = 10.0))
    }
    @After fun close() { db.close() }

    @Test fun rapidCountsAreAtomicAndPersisted() = runBlocking {
        coroutineScope { repeat(40) { launch(Dispatchers.IO) { repo.addEntry("c", 1.0, at() + it) } } }
        val rows = db.dao().entriesOf("c")
        assertEquals(40, rows.size)
        assertEquals(40.0, rows.last().valueAfter, 0.0)
        assertEquals(40.0, rows.sumOf { it.delta }, 0.0)
    }
    @Test fun editingCounterPreservesHistory() = runBlocking {
        repo.addEntry("c", 3.0, at())
        repo.upsertCounter(db.dao().allCounters().first().copy(name = "新名称", pinned = true))
        assertEquals(1, db.dao().allEntries().size)
        assertTrue(db.dao().allCounters().first().pinned)
    }
    @Test fun backfillEditDeleteRecalculatesFollowingResults() = runBlocking {
        repo.addEntry("c", 4.0, at() + 1000)
        val first = repo.addEntry("c", 2.0, at())
        assertEquals(6.0, db.dao().entriesOf("c").last().valueAfter, 0.0)
        repo.updateEntry(first.copy(delta = 7.0))
        assertEquals(11.0, db.dao().entriesOf("c").last().valueAfter, 0.0)
        repo.deleteEntry(first)
        assertEquals(4.0, db.dao().entriesOf("c").last().valueAfter, 0.0)
    }
    @Test fun dailyAndCumulativeModesKeepSeparateMeanings() = runBlocking {
        repo.addEntry("c", 3.0, at())
        repo.addEntry("c", -1.0, at(1))
        assertEquals(-1.0, db.dao().entriesOf("c").last().valueAfter, 0.0)
        settings.setCountMode(CountMode.TOTAL); repo.recalcAll()
        assertEquals(12.0, db.dao().entriesOf("c").last().valueAfter, 0.0)
        assertEquals(0.0, StatsCalculator.dayValue(db.dao().allEntries(), "c", "2026-11-16", CountMode.DAILY, 10.0), 0.0)
    }
    @Test fun undoRemovesExactEntryEvenAfterAnotherCount() = runBlocking {
        val first = repo.addEntry("c", 2.0, at())
        repo.addEntry("c", 5.0, at()+1000)
        repo.deleteEntry(first)
        assertEquals(5.0, db.dao().entriesOf("c").single().valueAfter, 0.0)
    }
    @Test fun failedRestoreRollsBackEntireTransaction() = runBlocking {
        repo.addEntry("c", 2.0, at())
        try { repo.replaceAll(emptyList(), listOf(EntryEntity("bad", "missing", 1.0, occurredAt=at(), localDate="2026-11-14")), emptyList()); fail() } catch (_: Exception) {}
        assertEquals("c", db.dao().allCounters().single().id)
        assertEquals(1, db.dao().allEntries().size)
    }
    @Test fun backupRoundTripAndValidation() = runBlocking {
        repo.addEntry("c", 2.5, at(), "备注,\"测试\"")
        val text = BackupCodec.encode(db.dao().allCounters(), db.dao().allEntries(), emptyList())
        val parsed = BackupCodec.decode(text)
        assertEquals(2.5, parsed.entries.single().delta, 0.0)
        try { BackupCodec.decode(text.replace("\"counterId\": \"c\"", "\"counterId\": \"missing\"")); fail() } catch (_: IllegalArgumentException) {}
    }
    @Test fun lastMillisecondIncludedInStats() {
        val p = StatsCalculator.buildPeriod(Dim.DAY, day, true, 2026, 2026)
        val row = EntryEntity("e", "c", -2.0, occurredAt=p.end, localDate="2026-11-14")
        assertEquals(-2.0, StatsCalculator.bucketValues(p, listOf(row)).sum(), 0.0)
    }
    @Test fun categoryDeletionMovesCounters() = runBlocking {
        repo.upsertCounter(db.dao().allCounters().single().copy(category="工作"))
        repo.deleteCategory("工作")
        assertEquals("默认", db.dao().allCounters().single().category)
    }
}
