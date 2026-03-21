package com.reminderapp

import android.app.Application
import com.reminderapp.notification.ReminderNotificationManager

class ReminderApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ReminderNotificationManager.createChannel(this)
    }
}
