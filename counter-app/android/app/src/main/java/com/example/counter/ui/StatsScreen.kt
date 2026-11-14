package com.example.counter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.counter.domain.Dim
import com.example.counter.domain.StatView
import com.example.counter.domain.StatsCalculator

@Composable
private fun StatCard(dark: Boolean, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(cardBackground(dark))
            .padding(16.dp),
        content = content
    )
}

@Composable
fun StatsScreen(
    vm: CounterViewModel,
    ui: UiState,
    onOpenMore: () -> Unit,
    onOpenCategories: () -> Unit,
    onPeriodPick: () -> Unit
) {
    val dark = ui.settings.darkTheme
    val counters = vm.visibleCounters()
    val ids = counters.map { it.id }.toSet()
    val years = StatsCalculator.yearRange(ui.entries, ui.anchor)
    val period = StatsCalculator.buildPeriod(
        ui.dim, ui.anchor, ui.settings.weekStartMonday, years.first, years.second
    )
    val entries = StatsCalculator.filterEntries(ui.entries, period, ids)
    val summary = StatsCalculator.summarize(entries, counters)
    val filterLabel = if (ui.filterCategory == "all") "全部" else ui.filterCategory

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, bottom = 16.dp)
    ) {
        TopBar(
            leading = {
                CircleButton(onClick = onOpenCategories) {
                    Icon(Icons.Default.Menu, contentDescription = "分类", tint = textColor(dark))
                }
            },
            center = { TopTitle("统计", filterLabel, dark) },
            trailing = {
                CircleButton(onClick = onOpenMore) {
                    Icon(Icons.Default.MoreHoriz, contentDescription = "更多", tint = textColor(dark))
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        SegmentControl(
            items = listOf(
                Dim.DAY to "日", Dim.WEEK to "周", Dim.MONTH to "月",
                Dim.YEAR to "年", Dim.ALL to "全部"
            ),
            selected = ui.dim,
            dark = dark,
            onSelect = vm::setDim
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(28.dp).clickable { vm.shiftPeriod(-1) },
                contentAlignment = Alignment.Center
            ) { Text("‹", fontSize = 19.sp, color = AppColors.Muted) }
            Text(
                period.label,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor(dark),
                modifier = Modifier.padding(horizontal = 14.dp).clickable(onClick = onPeriodPick)
            )
            Box(
                modifier = Modifier.size(28.dp).clickable { vm.shiftPeriod(1) },
                contentAlignment = Alignment.Center
            ) { Text("›", fontSize = 19.sp, color = AppColors.Muted) }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                MetricCard("记录次数", summary.recordCount.toString(), dark, Modifier.weight(1f))
                Spacer(modifier = Modifier.width(10.dp))
                MetricCard("记录天数", summary.recordDays.toString(), dark, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                MetricCard("总计", formatNumber(summary.total), dark, Modifier.weight(1f))
                Spacer(modifier = Modifier.width(10.dp))
                MetricCard("计数器数量", summary.counterCount.toString(), dark, Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            SegmentControl(
                items = listOf(
                    StatView.CHART to "图表", StatView.PIE to "饼图", StatView.CALENDAR to "日历"
                ),
                selected = ui.view,
                dark = dark,
                onSelect = vm::setView,
                modifier = Modifier.width(252.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (ui.view) {
            StatView.PIE -> {
                val slices = counters.mapNotNull { counter ->
                    val value = entries.filter { it.counterId == counter.id }.sumOf { it.delta }
                    if (value > 0.0) {
                        PieSlice(counter.name, Color(counter.color.toInt()), value)
                    } else null
                }
                StatCard(dark) {
                    Text("各计数器占比", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = textColor(dark))
                    Text(
                        "${period.label} · 总计 ${formatNumber(summary.total)}",
                        fontSize = 12.sp,
                        color = AppColors.Muted
                    )
                    PieChart(slices, dark)
                }
            }

            StatView.CALENDAR -> {
                StatCard(dark) {
                    Text("日历分布", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = textColor(dark))
                    if (ui.dim == Dim.YEAR || ui.dim == Dim.ALL) {
                        Text(
                            "下方显示所选周期末月的每日分布",
                            fontSize = 11.sp,
                            color = AppColors.Muted
                        )
                    }
                    CalendarView(period, entries, ui.settings.weekStartMonday, dark)
                }
            }

            StatView.CHART -> {
                if (counters.isEmpty()) {
                    Text("没有可统计的计数器", fontSize = 13.sp, color = AppColors.Muted)
                }
                counters.forEach { counter ->
                    val own = entries.filter { it.counterId == counter.id }
                    val dayCount = own.map { it.localDate }.toHashSet().size
                    val sum = own.sumOf { it.delta }
                    val dayAverage = if (dayCount > 0) sum / dayCount else null
                    val values = StatsCalculator.counterSeries(period, entries, counter.id)
                    StatCard(dark) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                counter.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor(dark)
                            )
                            Spacer(modifier = Modifier.width(9.dp))
                            Text("|", fontSize = 16.sp, color = AppColors.Muted)
                            Spacer(modifier = Modifier.width(9.dp))
                            Text(
                                formatNumber(sum),
                                fontSize = 16.sp,
                                color = AppColors.Orange
                            )
                        }
                        Row(modifier = Modifier.padding(top = 4.dp)) {
                            Text("次数 ", fontSize = 12.sp, color = AppColors.Muted)
                            Text(
                                if (own.isEmpty()) "-" else own.size.toString(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor(dark)
                            )
                            Text("   天数 ", fontSize = 12.sp, color = AppColors.Muted)
                            Text(
                                if (dayCount == 0) "-" else dayCount.toString(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.Orange
                            )
                            Text("   日均 ", fontSize = 12.sp, color = AppColors.Muted)
                            Text(
                                if (dayAverage == null) "-" else formatNumber(dayAverage),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.Blue
                            )
                        }
                        Box(modifier = Modifier.padding(top = 12.dp)) {
                            BarChart(
                                values = values,
                                ticks = period.buckets.map { it.tickLabel },
                                tickFlags = period.buckets.map { it.tick },
                                dark = dark,
                                accent = Color(counter.color.toInt())
                            )
                        }
                    }
                }
            }
        }
    }
}
