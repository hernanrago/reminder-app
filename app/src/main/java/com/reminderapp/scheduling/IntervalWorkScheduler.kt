package com.reminderapp.scheduling

import android.content.Context
import android.util.Log
import androidx.work.*
import com.reminderapp.data.model.Schedule
import com.reminderapp.data.model.Task
import java.util.concurrent.TimeUnit

object IntervalWorkScheduler {

    fun schedule(context: Context, task: Task) {
        val schedule = task.schedule as? Schedule.Interval ?: return
        val workName = workName(task.id)

        val request = PeriodicWorkRequestBuilder<ReminderWorker>(
            schedule.intervalMinutes.toLong(), TimeUnit.MINUTES
        )
            .setInputData(workDataOf(ReminderWorker.EXTRA_TASK_ID to task.id))
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            workName,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
        Log.d("IntervalWorkScheduler", "Scheduled task ${task.id} every ${schedule.intervalMinutes} min")
    }

    fun cancel(context: Context, taskId: Int) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(taskId))
        Log.d("IntervalWorkScheduler", "Cancelled work for task $taskId")
    }

    private fun workName(taskId: Int) = "interval_task_$taskId"
}
