package com.example.counter.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.counter.domain.TimeUtils
import java.nio.charset.StandardCharsets

private sealed class ScreenDialog {
    object None : ScreenDialog()
    object AddCounter : ScreenDialog()
    data class EditCounter(val id: String) : ScreenDialog()
    data class QuickLog(val id: String) : ScreenDialog()
    data class EditEntry(val id: String) : ScreenDialog()
    object Settings : ScreenDialog()
    object More : ScreenDialog()
    object Categories : ScreenDialog()
    object DateJump : ScreenDialog()
    object PeriodPick : ScreenDialog()
    object ConfirmClear : ScreenDialog()
}

@Composable
fun CounterRoot(vm: CounterViewModel) {
    val ui by vm.ui.collectAsState()
    val dark = ui.settings.darkTheme
    val context = LocalContext.current
    var dialog by remember { mutableStateOf<ScreenDialog>(ScreenDialog.None) }
    val close: () -> Unit = { dialog = ScreenDialog.None }

    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(vm.exportCsv().toByteArray(StandardCharsets.UTF_8))
                }
                Toast.makeText(context, "CSV 已导出", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "导出失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val jsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(vm.exportJson().toByteArray(StandardCharsets.UTF_8))
                }
                Toast.makeText(context, "备份已导出", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "导出失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()
                    ?.use { reader -> reader.readText() }
                if (text != null && vm.importJson(text)) {
                    Toast.makeText(context, "备份已恢复", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "文件格式不正确", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "导入失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val exportCsv = { csvLauncher.launch("counter_${TimeUtils.todayYmd()}.csv") }
    val exportJson = { jsonLauncher.launch("counter_backup_${TimeUtils.todayYmd()}.json") }
    val importJson = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }

    Box(modifier = Modifier.fillMaxSize().background(pageBackground(dark))) {
        when (ui.tab) {
            Tab.HOME -> HomeScreen(
                vm = vm,
                ui = ui,
                onOpenMore = { dialog = ScreenDialog.More },
                onOpenSettings = { dialog = ScreenDialog.Settings },
                onOpenCategories = { dialog = ScreenDialog.Categories },
                onAddCounter = { dialog = ScreenDialog.AddCounter },
                onQuickLog = { dialog = ScreenDialog.QuickLog(it.id) }
            )

            Tab.HISTORY -> HistoryScreen(
                vm = vm,
                ui = ui,
                onOpenMore = { dialog = ScreenDialog.More },
                onOpenCategories = { dialog = ScreenDialog.Categories },
                onOpenDateJump = { dialog = ScreenDialog.DateJump },
                onEditEntry = { dialog = ScreenDialog.EditEntry(it.id) }
            )

            Tab.STATS -> StatsScreen(
                vm = vm,
                ui = ui,
                onOpenMore = { dialog = ScreenDialog.More },
                onOpenCategories = { dialog = ScreenDialog.Categories },
                onPeriodPick = { dialog = ScreenDialog.PeriodPick }
            )
        }

        if (ui.tab == Tab.HOME) {
            FloatingActionButton(
                onClick = { dialog = ScreenDialog.AddCounter },
                shape = CircleShape,
                containerColor = cardBackground(dark),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 22.dp, bottom = 104.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "新增计数器", tint = textColor(dark))
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 22.dp)
        ) {
            BottomTabs(tab = ui.tab, dark = dark, onSelect = vm::setTab)
        }
    }

    when (val current = dialog) {
        ScreenDialog.None -> Unit

        ScreenDialog.AddCounter -> CounterEditorDialog(
            counter = null,
            categories = ui.categories,
            dark = dark,
            onDismiss = close,
            onSave = { vm.saveCounter(it); close() },
            onDelete = { }
        )

        is ScreenDialog.EditCounter -> {
            val counter = ui.counters.firstOrNull { it.id == current.id }
            if (counter != null) {
                CounterEditorDialog(
                    counter = counter,
                    categories = ui.categories,
                    dark = dark,
                    onDismiss = close,
                    onSave = { vm.saveCounter(it); close() },
                    onDelete = { id -> vm.deleteCounter(id); close() }
                )
            }
        }

        is ScreenDialog.QuickLog -> {
            val counter = ui.counters.firstOrNull { it.id == current.id }
            if (counter != null) {
                QuickLogDialog(
                    counter = counter,
                    initialDate = ui.selectedDate,
                    onDismiss = close,
                    onSave = { delta, date, hour, minute, note ->
                        vm.addCustomEntry(counter.id, delta, date, hour, minute, note)
                        close()
                    }
                )
            }
        }

        is ScreenDialog.EditEntry -> {
            val entry = ui.entries.firstOrNull { it.id == current.id }
            if (entry != null) {
                EntryEditorDialog(
                    entry = entry,
                    counterName = ui.counters.firstOrNull { it.id == entry.counterId }?.name ?: "已删除",
                    initialDate = entry.localDate,
                    onDismiss = close,
                    onSave = { delta, date, hour, minute, note ->
                        vm.updateEntry(entry, delta, date, hour, minute, note)
                        close()
                    },
                    onDelete = { vm.deleteEntry(entry); close() }
                )
            }
        }

        ScreenDialog.Settings -> SettingsDialog(
            vm = vm,
            ui = ui,
            onDismiss = close,
            onExportCsv = exportCsv,
            onExportJson = exportJson,
            onImportJson = importJson,
            onClearAll = { dialog = ScreenDialog.ConfirmClear }
        )

        ScreenDialog.More -> MoreDialog(
            onDismiss = close,
            onUndo = { vm.undo(); close() },
            onDateJump = { dialog = ScreenDialog.DateJump },
            onCategories = { dialog = ScreenDialog.Categories },
            onExportCsv = exportCsv,
            onExportJson = exportJson,
            onSettings = { dialog = ScreenDialog.Settings }
        )

        ScreenDialog.Categories -> CategoryDialog(
            categories = ui.categories,
            current = ui.filterCategory,
            onDismiss = close,
            onPick = { vm.setFilter(it); close() },
            onAdd = { vm.addCategory(it) }
        )

        ScreenDialog.DateJump -> DateJumpDialog(
            initialDate = ui.selectedDate,
            onDismiss = close,
            onPick = { date ->
                vm.setDate(date)
                vm.setAnchor(TimeUtils.parse(date))
            }
        )

        ScreenDialog.PeriodPick -> PeriodPickDialog(
            anchor = ui.anchor,
            onDismiss = close,
            onPick = { vm.setAnchor(it) }
        )

        ScreenDialog.ConfirmClear -> ConfirmClearDialog(
            onDismiss = close,
            onConfirm = { vm.clearAll(); close() }
        )
    }
}
