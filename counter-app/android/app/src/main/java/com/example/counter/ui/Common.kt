package com.example.counter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.counter.data.CounterEntity
import com.example.counter.data.CountMode
import com.example.counter.domain.TimeUtils
import java.time.LocalDate

fun formatNumber(value: Double): String {
    if (Math.abs(value - Math.round(value)) < 1e-9) return Math.round(value).toString()
    return (Math.round(value * 100.0) / 100.0).toString()
}

fun relativeLabel(minutes: Long): String = when {
    minutes < 3 -> "刚刚"
    minutes < 60 -> "${minutes}分钟前"
    minutes < 1440 -> "${minutes / 60}小时前"
    else -> "${minutes / 1440}天前"
}

/** Single source of truth for the home summary, shared by the inline and sticky variants. */
fun homeSummaryText(ui: UiState, counters: List<CounterEntity>): String {
    val mode = ui.settings.countMode
    val ids = counters.map { it.id }.toSet()
    val scoped = ui.entries.filter { entry ->
        entry.counterId in ids &&
            if (mode == CountMode.DAILY) entry.localDate == ui.selectedDate
            else entry.localDate <= ui.selectedDate
    }
    val total = scoped.sumOf { it.delta } +
        if (mode == CountMode.TOTAL) counters.sumOf { it.initialValue } else 0.0
    val average = if (counters.isEmpty()) 0.0 else total / counters.size
    val label = if (mode == CountMode.TOTAL) "累计" else "总计"
    return "$label：${formatNumber(total)}，项均：${formatNumber(average)}"
}

@Composable
fun CircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
fun TopBar(
    leading: @Composable () -> Unit,
    center: @Composable () -> Unit,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading()
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { center() }
        trailing()
    }
}

@Composable
fun TopTitle(title: String, subtitle: String, dark: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = textColor(dark))
        Text(subtitle, fontSize = 11.sp, color = AppColors.Muted)
    }
}

@Composable
fun WeekStrip(
    selectedDate: String,
    monday: Boolean,
    dark: Boolean,
    onSelect: (String) -> Unit,
    onShift: (Int) -> Unit
) {
    val base = TimeUtils.parse(selectedDate)
    val first = TimeUtils.weekStart(base, monday)
    val today = TimeUtils.todayYmd()
    val labels = arrayOf("日", "一", "二", "三", "四", "五", "六")
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.width(18.dp).height(40.dp).clickable { onShift(-1) },
            contentAlignment = Alignment.Center
        ) { Text("‹", fontSize = 18.sp, color = AppColors.Muted) }
        (0..6).forEach { index ->
            val date: LocalDate = first.plusDays(index.toLong())
            val key = TimeUtils.ymd(date)
            val selected = key == selectedDate
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp)
                    .padding(horizontal = 3.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(cardBackground(dark))
                    .then(
                        if (selected) Modifier.border(1.5.dp, AppColors.Orange, RoundedCornerShape(15.dp))
                        else Modifier
                    )
                    .clickable { onSelect(key) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(labels[date.dayOfWeek.value % 7], fontSize = 11.sp, color = AppColors.Muted)
                Text(
                    date.dayOfMonth.toString(),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        selected -> AppColors.OrangeDark
                        key == today -> AppColors.Blue
                        else -> textColor(dark)
                    }
                )
                if (selected) {
                    Box(
                        modifier = Modifier
                            .padding(top = 3.dp)
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(AppColors.Orange)
                    )
                }
            }
        }
        Box(
            modifier = Modifier.width(18.dp).height(40.dp).clickable { onShift(1) },
            contentAlignment = Alignment.Center
        ) { Text("›", fontSize = 18.sp, color = AppColors.Muted) }
    }
}

@Composable
fun CounterCard(
    counter: CounterEntity,
    value: Double,
    subtitle: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onOpen: () -> Unit,
    onEdit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(118.dp)
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(counter.color.toInt()))
            .clickable(onClick = onOpen)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(52.dp).clip(CircleShape).clickable(onClick = onMinus),
                contentAlignment = Alignment.Center
            ) { Text("−", fontSize = 30.sp, color = Color.White) }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (counter.icon.isBlank()) counter.name else "${counter.icon} ${counter.name}",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.95f)
                )
                Text(formatNumber(value), fontSize = 40.sp, color = Color.White)
                Text(subtitle, fontSize = 11.sp, color = Color.White.copy(alpha = 0.82f))
                if (counter.target > 0.0) {
                    val progress = (value / counter.target).coerceIn(0.0, 1.0).toFloat()
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .fillMaxWidth(0.7f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.28f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.White)
                        )
                    }
                }
            }
            Box(
                modifier = Modifier.size(52.dp).clip(CircleShape).clickable(onClick = onPlus),
                contentAlignment = Alignment.Center
            ) { Text("+", fontSize = 30.sp, color = Color.White) }
        }
        // Discoverable entry point for editing / deleting this counter.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 7.dp, end = 9.dp)
                .size(30.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.24f))
                .clickable(onClick = onEdit),
            contentAlignment = Alignment.Center
        ) {
            Text("✎", fontSize = 15.sp, color = Color.White)
        }
    }
}

@Composable
fun <T> SegmentControl(
    items: List<Pair<T, String>>,
    selected: T,
    dark: Boolean,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(chipColor(dark))
            .padding(3.dp)
    ) {
        items.forEach { (value, label) ->
            val active = value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (active) cardBackground(dark) else Color.Transparent)
                    .clickable { onSelect(value) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontSize = 13.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (active) textColor(dark) else AppColors.Muted
                )
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    dark: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(cardBackground(dark))
            .padding(14.dp)
            .height(76.dp)
    ) {
        Text(title, fontSize = 12.sp, color = AppColors.Muted)
        Spacer(modifier = Modifier.height(6.dp))
        Text(value, fontSize = 25.sp, fontWeight = FontWeight.SemiBold, color = textColor(dark))
    }
}

@Composable
private fun RowScope.TabItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    dark: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(27.dp))
            .background(if (selected) chipColor(dark) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (selected) AppColors.Blue else AppColors.Muted,
                modifier = Modifier.size(20.dp)
            )
            Text(
                label,
                fontSize = 10.sp,
                color = if (selected) AppColors.Blue else AppColors.Muted
            )
        }
    }
}

@Composable
fun BottomTabs(tab: Tab, dark: Boolean, onSelect: (Tab) -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = cardBackground(dark),
            shadowElevation = 6.dp,
            modifier = Modifier.width(300.dp).height(62.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TabItem("首页", Icons.Default.Home, tab == Tab.HOME, dark) { onSelect(Tab.HOME) }
                TabItem("历史记录", Icons.Default.History, tab == Tab.HISTORY, dark) { onSelect(Tab.HISTORY) }
                TabItem("统计", Icons.Default.BarChart, tab == Tab.STATS, dark) { onSelect(Tab.STATS) }
            }
        }
    }
}
