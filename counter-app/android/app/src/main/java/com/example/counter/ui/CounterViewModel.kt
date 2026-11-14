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
    val search: String = ""
)

class CounterViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as CounterApp).container
    private val dao = container.dao
    private val repo = container.repository
    private val settingsRepo = container.settings

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    init {
        viewModelScope.launch { repo.seedIfEmpty() }
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
            (state.filterCategory == "all" || counter.category == state.filterCategory) &&
                (state.search.isBlank() || counter.name.contains(state.search, ignoreCase = true))
        }
    }

    fun newCounterId(): String = UUID.randomUUID().toString()

    fun addCount(counterId: String, delta: Double) {
        val date = TimeUtils.parse(_ui.value.selectedDate)
        val now = java.time.LocalTime.now()
        viewModelScope.launch {
            repo.addEntry(counterId, delta, TimeUtils.toEpoch(date, now.hour, now.minute))
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
            repo.addEntry(counterId, delta, TimeUtils.toEpoch(date, hour, minute), note)
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
        val id = state.settings.volumeCounterId.ifEmpty {
            state.counters.firstOrNull()?.id ?: return
        }
        val step = state.counters.firstOrNull { it.id == id }?.step ?: 1.0
        addCount(id, step)
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
                ToneGenerator(AudioManager.STREAM_NOTIFICATION, 60)
                    .startTone(ToneGenerator.TONE_PROP_BEEP, 70)
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
        _ui.value.entries.sortedWith(compareBy({ it.localDate }, { it.occurredAt }))
            .forEach { entry ->
                val (hour, minute) = TimeUtils.dateTimeParts(entry.occurredAt)
                val counter = names[entry.counterId]
                builder.append(csv(entry.localDate)).append(',')
                    .append(csv(String.format("%02d:%02d", hour, minute))).append(',')
                    .append(csv(counter?.name ?: "已删除")).append(',')
                    .append(csv(counter?.category ?: "")).append(',')
                    .append(entry.delta).append(',')
                    .append(entry.valueAfter).append(',')
                    .append(csv(entry.note)).append('\n')
            }
        return builder.toString()
    }

    fun exportJson(): String {
        val root = JSONObject()
        val countersArray = JSONArray()
        _ui.value.counters.forEach { c ->
            countersArray.put(JSONObject().apply {
                put("id", c.id); put("name", c.name); put("color", c.color)
                put("icon", c.icon); put("step", c.step); put("initialValue", c.initialValue)
                put("target", c.target); put("category", c.category)
                put("sortOrder", c.sortOrder); put("archived", c.archived)
                put("createdAt", c.createdAt)
            })
        }
        val entriesArray = JSONArray()
        _ui.value.entries.forEach { e ->
            entriesArray.put(JSONObject().apply {
                put("id", e.id); put("counterId", e.counterId); put("delta", e.delta)
                put("valueAfter", e.valueAfter); put("occurredAt", e.occurredAt)
                put("localDate", e.localDate); put("tzOffsetSeconds", e.tzOffsetSeconds)
                put("note", e.note); put("createdAt", e.createdAt)
            })
        }
        val categoriesArray = JSONArray()
        _ui.value.categories.forEach { c ->
            categoriesArray.put(JSONObject().apply {
                put("name", c.name); put("sortOrder", c.sortOrder); put("color", c.color)
            })
        }
        root.put("version", 1)
        root.put("counters", countersArray)
        root.put("entries", entriesArray)
        root.put("categories", categoriesArray)
        val settingsObject = JSONObject().apply {
            put("darkTheme", _ui.value.settings.darkTheme)
            put("sound", _ui.value.settings.sound)
            put("haptic", _ui.value.settings.haptic)
            put("weekStartMonday", _ui.value.settings.weekStartMonday)
            put("countMode", _ui.value.settings.countMode.name)
            put("reminderEnabled", _ui.value.settings.reminderEnabled)
            put("reminderHour", _ui.value.settings.reminderHour)
            put("reminderMinute", _ui.value.settings.reminderMinute)
            put("volumeCountEnabled", _ui.value.settings.volumeCountEnabled)
            put("volumeCounterId", _ui.value.settings.volumeCounterId)
        }
        root.put("settings", settingsObject)
        return root.toString(2)
    }

    fun importJson(text: String): Boolean {
        return try {
            val root = JSONObject(text)
            val countersArray = root.optJSONArray("counters") ?: return false
            val entriesArray = root.optJSONArray("entries") ?: JSONArray()
            val categoriesArray = root.optJSONArray("categories") ?: JSONArray()

            val counters = (0 until countersArray.length()).map { i ->
                val o = countersArray.getJSONObject(i)
                CounterEntity(
                    id = o.optString("id", UUID.randomUUID().toString()),
                    name = o.optString("name", "未命名"),
                    color = o.optLong("color", 0xFFF5921E),
                    icon = o.optString("icon", ""),
                    step = o.optDouble("step", 1.0),
                    initialValue = o.optDouble("initialValue", 0.0),
                    target = o.optDouble("target", 0.0),
                    category = o.optString("category", "默认"),
                    sortOrder = o.optInt("sortOrder", 0),
                    archived = o.optBoolean("archived", false),
                    createdAt = o.optLong("createdAt", 0L)
                )
            }
            val entries = (0 until entriesArray.length()).map { i ->
                val o = entriesArray.getJSONObject(i)
                EntryEntity(
                    id = o.optString("id", UUID.randomUUID().toString()),
                    counterId = o.optString("counterId", ""),
                    delta = o.optDouble("delta", 0.0),
                    valueAfter = o.optDouble("valueAfter", 0.0),
                    occurredAt = o.optLong("occurredAt", System.currentTimeMillis()),
                    localDate = o.optString("localDate", TimeUtils.todayYmd()),
                    tzOffsetSeconds = o.optInt("tzOffsetSeconds", 0),
                    note = o.optString("note", ""),
                    createdAt = o.optLong("createdAt", 0L)
                )
            }
            val categories = (0 until categoriesArray.length()).map { i ->
                val o = categoriesArray.getJSONObject(i)
                CategoryEntity(
                    name = o.optString("name", "默认"),
                    sortOrder = o.optInt("sortOrder", 0),
                    color = o.optLong("color", 0xFF9B9B9B)
                )
            }
            viewModelScope.launch { repo.replaceAll(counters, entries, categories) }
            true
        } catch (e: Exception) {
            false
        }
    }
}
