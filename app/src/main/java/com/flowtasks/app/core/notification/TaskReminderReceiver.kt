package com.flowtasks.app.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.flowtasks.app.FlowTasksApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        if (taskId == -1L) return

        val fallbackTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Task Reminder"
        val fallbackDescription = intent.getStringExtra(EXTRA_TASK_DESCRIPTION)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as? FlowTasksApplication
                val task = if (app != null) {
                    app.container.taskRepository.getTaskByIdDirect(taskId)
                } else null

                // If task was found in database and is completed, don't show notification
                if (task != null) {
                    if (!task.isCompleted) {
                        FlowTasksNotificationHelper.showTaskReminderNotification(
                            context = context,
                            taskId = task.id,
                            taskTitle = task.title,
                            taskDescription = if (task.description.isNotBlank()) task.description else "Task is due now"
                        )
                    }
                } else {
                    // Fallback to intent extras if database could not be queried
                    FlowTasksNotificationHelper.showTaskReminderNotification(
                        context = context,
                        taskId = taskId,
                        taskTitle = fallbackTitle,
                        taskDescription = fallbackDescription
                    )
                }
            } catch (e: Exception) {
                Log.e("TaskReminderReceiver", "Error handling reminder broadcast for task $taskId", e)
                // Fallback to show notification
                FlowTasksNotificationHelper.showTaskReminderNotification(
                    context = context,
                    taskId = taskId,
                    taskTitle = fallbackTitle,
                    taskDescription = fallbackDescription
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_TASK_REMINDER = "com.flowtasks.app.flowtasks.ACTION_TASK_REMINDER"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_TASK_DESCRIPTION = "extra_task_description"
    }
}
