package com.reminderapp.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.reminderapp.data.db.AppDatabase
import com.reminderapp.data.model.Schedule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "BootReceiver"

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.d(TAG, "Device rebooted, re-registering all active tasks...")

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tasks = AppDatabase.getInstance(context).taskDao().getAllActive().first()
                val db = AppDatabase.getInstance(context)

                tasks.forEach { task ->
                    when (val schedule = task.schedule) {
                        null -> {
                            // Nota sin alarma, nada que reprogramar
                        }
                        is Schedule.Interval -> {
                            IntervalWorkScheduler.schedule(context, task)
                        }
                        else -> {
                            // Recalcular next fire desde ahora (el tiempo original puede estar en el pasado)
                            val next = NextFireTimeCalculator.compute(schedule)
                            if (next != null) {
                                val updated = task.copy(nextFireAtMillis = next)
                                db.taskDao().update(updated)
                                AlarmScheduler.schedule(context, updated)
                                Log.d(TAG, "Re-scheduled task ${task.id} at $next")
                            } else {
                                // One-time que expiró mientras el dispositivo estaba apagado
                                db.taskDao().update(task.copy(isActive = false, nextFireAtMillis = null))
                                Log.d(TAG, "Task ${task.id} expired while device was off, deactivated")
                            }
                        }
                    }
                }
                Log.d(TAG, "Re-registered ${tasks.size} active task(s)")
            } finally {
                pendingResult.finish()
            }
        }
    }
}
