package com.example.counter.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.counter.R
import com.example.counter.ui.formatNumber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CounterWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                WidgetUpdater.updateAll(context)
            } finally {
                result.finish()
            }
        }
    }

    companion object {
        const val ACTION_INCREMENT = "com.example.counter.WIDGET_INCREMENT"
        const val EXTRA_WIDGET_ID = "widget_id"

        fun render(
            context: Context,
            manager: AppWidgetManager,
            widgetId: Int,
            todayTotal: Double, name: String = "计数器", step: Double = 1.0
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_counter)
            views.setTextViewText(R.id.widget_label, "$name · 今日")
            views.setTextViewText(R.id.widget_add, "+${formatNumber(step)}")
            views.setTextViewText(R.id.widget_value, formatNumber(todayTotal))

            val intent = Intent(context, CountActionReceiver::class.java).apply {
                action = ACTION_INCREMENT
                putExtra(EXTRA_WIDGET_ID, widgetId)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pending = PendingIntent.getBroadcast(context, widgetId, intent, flags)
            views.setOnClickPendingIntent(R.id.widget_add, pending)

            manager.updateAppWidget(widgetId, views)
        }
    }
}
