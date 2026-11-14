package com.example.counter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.counter.data.EntryEntity
import com.example.counter.domain.TimeUtils

@Composable
fun HistoryScreen(
    vm: CounterViewModel,
    ui: UiState,
    onOpenMore: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenDateJump: () -> Unit,
    onEditEntry: (EntryEntity) -> Unit
) {
    val dark = ui.settings.darkTheme
    val names = ui.counters.associateBy { it.id }
    val ids = vm.visibleCounters().map { it.id }.toSet()
    val entries = ui.entries.filter { entry ->
        entry.counterId in ids && (ui.rangeStart == null || entry.localDate >= ui.rangeStart) && (ui.rangeEnd == null || entry.localDate <= ui.rangeEnd) &&
            (ui.search.isBlank() || (names[entry.counterId]?.name ?: "").contains(ui.search, ignoreCase = true))
    }.sortedWith(
        compareByDescending<EntryEntity> { it.occurredAt }
            .thenByDescending { it.createdAt }
    )
    var collapsed by remember { mutableStateOf(setOf<String>()) }
    val groups = entries.groupBy { it.localDate }.toList().sortedByDescending { it.first }
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
            center = { TopTitle("历史记录", filterLabel, dark) },
            trailing = {
                Row {
                    CircleButton(onClick = onOpenDateJump) {
                        Icon(Icons.Default.DateRange, contentDescription = "跳转日期", tint = textColor(dark))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    CircleButton(onClick = onOpenMore) {
                        Icon(Icons.Default.MoreHoriz, contentDescription = "更多", tint = textColor(dark))
                    }
                }
            }
        )

        RecordFilters(vm, ui)
        Spacer(modifier = Modifier.height(14.dp))

        if (groups.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无历史记录\n在首页点 + 开始计数", fontSize = 13.sp, color = AppColors.Muted)
            }
        }

        groups.forEach { (date, list) ->
            val localDate = TimeUtils.parse(date)
            val weekLabels = arrayOf("日", "一", "二", "三", "四", "五", "六")
            Row(modifier = Modifier.fillMaxWidth().clickable { collapsed = if (date in collapsed) collapsed - date else collapsed + date }, verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(cardBackground(dark)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            localDate.dayOfMonth.toString(),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.OrangeDark
                        )
                        Text("${localDate.monthValue}月", fontSize = 10.sp, color = AppColors.Muted)
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    "周${weekLabels[localDate.dayOfWeek.value % 7]} · ${list.size} 条记录  ${if (date in collapsed) "展开" else "收起"}",
                    fontSize = 14.sp,
                    color = AppColors.Muted
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (date !in collapsed) list.forEachIndexed { index, entry ->
                val (hour24, minute) = TimeUtils.dateTimeParts(entry.occurredAt)
                val ampm = if (hour24 < 12) "上午" else "下午"
                val hour12 = if (hour24 % 12 == 0) 12 else hour24 % 12
                val gapMinutes = if (index < list.size - 1) {
                    (entry.occurredAt - list[index + 1].occurredAt) / 60_000L
                } else -1L

                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Column(
                        modifier = Modifier.width(58.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        if (gapMinutes > 0) {
                            val gapText = if (gapMinutes >= 60) {
                                "${gapMinutes / 60}时" + if (gapMinutes % 60 > 0) "${gapMinutes % 60}分" else ""
                            } else "${gapMinutes}分"
                            Text(gapText, fontSize = 10.sp, color = AppColors.Muted)
                            RecordFilters(vm, ui)
        Spacer(modifier = Modifier.height(14.dp))
                        }
                        Text(
                            String.format("%02d:%02d", hour24, minute),
                            fontSize = 11.sp,
                            color = AppColors.Muted
                        )

                    }

                    Box(
                        modifier = Modifier.width(18.dp).height(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(lineColor(dark))
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .width(7.dp)
                                .height(7.dp)
                                .background(lineColor(dark))
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(cardBackground(dark))
                            .clickable { onEditEntry(entry) }
                            .padding(14.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().clickable { collapsed = if (date in collapsed) collapsed - date else collapsed + date }, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    names[entry.counterId]?.name ?: "已删除",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textColor(dark)
                                )
                                if (entry.note.isNotBlank()) {
                                    Text(entry.note, fontSize = 11.sp, color = AppColors.Muted)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AppColors.OrangeSoft)
                                    .padding(horizontal = 11.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    (if (entry.delta > 0) "+" else "") + formatNumber(entry.delta),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (entry.delta < 0) AppColors.Danger else AppColors.OrangeDark
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "→ ${formatNumber(entry.valueAfter)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.OrangeDark
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        Text(
            "点击记录可编辑或删除",
            fontSize = 11.sp,
            color = AppColors.Muted,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
        )
    }
}
