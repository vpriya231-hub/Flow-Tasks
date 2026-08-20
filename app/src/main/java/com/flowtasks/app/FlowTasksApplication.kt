package com.flowtasks.app

import android.app.Application
import com.flowtasks.app.core.di.AppContainer
import com.flowtasks.app.core.di.DefaultAppContainer
import com.flowtasks.app.core.notification.FlowTasksNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FlowTasksApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        FlowTasksNotificationHelper.createNotificationChannel(this)

        // Initialize Mobile Ads SDK safely and preload initial test ad
        container.interstitialAdManager.initialize(this)

        // Restore upcoming active reminders on app process startup
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val now = System.currentTimeMillis()
                val upcoming = container.taskRepository.getTasksWithUpcomingReminders(now)
                upcoming.forEach { task ->
                    container.reminderScheduler.scheduleReminder(task)
                }
            } catch (e: Exception) {
                // Ignore background initialization exceptions
            }
        }
    }
}
