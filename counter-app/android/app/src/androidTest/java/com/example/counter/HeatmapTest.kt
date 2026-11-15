package com.example.counter

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.counter.data.EntryEntity
import com.example.counter.domain.*
import com.example.counter.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class HeatmapTest {
    @get:Rule val rule = createComposeRule()
    private val day = LocalDate.of(2026, 11, 1)
    private val period = StatsCalculator.buildPeriod(Dim.MONTH, day, false, 2026, 2026)
    private fun entry(id: String, date: LocalDate, delta: Double) = EntryEntity(id, "test", delta,
        occurredAt = TimeUtils.toEpoch(date, 12, 0), localDate = date.toString())

    @Test fun tappingCellSelectsDetailsWithoutNavigating() {
        var opened: String? = null
        val rows = listOf(entry("1", day, 1.0), entry("2", day.plusDays(1), 4.0), entry("3", day.plusDays(2), -8.0))
        rule.setContent { CounterTheme(false) { Box(Modifier.width(360.dp)) { CalendarView(period, rows, false, false) { opened = it } } } }
        rule.onNodeWithTag("heatmap-2026-11-02").performClick()
        rule.onNodeWithTag("heatmap-selected-value").assertTextEquals("4")
        assertNull(opened)
        val bounds = rule.onNodeWithTag("heatmap-2026-11-02").fetchSemanticsNode().boundsInRoot
        assertEquals(bounds.width, bounds.height, 1f)
        rule.onNodeWithTag("heatmap-open-history").performClick()
        assertEquals("2026-11-02", opened)
        rule.onNodeWithTag("heatmap-2026-11-03").performClick()
        rule.onNodeWithTag("heatmap-selected-value").assertTextEquals("-8")
    }
    @Test fun zeroNetAndEmptyDaysStayDistinguishable() {
        val rows = listOf(entry("1", day, 3.0), entry("2", day, -3.0))
        rule.setContent { CounterTheme(true) { Box(Modifier.width(360.dp)) { CalendarView(period, rows, true, true) } } }
        rule.onNodeWithTag("heatmap-2026-11-01").performClick()
        rule.onNodeWithContentDescription("2026-11-01，净增量 0，2 条记录").assertExists()
        rule.onNodeWithTag("heatmap-selected-value").assertTextEquals("0")
        rule.onNodeWithTag("heatmap-2026-11-02").performClick()
        rule.onNodeWithText("当天暂无记录").assertExists()
    }
}
