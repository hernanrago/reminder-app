package com.reminderapp.data.db

import androidx.room.TypeConverter
import com.reminderapp.data.model.Schedule
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

class Converters {

    @TypeConverter
    fun scheduleToJson(schedule: Schedule?): String? =
        schedule?.let { json.encodeToString(it) }

    @TypeConverter
    fun jsonToSchedule(value: String?): Schedule? =
        value?.let { json.decodeFromString(it) }
}
