package com.example.counter.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TimeUtils {

    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun today(): LocalDate = LocalDate.now()

    fun todayYmd(): String = ymd(today())

    fun ymd(date: LocalDate): String = date.format(formatter)

    fun parse(value: String): LocalDate = LocalDate.parse(value, formatter)

    fun ymdOfEpoch(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate().format(formatter)

    fun offsetSeconds(epochMillis: Long): Int =
        ZoneId.systemDefault().rules.getOffset(Instant.ofEpochMilli(epochMillis)).totalSeconds

    fun toEpoch(date: LocalDate, hour: Int, minute: Int): Long =
        date.atTime(hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun toEpoch(date: LocalDate, time: LocalTime): Long = toEpoch(date, time.hour, time.minute)

    fun dateTimeParts(epochMillis: Long): Pair<Int, Int> {
        val zoned = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
        return zoned.hour to zoned.minute
    }

    /** Monday = 1 ... Sunday = 7. */
    fun weekStart(date: LocalDate, monday: Boolean): LocalDate {
        val dow = date.dayOfWeek.value
        return if (monday) {
            date.minusDays((dow - 1).toLong())
        } else {
            date.minusDays((dow % 7).toLong())
        }
    }
}
