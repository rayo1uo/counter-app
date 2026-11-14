package com.example.counter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.counter.data.CounterEntity
import com.example.counter.domain.StatsCalculator
import com.example.counter.domain.TimeUtils

@Composable
fun HomeScreen(
    vm: CounterViewModel,
    ui: UiState,
    onOpenMore: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCategories: () -> Unit,
    onEditCounter: (CounterEntity) -> Unit,
    onQuickLog: (CounterEntity) -> Unit,
    inlineSummary: Boolean = true
) {
    val dark = ui.settings.darkTheme
    val counters = vm.visibleCounters()
    val filterLabel = if (ui.filterCategory == "all") "全部" else ui.filterCategory

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, bottom = 16.dp)
    ) {
        TopBar(
            leading = {
                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(cardBackground(dark))
                        .clickable(onClick = onOpenCategories)
                        .padding(horizontal = 15.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("‹ $filterLabel", fontSize = 14.sp, color = textColor(dark))
                }
            },
            center = { },
            trailing = {
                Row {
                    CircleButton(onClick = onOpenMore) {
                        Icon(
                            Icons.Default.MoreHoriz,
                            contentDescription = "更多",
                            tint = textColor(dark),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    CircleButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "设置",
                            tint = textColor(dark),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(cardBackground(dark))
                .padding(horizontal = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = AppColors.Muted,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(9.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (ui.search.isEmpty()) {
                    Text("搜索", fontSize = 14.sp, color = AppColors.Muted)
                }
                BasicTextField(
                    value = ui.search,
                    onValueChange = vm::setSearch,
                    singleLine = true,
                    textStyle = TextStyle(color = textColor(dark), fontSize = 14.sp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        WeekStrip(
            selectedDate = ui.selectedDate,
            monday = ui.settings.weekStartMonday,
            dark = dark,
            onSelect = { date ->
                vm.setDate(date)
                vm.setAnchor(TimeUtils.parse(date))
            },
            onShift = { direction ->
                vm.setDate(TimeUtils.ymd(TimeUtils.parse(ui.selectedDate).plusDays(direction * 7L)))
            }
        )

        counters.forEach { counter ->
            val value = StatsCalculator.dayValue(
                ui.entries, counter.id, ui.selectedDate,
                ui.settings.countMode, counter.initialValue
            )
            val last = StatsCalculator.lastOccurred(ui.entries, counter.id, ui.selectedDate)
            val subtitle = if (last == null) "暂无记录"
            else relativeLabel(StatsCalculator.minutesSince(last))
            CounterCard(
                counter = counter,
                value = value,
                subtitle = subtitle,
                onMinus = { vm.addCount(counter.id, -counter.step) },
                onPlus = { vm.addCount(counter.id, counter.step) },
                onOpen = { onQuickLog(counter) },
                onEdit = { onEditCounter(counter) }
            )
        }

        if (counters.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("还没有计数器，点右下角 + 新增一个", fontSize = 13.sp, color = AppColors.Muted)
            }
        }

        // On wide screens this is pinned next to the tab bar in CounterRoot instead.
        if (inlineSummary) {
            Text(
                homeSummaryText(ui, counters),
                fontSize = 13.sp,
                color = AppColors.Muted,
                modifier = Modifier.padding(top = 6.dp, start = 4.dp)
            )
            Text(
                "点卡片可补录，点卡片右上角 ✎ 可编辑或删除",
                fontSize = 11.sp,
                color = AppColors.Muted,
                modifier = Modifier.padding(top = 2.dp, start = 4.dp, bottom = 2.dp)
            )
        }
    }
}
