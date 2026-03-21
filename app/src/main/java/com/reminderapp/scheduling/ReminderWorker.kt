package com.reminderapp.scheduling

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.reminderapp.data.db.AppDatabase
import com.reminderapp.notification.ReminderNotificationManager

private const val TAG = "ReminderWorker"

class ReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getInt(EXTRA_TASK_ID, -1)
        if (taskId == -1) return Result.failure()

        val task = AppDatabase.getInstance(applicationContext).taskDao().getById(taskId)

        if (task == null) {
            Log.w(TAG, "Task $taskId not found")
            return Result.failure()
        }

        if (!task.isActive) {
            Log.d(TAG, "Task $taskId is inactive, skipping")
            return Result.success()
        }

        ReminderNotificationManager.sendNotification(applicationContext, task)
        Log.d(TAG, "Interval notification sent: ${task.title}")
        return Result.success()
    }

    companion object {
        const val EXTRA_TASK_ID = "task_id"
    }
}
