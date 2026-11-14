package com.example.counter.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.example.counter.data.CategoryEntity
import com.example.counter.data.CounterEntity
import com.example.counter.data.CountMode
import com.example.counter.data.EntryEntity
import com.example.counter.domain.TimeUtils
import java.time.LocalDate
import java.util.UUID

internal fun pickDate(context: Context, initial: LocalDate, onPick: (LocalDate) -> Unit) {
    DatePickerDialog(
        context,
        { _, year, month, day -> onPick(LocalDate.of(year, month + 1, day)) },
        initial.year, initial.monthValue - 1, initial.dayOfMonth
    ).show()
}

private fun pickTime(context: Context, hour: Int, minute: Int, onPick: (Int, Int) -> Unit) {
    TimePickerDialog(context, { _, h, m -> onPick(h, m) }, hour, minute, true).show()
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    numeric: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text
        ),
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
    )
}

@Composable
fun CounterEditorDialog(
    counter: CounterEntity?,
    categories: List<CategoryEntity>,
    dark: Boolean,
    onDismiss: () -> Unit,
    onSave: (CounterEntity) -> Unit,
    onDelete: (String) -> Unit
) {
    var name by remember { mutableStateOf(counter?.name ?: "") }
    var icon by remember { mutableStateOf(counter?.icon ?: "") }
    var step by remember { mutableStateOf(formatNumber(counter?.step ?: 1.0)) }
    var initial by remember { mutableStateOf(formatNumber(counter?.initialValue ?: 0.0)) }
    var target by remember { mutableStateOf(formatNumber(counter?.target ?: 0.0)) }
    var category by remember { mutableStateOf(counter?.category ?: (categories.firstOrNull()?.name ?: "默认")) }
    var color by remember { mutableStateOf(counter?.color ?: 0xFFF5921E) }
    var advanced by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (counter == null) "新增计数器" else "编辑计数器") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                FormField("名称", name, { name = it })
                FormField("图标（可选 emoji）", icon, { icon = it })
                TextButton(onClick = { advanced = !advanced }) { Text(if (advanced) "收起高级设置" else "高级设置 · 增量 / 初始值 / 目标") }
                if (advanced) {
                Row {
                    Box(Modifier.weight(1f)) { FormField("每次增量", step, { step = it }, true) }
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.weight(1f)) { FormField("初始值", initial, { initial = it }, true) }
                }
                FormField("目标值（0 为不设）", target, { target = it }, true)
                }
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    (categories.map { it.name } + "默认").distinct().forEach { label ->
                        TextButton(onClick = { category = label }) { Text(label + if (category == label) " ✓" else "") }
                    }
                }
                FormField("分类（可填新名称）", category, { category = it })
                Text(
                    "已有分类：${categories.joinToString("、") { it.name }}",
                    fontSize = 11.sp,
                    color = AppColors.Muted,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                Text("预设颜色", fontSize = 12.sp, color = AppColors.Muted)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    AppColors.PaletteArgb.forEach { argb ->
                        val selected = argb == color
                        Box(
                            modifier = Modifier
                                .padding(end = 9.dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(argb.toInt()))
                                .then(
                                    if (selected) Modifier.border(2.dp, textColor(dark), CircleShape)
                                    else Modifier
                                )
                                .clickable { color = argb }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (error.isNotEmpty()) Text(error, fontSize = 12.sp, color = AppColors.Danger)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) { error = "请填写名称"; return@TextButton }
                val numbers = listOf(step.toDoubleOrNull(), initial.toDoubleOrNull(), target.toDoubleOrNull())
                if (numbers.any { it == null || !it.isFinite() } || numbers[0]!! <= 0 || numbers[2]!! < 0) { error = "请输入有效数字，增量须大于 0，目标不能为负数"; return@TextButton }
                val result = CounterEntity(
                    id = counter?.id ?: UUID.randomUUID().toString(),
                    name = name.trim(),
                    color = color,
                    icon = icon.trim(),
                    step = step.toDoubleOrNull() ?: 1.0,
                    initialValue = initial.toDoubleOrNull() ?: 0.0,
                    target = target.toDoubleOrNull() ?: 0.0,
                    category = category.trim().ifBlank { "默认" },
                    pinned = counter?.pinned ?: false,
                    sortOrder = counter?.sortOrder ?: 0,
                    archived = counter?.archived ?: false,
                    createdAt = counter?.createdAt ?: System.currentTimeMillis()
                )
                onSave(result)
            }) { Text("保存") }
        },
        dismissButton = {
            Row {
                if (counter != null) {
                    TextButton(onClick = { onDelete(counter.id) }) {
                        Text("删除", color = AppColors.Danger)
                    }
                }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        }
    )
}

@Composable
fun QuickLogDialog(
    counter: CounterEntity,
    initialDate: String,
    onDismiss: () -> Unit,
    onSave: (Double, LocalDate, Int, Int, String) -> Unit
) {
    val context = LocalContext.current
    var delta by remember { mutableStateOf(formatNumber(counter.step)) }
    var date by remember { mutableStateOf(TimeUtils.parse(initialDate)) }
    val parts = TimeUtils.dateTimeParts(System.currentTimeMillis())
    var hour by remember { mutableStateOf(parts.first) }
    var minute by remember { mutableStateOf(parts.second) }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("记录 · ${counter.name}") },
        text = {
            Column {
                FormField("本次增量（可填负数）", delta, { delta = it }, true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("日期", fontSize = 13.sp, color = AppColors.Muted)
                    Spacer(Modifier.width(12.dp))
                    TextButton(onClick = { pickDate(context, date) { date = it } }) {
                        Text(TimeUtils.ymd(date))
                    }
                    TextButton(onClick = { pickTime(context, hour, minute) { h, m -> hour = h; minute = m } }) {
                        Text(String.format("%02d:%02d", hour, minute))
                    }
                }
                Text("可补录到过去日期，统计会自动归入对应周期", fontSize = 11.sp, color = AppColors.Muted)
                FormField("备注（可选）", note, { note = it })
                if (error.isNotEmpty()) Text(error, fontSize = 12.sp, color = AppColors.Danger)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val value = delta.toDoubleOrNull()
                if (value == null || !value.isFinite() || value == 0.0) { error = "增量不能为 0"; return@TextButton }
                onSave(value, date, hour, minute, note.trim())
            }) { Text("记录") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun EntryEditorDialog(
    entry: EntryEntity,
    counterName: String,
    initialDate: String,
    onDismiss: () -> Unit,
    onSave: (Double, LocalDate, Int, Int, String) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var delta by remember { mutableStateOf(formatNumber(entry.delta)) }
    var date by remember { mutableStateOf(TimeUtils.parse(initialDate)) }
    val parts = TimeUtils.dateTimeParts(entry.occurredAt)
    var hour by remember { mutableStateOf(parts.first) }
    var minute by remember { mutableStateOf(parts.second) }
    var note by remember { mutableStateOf(entry.note) }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑记录 · $counterName") },
        text = {
            Column {
                FormField("增量", delta, { delta = it }, true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { pickDate(context, date) { date = it } }) {
                        Text(TimeUtils.ymd(date))
                    }
                    TextButton(onClick = { pickTime(context, hour, minute) { h, m -> hour = h; minute = m } }) {
                        Text(String.format("%02d:%02d", hour, minute))
                    }
                }
                FormField("备注", note, { note = it })
                if (error.isNotEmpty()) Text(error, fontSize = 12.sp, color = AppColors.Danger)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val value = delta.toDoubleOrNull()
                if (value == null || !value.isFinite() || value == 0.0) { error = "增量不能为 0"; return@TextButton }
                onSave(value, date, hour, minute, note.trim())
            }) { Text("保存修改") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("删除", color = AppColors.Danger) }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        }
    )
}

@Composable
private fun SettingRow(label: String, control: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(50.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp)
        control()
    }
}

@Composable
fun SettingsDialog(
    vm: CounterViewModel,
    ui: UiState,
    onDismiss: () -> Unit,
    onExportCsv: () -> Unit,
    onExportDailyCsv: () -> Unit,
    onExportJson: () -> Unit,
    onImportJson: () -> Unit,
    onClearAll: () -> Unit
) {
    val context = LocalContext.current
    val settings = ui.settings
    val volumeTarget = settings.volumeCounterId.ifEmpty { ui.counters.firstOrNull()?.id ?: "" }
    val volumeName = ui.counters.firstOrNull { it.id == volumeTarget }?.name ?: "第一个计数器"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                SettingRow("紧凑卡片") { Switch(settings.compact, vm::setCompact) }
                SettingRow("每次弹窗确认") { Switch(settings.confirmCount, vm::setConfirmCount) }
                SettingRow("深色模式") {
                    Switch(checked = settings.darkTheme, onCheckedChange = vm::setDark)
                }
                SettingRow("点击音效") {
                    Switch(checked = settings.sound, onCheckedChange = vm::setSound)
                }
                SettingRow("震动反馈") {
                    Switch(checked = settings.haptic, onCheckedChange = vm::setHaptic)
                }
                SettingRow("每周起始日") {
                    TextButton(onClick = { vm.setWeekStartMonday(!settings.weekStartMonday) }) {
                        Text(if (settings.weekStartMonday) "周一" else "周日")
                    }
                }
                SettingRow("计数模式") {
                    TextButton(onClick = {
                        vm.setCountMode(
                            if (settings.countMode == CountMode.DAILY) CountMode.TOTAL else CountMode.DAILY
                        )
                    }) { Text(if (settings.countMode == CountMode.DAILY) "每日重置" else "累计不清零") }
                }
                Text(
                    "每日重置：首页数字为所选日期当日合计；累计不清零：从初始值累加。切换后历史结果值会重算。",
                    fontSize = 11.sp,
                    color = AppColors.Muted
                )
                SettingRow("每日提醒") {
                    Switch(
                        checked = settings.reminderEnabled,
                        onCheckedChange = { vm.setReminder(it, settings.reminderHour, settings.reminderMinute) }
                    )
                }
                SettingRow("提醒时间") {
                    TextButton(onClick = {
                        pickTime(context, settings.reminderHour, settings.reminderMinute) { h, m ->
                            vm.setReminder(true, h, m)
                        }
                    }) {
                        Text(String.format("%02d:%02d", settings.reminderHour, settings.reminderMinute))
                    }
                }
                SettingRow("音量上键计数") {
                    Switch(
                        checked = settings.volumeCountEnabled,
                        onCheckedChange = { vm.setVolume(it, volumeTarget) }
                    )
                }
                Text("绑定：$volumeName（按音量上键即可 +1，不调节系统音量）", fontSize = 11.sp, color = AppColors.Muted)
                SettingRow("导出 CSV 流水") { TextButton(onClick = onExportCsv) { Text("导出") } }
                SettingRow("导出每日汇总 CSV") { TextButton(onClick = onExportDailyCsv) { Text("导出") } }
                Text("每次修改后自动保存本地备份；导出 JSON 可另存到其他设备。", fontSize = 11.sp, color = AppColors.Muted)
                SettingRow("恢复自动备份") { TextButton(onClick = { vm.restoreAutomatic { ok -> android.widget.Toast.makeText(context, if (ok) "已恢复自动备份" else "暂无可恢复的自动备份", android.widget.Toast.LENGTH_SHORT).show() } }) { Text("恢复") } }
                SettingRow("恢复删除 / 导入前数据") { TextButton(onClick = { vm.restoreAutomatic(true) { ok -> android.widget.Toast.makeText(context, if (ok) "已恢复操作前的数据" else "暂无操作前备份", android.widget.Toast.LENGTH_SHORT).show() } }) { Text("恢复") } }
                SettingRow("备份 JSON") { TextButton(onClick = onExportJson) { Text("备份") } }
                SettingRow("导入备份") { TextButton(onClick = onImportJson) { Text("导入") } }
                SettingRow("清空所有数据") {
                    TextButton(onClick = onClearAll) { Text("清空", color = AppColors.Danger) }
                }
                Text(
                    "桌面小组件：长按主屏幕添加「计数器」小组件，可一键给默认计数器 +1。\n所有数据仅存本机，无账号、无联网。",
                    fontSize = 11.sp,
                    color = AppColors.Muted,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

@Composable
fun MoreDialog(
    onDismiss: () -> Unit,
    onUndo: () -> Unit,
    onDateJump: () -> Unit,
    onCategories: () -> Unit,
    onExportCsv: () -> Unit,
    onExportJson: () -> Unit,
    onSettings: () -> Unit
) {
    val items = listOf(
        "撤销上一次计数" to onUndo,
        "跳转到日期" to onDateJump,
        "切换分类筛选" to onCategories,
        "导出 CSV" to onExportCsv,
        "备份 JSON" to onExportJson,
        "设置" to onSettings
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("更多") },
        text = {
            Column {
                items.forEach { (label, action) ->
                    TextButton(
                        onClick = action,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(label, modifier = Modifier.fillMaxWidth()) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

@Composable
fun CategoryDialog(
    categories: List<CategoryEntity>,
    current: String,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
    onAdd: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    var newName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("分类筛选") },
        text = {
            Column {
                TextButton(
                    onClick = { onPick("all") },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (current == "all") "全部  ✓" else "全部", modifier = Modifier.fillMaxWidth()) }
                categories.forEach { category ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = { onPick(category.name) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                if (current == category.name) "${category.name}  ✓" else category.name,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        TextButton(onClick = { onDelete(category.name) }) {
                            Text("删除", fontSize = 12.sp, color = AppColors.Danger)
                        }
                    }
                }
                FormField("新建分类", newName, { newName = it })
                Text(
                    "删除分类后，其下计数器会移到「默认」分类",
                    fontSize = 11.sp,
                    color = AppColors.Muted
                )
                TextButton(
                    onClick = {
                        if (newName.isNotBlank()) {
                            onAdd(newName.trim())
                            newName = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("＋ 添加分类", modifier = Modifier.fillMaxWidth()) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

@Composable
fun DateJumpDialog(
    initialDate: String,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("跳转到日期") },
        text = {
            Column {
                Text("当前：${TimeUtils.ymd(TimeUtils.parse(initialDate))}", fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = {
                    pickDate(context, TimeUtils.parse(initialDate)) { date ->
                        onPick(TimeUtils.ymd(date))
                        onDismiss()
                    }
                }) { Text("打开日历选择") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun PeriodPickDialog(
    anchor: LocalDate,
    onDismiss: () -> Unit,
    onPick: (LocalDate) -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择统计周期") },
        text = {
            Column {
                Text("当前：${anchor.year}年${anchor.monthValue}月", fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = {
                    pickDate(context, anchor) { date ->
                        onPick(date.withDayOfMonth(1))
                        onDismiss()
                    }
                }) { Text("打开日历选择年月") }
                Text("选中日期所在月份即为统计周期", fontSize = 11.sp, color = AppColors.Muted)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun ConfirmActionDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("删除", color = AppColors.Danger) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun ConfirmClearDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("清空所有数据") },
        text = { Text("所有计数器和历史记录都会被删除，且无法撤销。") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("确认清空", color = AppColors.Danger) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
