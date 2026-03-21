package com.reminderapp.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class Schedule {

    @Serializable
    @SerialName("one_time")
    data class OneTime(
        val triggerAtMillis: Long
    ) : Schedule()

    @Serializable
    @SerialName("interval")
    data class Interval(
        val intervalMinutes: Int  // mínimo 15 (limitación WorkManager)
    ) : Schedule()

    @Serializable
    @SerialName("daily")
    data class Daily(
        val hourOfDay: Int,  // 0-23
        val minute: Int      // 0-59
    ) : Schedule()

    @Serializable
    @SerialName("weekly")
    data class Weekly(
        val daysOfWeek: List<Int>,  // Calendar.MONDAY (2)..Calendar.SUNDAY (1)
        val hourOfDay: Int,
        val minute: Int
    ) : Schedule()
}
