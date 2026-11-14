package com.example.counter.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.example.counter.CounterApp
import com.example.counter.domain.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WidgetUpdater {

    suspend fun updateAll(context: Context) {
        val app = context.applicationContext as? CounterApp ?: return
        val entries = app.container.dao.allEntries()
        val today = TimeUtils.todayYmd()
        val target = app.container.dao.allCounters().filter { !it.archived }.sortedWith(compareByDescending<com.example.counter.data.CounterEntity> { it.pinned }.thenBy { it.sortOrder }.thenBy { it.createdAt }).firstOrNull()
        val total = entries.filter { it.localDate == today && it.counterId == target?.id }.sumOf { it.delta }
        withContext(Dispatchers.Main) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, CounterWidget::class.java)
            )
            ids.forEach { id -> CounterWidget.render(context, manager, id, total, target?.name ?: "暂无计数器", target?.step ?: 1.0) }
        }
    }
}
