package com.example.counter.ui
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.counter.domain.TimeUtils

@Composable
fun RecordFilters(vm: CounterViewModel, ui: UiState) {
    val context = LocalContext.current
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        FilterChip(selected = ui.counterFilter == null, onClick = { vm.setCounterFilter(null) }, label = { Text("全部计数器") })
        ui.counters.forEach { counter ->
            Spacer(Modifier.width(6.dp))
            FilterChip(selected = ui.counterFilter == counter.id, onClick = { vm.setCounterFilter(counter.id) }, label = { Text(counter.name) })
        }
    }
    Row {
        TextButton(onClick = { pickDate(context, TimeUtils.parse(ui.rangeStart ?: ui.selectedDate)) { d ->
            val start = TimeUtils.ymd(d); vm.setRange(start, ui.rangeEnd?.takeIf { it >= start } ?: start)
        } }) { Text(ui.rangeStart ?: "开始日期") }
        TextButton(onClick = { pickDate(context, TimeUtils.parse(ui.rangeEnd ?: ui.selectedDate)) { d ->
            val end = TimeUtils.ymd(d); vm.setRange(ui.rangeStart?.takeIf { it <= end } ?: end, end)
        } }) { Text(ui.rangeEnd ?: "结束日期") }
        if (ui.rangeStart != null) TextButton(onClick = { vm.setRange(null, null) }) { Text("清除") }
    }
}
