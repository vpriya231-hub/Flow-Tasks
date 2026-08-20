package com.flowtasks.app.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.flowtasks.app.FlowTasksApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val appContainer = (context.applicationContext as? FlowTasksApplication)?.container ?: return
            val scheduler = TaskReminderScheduler(context)
            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val currentTime = System.currentTimeMillis()
                    val upcomingTasks = appContainer.taskRepository.getTasksWithUpcomingReminders(currentTime)
                    upcomingTasks.forEach { task ->
                        scheduler.scheduleReminder(task)
                    }
                } catch (e: Exception) {
                    // Ignore background restore errors
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
