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
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
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
    val lines = if (values.any { it < 0 }) 4 else if (top <= 1.0) 1 else 3
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
                            fontSize = 10.sp, maxLines = 1, softWrap = false,
                            modifier = Modifier.requiredWidth(28.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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

/** A bounded monthly heatmap: cell size does not expand with the window width. */
@Composable
fun CalendarView(
    period: Period, entries: List<EntryEntity>, monday: Boolean, dark: Boolean,
    onDate: (String) -> Unit = {}
) {
    val month = period.calendarMonth
    val grouped = entries.filter { it.localDate.startsWith(month.toString() + "-") }.groupBy { it.localDate }
    val values = grouped.mapValues { (_, rows) -> rows.sumOf { it.delta } }
    val peak = values.values.maxOfOrNull { kotlin.math.abs(it) }?.coerceAtLeast(1.0) ?: 1.0
    var selected by remember(month) { mutableStateOf(grouped.keys.maxOrNull()
        ?: (if (java.time.YearMonth.from(LocalDate.now()) == month) LocalDate.now() else month.atDay(1)).toString()) }
    val selectedRows = grouped[selected].orEmpty()
    val selectedValue = values[selected] ?: 0.0
    val date = LocalDate.parse(selected)
    val labels = if (monday) listOf("一", "二", "三", "四", "五", "六", "日") else listOf("日", "一", "二", "三", "四", "五", "六")
    val lead = if (monday) month.atDay(1).dayOfWeek.value - 1 else month.atDay(1).dayOfWeek.value % 7
    val emptyColor = if (dark) Color(0xFF363638) else Color(0xFFF0F0EC)
    val warm = if (dark) listOf(0xFF594129, 0xFF855825, 0xFFBF7829, 0xFFF1A343) else listOf(0xFFFFE8C9, 0xFFFAC77E, 0xFFF09B35, 0xFFBC640B)
    val cool = if (dark) listOf(0xFF293E52, 0xFF365F82, 0xFF4784AD, 0xFF72B5DF) else listOf(0xFFDFECF5, 0xFFACCCE2, 0xFF6DA5CD, 0xFF306E9C)
    fun color(value: Double): Color {
        if (value == 0.0) return emptyColor
        val level = kotlin.math.ceil(kotlin.math.abs(value) / peak * 4).toInt().coerceIn(1, 4) - 1
        return Color((if (value < 0) cool else warm)[level])
    }
    val grid: @Composable () -> Unit = {
        Column(Modifier.widthIn(max = 336.dp).fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                labels.forEach { Text(it, modifier = Modifier.weight(1f), fontSize = 11.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = AppColors.Muted) }
            }
            repeat((lead + month.lengthOfMonth() + 6) / 7) { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                    repeat(7) { column ->
                        val day = row * 7 + column - lead + 1
                        if (day !in 1..month.lengthOfMonth()) {
                            Spacer(Modifier.weight(1f).aspectRatio(1f))
                        } else {
                            val key = month.atDay(day).toString()
                            val value = values[key] ?: 0.0
                            val count = grouped[key]?.size ?: 0
                            val strong = kotlin.math.abs(value) / peak > 0.5 && value != 0.0
                            Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp)
                                .testTag("heatmap-$key")
                                .semantics { contentDescription = "$key，净增量 ${formatNumber(value)}，$count 条记录" }
                                .clip(RoundedCornerShape(8.dp))
                                .background(color(value))
                                .then(if (key == selected) Modifier.border(2.dp, if (dark) Color(0xFFFFC782) else Color(0xFF804600), RoundedCornerShape(8.dp)) else Modifier)
                                .clickable { selected = key }, contentAlignment = Alignment.Center) {
                                Text(day.toString(), fontSize = 12.sp, maxLines = 1,
                                    fontWeight = if (count > 0) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal,
                                    color = if (strong && !dark) Color.White else textColor(dark))
                                if (key == LocalDate.now().toString()) Box(Modifier.align(Alignment.BottomCenter)
                                    .padding(bottom = 4.dp).size(3.dp).background(if (strong && !dark) Color.White else textColor(dark), androidx.compose.foundation.shape.CircleShape))
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("少", fontSize = 11.sp, color = AppColors.Muted)
                (listOf(emptyColor) + warm.map { Color(it) }).forEach { shade ->
                    Box(Modifier.padding(start = 4.dp).size(12.dp).clip(RoundedCornerShape(3.dp)).background(shade))
                }
                Text("  多", fontSize = 11.sp, color = AppColors.Muted)
                Spacer(Modifier.weight(1f))
                Text("蓝色为净减少", fontSize = 10.sp, color = AppColors.Muted)
            }
        }
    }
    val detail: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(chipColor(dark)).padding(16.dp)) {
            Text("${date.monthValue}月${date.dayOfMonth}日 · 周${listOf("日", "一", "二", "三", "四", "五", "六")[date.dayOfWeek.value % 7]}",
                fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, color = textColor(dark))
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.Bottom) {
                Text(formatNumber(selectedValue), fontSize = 32.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = if (selectedValue < 0) Color(0xFF4C93C2) else if (dark) Color(0xFFFFB85C) else AppColors.OrangeDark,
                    modifier = Modifier.testTag("heatmap-selected-value"))
                Text("  净增量 · ${selectedRows.size} 条记录", fontSize = 12.sp, color = AppColors.Muted, modifier = Modifier.padding(bottom = 5.dp))
            }
            Text(if (selectedRows.isEmpty()) "当天暂无记录" else "增加 ${formatNumber(selectedRows.filter { it.delta > 0 }.sumOf { it.delta })}  ·  减少 ${formatNumber(-selectedRows.filter { it.delta < 0 }.sumOf { it.delta })}",
                fontSize = 12.sp, color = AppColors.Muted, modifier = Modifier.padding(top = 6.dp))
            androidx.compose.material3.TextButton(onClick = { onDate(selected) }, modifier = Modifier.testTag("heatmap-open-history")) {
                Text("查看当天记录  →")
            }
        }
    }
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text("${month.year}年${month.monthValue}月 · 颜色越深，当日净变化越多", fontSize = 12.sp, color = AppColors.Muted)
        Spacer(Modifier.height(16.dp))
        BoxWithConstraints {
            if (maxWidth >= 600.dp) Row(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(336.dp)) { grid() }
                Box(Modifier.widthIn(max = 360.dp).weight(1f)) { detail() }
            } else Column { grid(); Spacer(Modifier.height(16.dp)); detail() }
        }
    }
}
