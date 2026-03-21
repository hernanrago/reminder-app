package com.reminderapp.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.reminderapp.R
import com.reminderapp.data.model.Task
import com.reminderapp.ui.MainActivity

object ReminderNotificationManager {

    private const val CHANNEL_ID = "reminders"
    private const val CHANNEL_NAME = "Recordatorios"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notificaciones de recordatorios configurados"
            enableVibration(true)
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    fun sendNotification(context: Context, task: Task) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, task.id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val body = task.description.ifBlank { scheduleLabel(task) }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(task.title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(task.id, notification)
    }

    private fun scheduleLabel(task: Task): String = when (val s = task.schedule) {
        is com.reminderapp.data.model.Schedule.OneTime -> "Recordatorio puntual"
        is com.reminderapp.data.model.Schedule.Interval -> "Cada ${s.intervalMinutes} min"
        is com.reminderapp.data.model.Schedule.Daily -> "Diario %02d:%02d".format(s.hourOfDay, s.minute)
        is com.reminderapp.data.model.Schedule.Weekly -> "Semanal %02d:%02d".format(s.hourOfDay, s.minute)
    }
}
