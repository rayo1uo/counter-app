package com.example.counter.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.counter.CounterApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CountActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != CounterWidget.ACTION_INCREMENT) return
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as? CounterApp
                if (app != null) {
                    val target = app.container.dao.allCounters()
                        .filter { !it.archived }
                        .sortedWith(compareByDescending<com.example.counter.data.CounterEntity> { it.pinned }.thenBy { it.sortOrder }.thenBy { it.createdAt }).firstOrNull()
                    if (target != null) {
                        app.container.repository.addEntry(
                            target.id,
                            target.step,
                            System.currentTimeMillis()
                        )
                        WidgetUpdater.updateAll(context)
                    }
                }
            } finally {
                result.finish()
            }
        }
    }
}
