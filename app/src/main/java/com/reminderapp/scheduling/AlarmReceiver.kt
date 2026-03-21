package com.reminderapp.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.reminderapp.data.db.AppDatabase
import com.reminderapp.data.model.Schedule
import com.reminderapp.notification.ReminderNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "AlarmReceiver"

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_TASK_ID = "task_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
        if (taskId == -1) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val task = db.taskDao().getById(taskId)

                if (task == null || !task.isActive) {
                    Log.w(TAG, "Task $taskId not found or inactive, skipping")
                    return@launch
                }

                // Disparar la notificación
                ReminderNotificationManager.sendNotification(context, task)
                Log.d(TAG, "Notification sent for task: ${task.title}")

                // Reprogramar o desactivar según el tipo de schedule
                when (task.schedule) {
                    is Schedule.OneTime -> {
                        db.taskDao().update(task.copy(isActive = false, nextFireAtMillis = null))
                        Log.d(TAG, "One-time task ${task.id} deactivated")
                    }
                    is Schedule.Daily, is Schedule.Weekly -> {
                        val next = NextFireTimeCalculator.compute(task.schedule)
                        if (next != null) {
                            val updated = task.copy(nextFireAtMillis = next)
                            db.taskDao().update(updated)
                            AlarmScheduler.schedule(context, updated)
                            Log.d(TAG, "Task ${task.id} rescheduled to $next")
                        } else {
                            db.taskDao().update(task.copy(isActive = false, nextFireAtMillis = null))
                        }
                    }
                    is Schedule.Interval -> {
                        // Manejado por WorkManager, no debería llegar aquí
                        Log.w(TAG, "Interval task ${task.id} received by AlarmReceiver — unexpected")
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
