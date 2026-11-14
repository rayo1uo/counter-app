package com.example.counter.notify

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.counter.CounterApp
import com.example.counter.R
import kotlinx.coroutines.flow.first

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? CounterApp
        if (app != null) {
            val settings = app.container.settings.settings.first()
            if (settings.reminderEnabled) {
                sendReminder()
                ReminderScheduler.schedule(
                    applicationContext, true,
                    settings.reminderHour, settings.reminderMinute
                )
            }
        }
        return Result.success()
    }

    private fun sendReminder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val launchIntent = applicationContext.packageManager
            .getLaunchIntentForPackage(applicationContext.packageName) ?: return
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pending = PendingIntent.getActivity(applicationContext, 0, launchIntent, flags)

        val notification = NotificationCompat.Builder(applicationContext, CounterApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(applicationContext.getString(R.string.notification_title))
            .setContentText(applicationContext.getString(R.string.notification_body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(applicationContext).notify(1001, notification)
        } catch (ignored: SecurityException) {
        }
    }
}
