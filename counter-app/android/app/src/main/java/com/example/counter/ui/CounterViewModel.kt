package com.example.counter.ui

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.counter.CounterApp
import com.example.counter.data.AppSettings
import com.example.counter.data.CategoryEntity
import com.example.counter.data.CounterEntity
import com.example.counter.data.CountMode
import com.example.counter.data.EntryEntity
import com.example.counter.domain.Dim
import com.example.counter.domain.StatView
import com.example.counter.domain.TimeUtils
import com.example.counter.notify.ReminderScheduler
import com.example.counter.widget.WidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

enum class Tab { HOME, HISTORY, STATS }

data class UiState(
    val counters: List<CounterEntity> = emptyList(),
    val entries: List<EntryEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val tab: Tab = Tab.HOME,
    val selectedDate: String = TimeUtils.todayYmd(),
    val anchor: LocalDate = TimeUtils.today(),
    val dim: Dim = Dim.MONTH,
    val view: StatView = StatView.CHART,
    val filterCategory: String = "all",
    val search: String = "",
    val counterFilter: String? = null,
    val rangeStart: String? = null,
    val rangeEnd: String? = null
)

class CounterViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as CounterApp).container
    private val dao = container.dao
    private val repo = container.repository
    private val settingsRepo = container.settings

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    init {
        viewModelScope.launch {
            dao.observeCounters().collect { value -> _ui.value = _ui.value.copy(counters = value) }
        }
        viewModelScope.launch {
            dao.observeEntries().collect { value ->
                _ui.value = _ui.value.copy(entries = value)
                WidgetUpdater.updateAll(getApplication<Application>())
            }
        }
        viewModelScope.launch {
            dao.observeCategories().collect { value -> _ui.value = _ui.value.copy(categories = value) }
        }
        viewModelScope.launch {
            settingsRepo.settings.collect { value -> _ui.value = _ui.value.copy(settings = value) }
        }
    }

    private fun update(block: (UiState) -> UiState) {
        _ui.value = block(_ui.value)
    }

    fun setTab(tab: Tab) = update { it.copy(tab = tab) }
    fun setDate(date: String) = update { it.copy(selectedDate = date) }
    fun setAnchor(date: LocalDate) = update { it.copy(anchor = date) }
    fun setDim(dim: Dim) = update { it.copy(dim = dim) }
    fun setView(view: StatView) = update { it.copy(view = view) }
    fun setFilter(filter: String) = update { it.copy(filterCategory = filter) }
    fun setCounterFilter(id: String?) = update { it.copy(counterFilter = id) }
    fun setRange(start: String?, end: String?) = update { it.copy(rangeStart = start, rangeEnd = end) }
    fun setCompact(value: Boolean) = viewModelScope.launch { settingsRepo.setCompact(value) }
    fun setConfirmCount(value: Boolean) = viewModelScope.launch { settingsRepo.setConfirmCount(value) }
    fun pin(counter: CounterEntity) = saveCounter(counter.copy(pinned = !counter.pinned))
    fun move(counter: CounterEntity, direction: Int) {
        val ordered = _ui.value.counters.filter { it.pinned == counter.pinned }.toMutableList()
        val i = ordered.indexOfFirst { it.id == counter.id }
        val j = i + direction
        if (i < 0 || j !in ordered.indices) return
        java.util.Collections.swap(ordered, i, j)
        viewModelScope.launch { ordered.forEachIndexed { index, c -> repo.upsertCounter(c.copy(sortOrder = index)) } }
    }
    val events = kotlinx.coroutines.channels.Channel<com.example.counter.data.EntryEntity>(kotlinx.coroutines.channels.Channel.UNLIMITED)
    fun undoEntry(entry: EntryEntity) = viewModelScope.launch { repo.deleteEntry(entry) }
    fun restoreAutomatic(onResult: (Boolean) -> Unit) = viewModelScope.launch {
        onResult(runCatching { repo.restoreAutomatic() }.getOrDefault(false))
    }
    fun setSearch(text: String) = update { it.copy(search = text) }

    fun shiftPeriod(direction: Int) {
        val current = _ui.value
        val next = when (current.dim) {
            Dim.DAY -> current.anchor.plusDays(direction.toLong())
            Dim.WEEK -> current.anchor.plusWeeks(direction.toLong())
            Dim.MONTH -> current.anchor.plusMonths(direction.toLong())
            Dim.YEAR, Dim.ALL -> current.anchor.plusYears(direction.toLong())
        }
        update { it.copy(anchor = next) }
    }

    fun visibleCounters(): List<CounterEntity> {
        val state = _ui.value
        return state.counters.filter { counter ->
            (state.counterFilter == null || counter.id == state.counterFilter) &&
            (state.filterCategory == "all" || counter.category == state.filterCategory) &&
                (state.search.isBlank() || counter.name.contains(state.search, ignoreCase = true))
        }
    }

    fun newCounterId(): String = UUID.randomUUID().toString()

    fun addCount(counterId: String, delta: Double) {
        val date = TimeUtils.parse(_ui.value.selectedDate)
        val now = java.time.LocalTime.now()
        viewModelScope.launch {
            val at = if (date == TimeUtils.today()) System.currentTimeMillis() else TimeUtils.toEpoch(date, now.hour, now.minute)
            events.send(repo.addEntry(counterId, delta, at))
            feedback()
        }
    }

    fun addCustomEntry(
        counterId: String,
        delta: Double,
        date: LocalDate,
        hour: Int,
        minute: Int,
        note: String
    ) {
        viewModelScope.launch {
            events.send(repo.addEntry(counterId, delta, TimeUtils.toEpoch(date, hour, minute), note))
            feedback()
            WidgetUpdater.updateAll(getApplication<Application>())
        }
        update { it.copy(selectedDate = TimeUtils.ymd(date)) }
    }

    fun updateEntry(
        entry: EntryEntity,
        delta: Double,
        date: LocalDate,
        hour: Int,
        minute: Int,
        note: String
    ) {
        viewModelScope.launch {
            repo.updateEntry(
                entry.copy(
                    delta = delta,
                    localDate = TimeUtils.ymd(date),
                    occurredAt = TimeUtils.toEpoch(date, hour, minute),
                    note = note
                )
            )
        }
    }

    fun deleteEntry(entry: EntryEntity) {
        viewModelScope.launch { repo.deleteEntry(entry) }
    }

    fun undo() {
        viewModelScope.launch { repo.undoLast() }
    }

    fun saveCounter(counter: CounterEntity) {
        viewModelScope.launch { repo.upsertCounter(counter) }
    }

    fun deleteCounter(id: String) {
        viewModelScope.launch { repo.deleteCounter(id) }
    }

    fun addCategory(name: String) {
        viewModelScope.launch { repo.addCategory(name) }
    }

    fun deleteCategory(name: String) {
        viewModelScope.launch { repo.deleteCategory(name) }
        if (_ui.value.filterCategory == name) setFilter("all")
    }

    fun setDark(value: Boolean) = viewModelScope.launch { settingsRepo.setDark(value) }
    fun setSound(value: Boolean) = viewModelScope.launch { settingsRepo.setSound(value) }
    fun setHaptic(value: Boolean) = viewModelScope.launch { settingsRepo.setHaptic(value) }
    fun setWeekStartMonday(value: Boolean) = viewModelScope.launch { settingsRepo.setWeekStartMonday(value) }

    fun setCountMode(mode: CountMode) = viewModelScope.launch {
        settingsRepo.setCountMode(mode)
        repo.recalcAll()
    }

    fun setReminder(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch { settingsRepo.setReminder(enabled, hour, minute) }
        ReminderScheduler.schedule(getApplication<Application>(), enabled, hour, minute)
    }

    fun setVolume(enabled: Boolean, counterId: String) =
        viewModelScope.launch { settingsRepo.setVolume(enabled, counterId) }

    fun volumeIncrement() {
        val state = _ui.value
        if (!state.settings.volumeCountEnabled) return
        // A stale binding (e.g. its counter was deleted) would violate the FK on insert.
        val target = state.counters.firstOrNull { it.id == state.settings.volumeCounterId }
            ?: state.counters.firstOrNull()
            ?: return
        addCount(target.id, target.step)
    }

    fun clearAll() {
        viewModelScope.launch { repo.clearAll() }
    }

    private fun feedback() {
        val settings = _ui.value.settings
        if (settings.haptic) {
            @Suppress("DEPRECATION")
            val vibrator = getApplication<Application>()
                .getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
        if (settings.sound) {
            try {
                val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 60)
                tone.startTone(ToneGenerator.TONE_PROP_BEEP, 70)
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ tone.release() }, 150)
            } catch (ignored: Exception) {
            }
        }
    }

    private fun csv(value: Any?): String =
        "\"" + value.toString().replace("\"", "\"\"") + "\""

    fun exportCsv(): String {
        val builder = StringBuilder()
        builder.append("日期,时间,计数器,分类,增量,结果值,备注\n")
        val names = _ui.value.counters.associateBy { it.id }
        _ui.value.entries
            .sortedWith(
                compareBy<EntryEntity>(
                    { it.localDate }, { it.occurredAt }, { it.createdAt }, { it.id }
                )
            )
            .forEach { entry ->
                val (hour, minute) = TimeUtils.dateTimeParts(entry.occurredAt)
                val counter = names[entry.counterId]
                builder.append(csv(entry.localDate)).append(',')
                    .append(csv(String.format("%02d:%02d", hour, minute))).append(',')
                    .append(csv(counter?.name ?: "已删除")).append(',')
                    .append(csv(counter?.category ?: "")).append(',')
                    .append(formatNumber(entry.delta)).append(',')
                    .append(formatNumber(entry.valueAfter)).append(',')
                    .append(csv(entry.note)).append('\n')
            }
        return builder.toString()
    }

    fun exportDailyCsv(): String {
        val state = _ui.value
        val counters = visibleCounters()
        val ids = counters.map { it.id }.toSet()
        val entries = state.entries.filter { it.counterId in ids &&
            (state.rangeStart == null || it.localDate >= state.rangeStart) &&
            (state.rangeEnd == null || it.localDate <= state.rangeEnd) }
        return "\uFEFF" + (listOf("日期") + counters.map { it.name } + "当日净增量").joinToString(",") { csv(it) } + "\n" +
            entries.groupBy { it.localDate }.toSortedMap().entries.joinToString("\n") { (date, rows) ->
                (listOf(csv(date)) + counters.map { c -> formatNumber(rows.filter { it.counterId == c.id }.sumOf { it.delta }) } + formatNumber(rows.sumOf { it.delta })).joinToString(",")
            }
    }
    fun exportJson(): String = com.example.counter.data.BackupCodec.encode(_ui.value.counters, _ui.value.entries, _ui.value.categories)

    fun importJson(text: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = runCatching {
                val backup = com.example.counter.data.BackupCodec.decode(text)
                repo.replaceAll(backup.counters, backup.entries, backup.categories)
            }
            onResult(result.isSuccess)
        }
    }
}
