package com.example.counter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
    val label = if (mode == CountMode.TOTAL) "截至所选日期累计" else if (ui.selectedDate == TimeUtils.todayYmd()) "今日净增量" else "所选日期净增量"
    return "$label  ${formatNumber(total)}  ·  ${counters.size} 个计数器"
}

@Composable
fun CircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(48.dp)
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

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun CounterCard(
    counter: CounterEntity, value: Double, subtitle: String,
    onMinus: () -> Unit, onPlus: () -> Unit, onOpen: () -> Unit, onEdit: () -> Unit,
    dark: Boolean = false, compact: Boolean = true, cumulative: Double = value,
    onBatch: () -> Unit = onOpen, onPin: () -> Unit = {}, onMove: (Int) -> Unit = {}
) {
    var menu by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val accent = Color(counter.color.toInt())
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(20.dp))
        .background(cardBackground(dark)).height(if (compact) 112.dp else 174.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(5.dp).fillMaxHeight().background(accent))
        Column(Modifier.weight(1f).clickable(onClick = onOpen).padding(start = 16.dp, end = 6.dp)) {
            Text((if (counter.pinned) "置顶 · " else "") + counter.icon + " " + counter.name,
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = textColor(dark), maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(formatNumber(value), fontSize = if (compact) 30.sp else 48.sp,
                fontWeight = FontWeight.Bold, color = textColor(dark), maxLines = 1)
            Text("累计 ${formatNumber(cumulative)} · $subtitle", fontSize = 11.sp, color = AppColors.Muted, maxLines = 1)
            if (counter.target > 0) {
                androidx.compose.material3.LinearProgressIndicator(progress = (value / counter.target).coerceIn(0.0, 1.0).toFloat(),
                    modifier = Modifier.padding(top = 5.dp).fillMaxWidth().height(3.dp), color = accent)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(chipColor(dark)).clickable(onClick = onMinus), contentAlignment = Alignment.Center) {
                Text("−", fontSize = 26.sp, color = textColor(dark))
            }
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(48.dp).clip(CircleShape).background(accent)
                .combinedClickable(onClick = onPlus, onLongClick = onBatch), contentAlignment = Alignment.Center) {
                Text("+", fontSize = 28.sp, color = Color.White)
            }
            Box {
                androidx.compose.material3.IconButton(onClick = { menu = true }, modifier = Modifier.width(40.dp)) {
                    Text("⋮", fontSize = 24.sp, color = AppColors.Muted)
                }
                androidx.compose.material3.DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    listOf("编辑计数器" to onEdit, "批量 / 补录" to onBatch,
                        (if (counter.pinned) "取消置顶" else "置顶") to onPin,
                        "上移" to { onMove(-1) }, "下移" to { onMove(1) }).forEach { (label, action) ->
                        androidx.compose.material3.DropdownMenuItem(text = { Text(label) }, onClick = { menu = false; action() })
                    }
                }
            }
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
                tint = if (selected) AppColors.OrangeDark else AppColors.Muted,
                modifier = Modifier.size(20.dp)
            )
            Text(
                label,
                fontSize = 10.sp,
                color = if (selected) AppColors.OrangeDark else AppColors.Muted
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
