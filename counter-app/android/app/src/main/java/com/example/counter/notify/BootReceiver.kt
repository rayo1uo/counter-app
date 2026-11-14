package com.example.counter.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.counter.CounterApp
import com.example.counter.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as? CounterApp
                val settings = app?.container?.settings?.settings?.first()
                if (settings != null && settings.reminderEnabled) {
                    ReminderScheduler.schedule(
                        context, true, settings.reminderHour, settings.reminderMinute
                    )
                }
                WidgetUpdater.updateAll(context)
            } finally {
                result.finish()
            }
        }
    }
}
