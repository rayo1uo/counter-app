package com.example.counter.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.counterDataStore: DataStore<Preferences> by preferencesDataStore(name = "counter_settings")

enum class CountMode { DAILY, TOTAL }

data class AppSettings(
    val compact: Boolean = true,
    val confirmCount: Boolean = false,
    val darkTheme: Boolean = false,
    val sound: Boolean = true,
    val haptic: Boolean = true,
    val weekStartMonday: Boolean = false,
    val countMode: CountMode = CountMode.DAILY,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val volumeCountEnabled: Boolean = false,
    val volumeCounterId: String = ""
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val COMPACT = booleanPreferencesKey("compact")
        val CONFIRM = booleanPreferencesKey("confirm_count")
        val DARK = booleanPreferencesKey("dark")
        val SOUND = booleanPreferencesKey("sound")
        val HAPTIC = booleanPreferencesKey("haptic")
        val WEEK_MONDAY = booleanPreferencesKey("week_monday")
        val MODE = stringPreferencesKey("count_mode")
        val REMINDER_ON = booleanPreferencesKey("reminder_on")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val VOLUME_ON = booleanPreferencesKey("volume_on")
        val VOLUME_ID = stringPreferencesKey("volume_counter_id")
    }

    val settings: Flow<AppSettings> = context.counterDataStore.data.map { p ->
        AppSettings(
            compact = p[Keys.COMPACT] ?: true,
            confirmCount = p[Keys.CONFIRM] ?: false,
            darkTheme = p[Keys.DARK] ?: false,
            sound = p[Keys.SOUND] ?: true,
            haptic = p[Keys.HAPTIC] ?: true,
            weekStartMonday = p[Keys.WEEK_MONDAY] ?: false,
            countMode = if (p[Keys.MODE] == "total") CountMode.TOTAL else CountMode.DAILY,
            reminderEnabled = p[Keys.REMINDER_ON] ?: false,
            reminderHour = p[Keys.REMINDER_HOUR] ?: 20,
            reminderMinute = p[Keys.REMINDER_MINUTE] ?: 0,
            volumeCountEnabled = p[Keys.VOLUME_ON] ?: false,
            volumeCounterId = p[Keys.VOLUME_ID] ?: ""
        )
    }

    suspend fun setCompact(value: Boolean) { context.counterDataStore.edit { it[Keys.COMPACT] = value } }
    suspend fun setConfirmCount(value: Boolean) { context.counterDataStore.edit { it[Keys.CONFIRM] = value } }

    suspend fun setDark(value: Boolean) {
        context.counterDataStore.edit { it[Keys.DARK] = value }
    }

    suspend fun setSound(value: Boolean) {
        context.counterDataStore.edit { it[Keys.SOUND] = value }
    }

    suspend fun setHaptic(value: Boolean) {
        context.counterDataStore.edit { it[Keys.HAPTIC] = value }
    }

    suspend fun setWeekStartMonday(value: Boolean) {
        context.counterDataStore.edit { it[Keys.WEEK_MONDAY] = value }
    }

    suspend fun setCountMode(mode: CountMode) {
        context.counterDataStore.edit { it[Keys.MODE] = if (mode == CountMode.TOTAL) "total" else "daily" }
    }

    suspend fun setReminder(enabled: Boolean, hour: Int, minute: Int) {
        context.counterDataStore.edit { p: MutablePreferences ->
            p[Keys.REMINDER_ON] = enabled
            p[Keys.REMINDER_HOUR] = hour
            p[Keys.REMINDER_MINUTE] = minute
        }
    }

    suspend fun setVolume(enabled: Boolean, counterId: String) {
        context.counterDataStore.edit { p: MutablePreferences ->
            p[Keys.VOLUME_ON] = enabled
            p[Keys.VOLUME_ID] = counterId
        }
    }
}
