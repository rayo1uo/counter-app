package com.example.counter.domain

import com.example.counter.data.CounterEntity
import com.example.counter.data.CountMode
import com.example.counter.data.EntryEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

enum class Dim { DAY, WEEK, MONTH, YEAR, ALL }

enum class StatView { CHART, PIE, CALENDAR }

data class Bucket(
    val start: Long,
    val end: Long,
    val tick: Boolean,
    val tickLabel: String
)

data class Period(
    val start: Long,
    val end: Long,
    val label: String,
    val buckets: List<Bucket>,
    val calendarMonth: YearMonth
)

data class Summary(
    val recordCount: Int,
    val recordDays: Int,
    val total: Double,
    val counterCount: Int
)

object StatsCalculator {

    private val WEEK_LABELS = arrayOf("日", "一", "二", "三", "四", "五", "六")

    private fun LocalDateTime.millis(): Long =
        atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun dayBounds(date: LocalDate): Pair<Long, Long> =
        date.atStartOfDay().millis() to date.atTime(23, 59, 59, 999_000_000).millis()

    fun buildPeriod(
        dim: Dim,
        anchor: LocalDate,
        monday: Boolean,
        earliestYear: Int,
        latestYear: Int
    ): Period {
        val month = YearMonth.from(anchor)
        return when (dim) {
            Dim.DAY -> {
                val (start, end) = dayBounds(anchor)
                val buckets = (0..23).map { hour ->
                    val from = anchor.atTime(hour, 0)
                    val to = if (hour == 23) anchor.atTime(23, 59, 59, 999_000_000)
                    else anchor.atTime(hour + 1, 0).minusNanos(1)
                    Bucket(from.millis(), to.millis(), hour % 6 == 0, hour.toString())
                }
                Period(start, end, "${anchor.year}年${anchor.monthValue}月${anchor.dayOfMonth}日", buckets, month)
            }

            Dim.WEEK -> {
                val first = TimeUtils.weekStart(anchor, monday)
                val last = first.plusDays(6)
                val buckets = (0..6).map { index ->
                    val date = first.plusDays(index.toLong())
                    val (from, to) = dayBounds(date)
                    Bucket(from, to, true, WEEK_LABELS[date.dayOfWeek.value % 7])
                }
                val (start, _) = dayBounds(first)
                val (_, end) = dayBounds(last)
                val label = "${first.monthValue}月${first.dayOfMonth}日 - ${last.monthValue}月${last.dayOfMonth}日"
                Period(start, end, label, buckets, month)
            }

            Dim.MONTH -> {
                val days = month.lengthOfMonth()
                val buckets = (1..days).map { day ->
                    val (from, to) = dayBounds(month.atDay(day))
                    Bucket(from, to, day == 1 || day % 3 == 0, day.toString())
                }
                val (start, _) = dayBounds(month.atDay(1))
                val (_, end) = dayBounds(month.atDay(days))
                Period(start, end, "${month.year}年${month.monthValue}月", buckets, month)
            }

            Dim.YEAR -> {
                val year = anchor.year
                val buckets = (1..12).map { m ->
                    val ym = YearMonth.of(year, m)
                    val from = ym.atDay(1).atStartOfDay().millis()
                    val to = ym.atEndOfMonth().atTime(23, 59, 59, 999_000_000).millis()
                    Bucket(from, to, m % 2 == 1, m.toString())
                }
                val start = LocalDate.of(year, 1, 1).atStartOfDay().millis()
                val end = LocalDate.of(year, 12, 31).atTime(23, 59, 59, 999_000_000).millis()
                Period(start, end, "${year}年", buckets, month)
            }

            Dim.ALL -> {
                val lo = minOf(earliestYear, latestYear, anchor.year)
                val hi = maxOf(earliestYear, latestYear, anchor.year)
                val buckets = (lo..hi).map { year ->
                    val from = LocalDate.of(year, 1, 1).atStartOfDay().millis()
                    val to = LocalDate.of(year, 12, 31).atTime(23, 59, 59, 999_000_000).millis()
                    Bucket(from, to, true, year.toString())
                }
                val start = LocalDate.of(lo, 1, 1).atStartOfDay().millis()
                val end = LocalDate.of(hi, 12, 31).atTime(23, 59, 59, 999_000_000).millis()
                Period(start, end, "$lo - $hi", buckets, month)
            }
        }
    }

    fun filterEntries(
        entries: List<EntryEntity>,
        period: Period,
        counterIds: Set<String>
    ): List<EntryEntity> = entries.filter { entry ->
        entry.counterId in counterIds &&
            entry.occurredAt >= period.start &&
            entry.occurredAt <= period.end
    }

    fun summarize(entries: List<EntryEntity>, counters: List<CounterEntity>): Summary = Summary(
        recordCount = entries.size,
        recordDays = entries.map { it.localDate }.toHashSet().size,
        total = entries.sumOf { it.delta },
        counterCount = counters.size
    )

    fun bucketValues(period: Period, entries: List<EntryEntity>): List<Double> =
        period.buckets.map { bucket ->
            entries.filter { it.occurredAt >= bucket.start && it.occurredAt <= bucket.end }
                .sumOf { it.delta }
        }

    fun counterSeries(period: Period, entries: List<EntryEntity>, counterId: String): List<Double> =
        bucketValues(period, entries.filter { it.counterId == counterId })

    fun dayValue(
        entries: List<EntryEntity>,
        counterId: String,
        date: String,
        mode: CountMode,
        initialValue: Double
    ): Double {
        val matched = entries.filter { entry ->
            entry.counterId == counterId &&
                if (mode == CountMode.DAILY) entry.localDate == date else entry.localDate <= date
        }
        return matched.sumOf { it.delta } + if (mode == CountMode.TOTAL) initialValue else 0.0
    }

    fun lastOccurred(entries: List<EntryEntity>, counterId: String, date: String): Long? =
        entries.filter { it.counterId == counterId && it.localDate == date }
            .maxOfOrNull { it.occurredAt }

    fun minutesSince(epochMillis: Long): Long =
        Math.max(0L, (System.currentTimeMillis() - epochMillis) / 60_000L)

    fun yearRange(entries: List<EntryEntity>, anchor: LocalDate): Pair<Int, Int> {
        if (entries.isEmpty()) return anchor.year to anchor.year
        val years = entries.mapNotNull { it.localDate.take(4).toIntOrNull() }
        if (years.isEmpty()) return anchor.year to anchor.year
        return years.minOrNull()!! to years.maxOrNull()!!
    }
}
