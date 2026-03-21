package com.reminderapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val title: String,
    val description: String = "",

    // Almacenado como JSON via TypeConverter
    val schedule: Schedule,

    val isActive: Boolean = true,

    // Epoch millis del próximo disparo. Null si ya disparó (one-time) o inactiva.
    val nextFireAtMillis: Long? = null,

    val createdAtMillis: Long = System.currentTimeMillis()
)
