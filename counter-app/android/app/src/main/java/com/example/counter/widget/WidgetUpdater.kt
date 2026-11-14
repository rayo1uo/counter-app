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
        val total = entries.filter { it.localDate == today }.sumOf { it.delta }
        withContext(Dispatchers.Main) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, CounterWidget::class.java)
            )
            ids.forEach { id -> CounterWidget.render(context, manager, id, total) }
        }
    }
}
