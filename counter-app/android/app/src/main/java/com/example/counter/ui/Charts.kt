package com.example.counter.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.counter.data.EntryEntity
import com.example.counter.domain.Period
import java.time.LocalDate

data class PieSlice(val label: String, val color: Color, val value: Double)

@Composable
fun BarChart(
    values: List<Double>,
    ticks: List<String>,
    tickFlags: List<Boolean>,
    dark: Boolean,
    modifier: Modifier = Modifier,
    accent: Color = AppColors.Orange
) {
    if (values.isEmpty()) return
    val peak = values.maxOf { Math.abs(it) }.coerceAtLeast(0.0)
    var top = Math.max(1.0, Math.ceil(peak))
    if (top > 3.0) top = Math.ceil(top / 3.0) * 3.0
    val lines = if (top <= 1.0) 1 else 3
    val gridColor = lineColor(dark)
    val negativeColor = AppColors.Danger
    val signed = values.any { it < 0 }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().height(150.dp)) {
            Column(
                modifier = Modifier.width(28.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                (lines downTo 0).forEach { index ->
                    Text(
                        formatNumber(if (signed) top * (2.0 * index / lines - 1) else top * index / lines),
                        fontSize = 10.sp,
                        color = AppColors.Muted
                    )
                }
            }
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    for (index in 0..lines) {
                        val y = height - height * index / lines
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1f
                        )
                    }
                    val slot = width / values.size
                    val barWidth = minOf(slot * 0.56f, 24.dp.toPx())
                    values.forEachIndexed { index, value ->
                        if (value != 0.0) {
                            val barHeight = (Math.abs(value) / top * height / (if (signed) 2 else 1)).toFloat().coerceAtMost(height)
                            drawRoundRect(
                                color = if (value < 0) negativeColor else accent,
                                topLeft = Offset(slot * index + (slot - barWidth) / 2, (if (signed) height / 2 else height) - (if (value < 0) 0f else barHeight)),
                                size = Size(barWidth, maxOf(barHeight, 2f)),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                        }
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(start = 28.dp)) {
            values.forEachIndexed { index, _ ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (index < tickFlags.size && tickFlags[index]) {
                        Text(
                            if (index < ticks.size) ticks[index] else "",
                            fontSize = 10.sp,
                            color = AppColors.Muted
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PieChart(
    slices: List<PieSlice>,
    dark: Boolean,
    modifier: Modifier = Modifier
) {
    val total = slices.sumOf { it.value }
    if (total <= 0.0) {
        Box(modifier = modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
            Text("该周期暂无计数数据", fontSize = 13.sp, color = AppColors.Muted)
        }
        return
    }
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(132.dp)) {
            var start = -90f
            slices.forEach { slice ->
                val sweep = (slice.value / total * 360.0).toFloat()
                drawArc(
                    color = slice.color,
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = true
                )
                start += sweep
            }
        }
        Spacer(modifier = Modifier.width(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            slices.forEach { slice ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(slice.color)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "${slice.label}  ${formatNumber(slice.value)}  ${Math.round(slice.value / total * 100)}%",
                        fontSize = 12.sp,
                        color = textColor(dark)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
fun CalendarView(
    period: Period,
    entries: List<EntryEntity>,
    monday: Boolean,
    dark: Boolean,
    onDate: (String) -> Unit = {}
) {
    val yearMonth = period.calendarMonth
    val first = yearMonth.atDay(1)
    val lead = if (monday) first.dayOfWeek.value - 1 else first.dayOfWeek.value % 7
    val days = yearMonth.lengthOfMonth()
    val byDay = HashMap<String, Double>()
    entries.forEach { entry ->
        byDay[entry.localDate] = (byDay[entry.localDate] ?: 0.0) + entry.delta
    }
    val max = byDay.values.maxOfOrNull { Math.abs(it) } ?: 1.0
    val labels = if (monday) arrayOf("一", "二", "三", "四", "五", "六", "日")
    else arrayOf("日", "一", "二", "三", "四", "五", "六")

    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Text(
            "${yearMonth.year}年${yearMonth.monthValue}月 · 每日合计",
            fontSize = 12.sp,
            color = AppColors.Muted
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            labels.forEach { label ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(label, fontSize = 11.sp, color = AppColors.Muted)
                }
            }
        }
        val cells = lead + days
        val rows = (cells + 6) / 7
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 5.dp)) {
                for (column in 0..6) {
                    val index = row * 7 + column - lead
                    val date: LocalDate? =
                        if (index in 0 until days) yearMonth.atDay(index + 1) else null
                    if (date == null) {
                        Box(modifier = Modifier.weight(1f).height(44.dp))
                    } else {
                        val key = com.example.counter.domain.TimeUtils.ymd(date)
                        val value = byDay[key] ?: 0.0
                        val alpha = if (value != 0.0) {
                            (0.42f + 0.58f * (Math.abs(value) / max).toFloat()).coerceIn(0f, 1f)
                        } else 0f
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clickable { onDate(key) }
                                .padding(2.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(
                                    if (value != 0.0) AppColors.Orange.copy(alpha = alpha)
                                    else chipColor(dark)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    date.dayOfMonth.toString(),
                                    fontSize = 11.sp,
                                    color = if (value != 0.0) Color.White else AppColors.Muted
                                )
                                if (value != 0.0) {
                                    Text(
                                        formatNumber(value),
                                        fontSize = 11.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
