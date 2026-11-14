package com.example.counter

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.counter.data.CounterDatabase
import com.example.counter.data.CounterRepository
import com.example.counter.data.SettingsRepository

class CounterApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "计数提醒",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "counter_reminder"
    }
}

class AppContainer(private val app: Application) {
    private val database = CounterDatabase.build(app)
    val dao = database.dao()
    val settings = SettingsRepository(app)
    val repository = CounterRepository(dao, settings, database, app)
}
