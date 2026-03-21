package com.reminderapp.data.db

import androidx.room.TypeConverter
import com.reminderapp.data.model.Schedule
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

class Converters {

    @TypeConverter
    fun scheduleToJson(schedule: Schedule): String =
        json.encodeToString(schedule)

    @TypeConverter
    fun jsonToSchedule(value: String): Schedule =
        json.decodeFromString(value)
}
