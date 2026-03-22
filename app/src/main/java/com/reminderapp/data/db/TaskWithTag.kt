package com.reminderapp.data.db

import androidx.room.Embedded
import androidx.room.Relation
import com.reminderapp.data.model.Tag
import com.reminderapp.data.model.Task

data class TaskWithTag(
    @Embedded val task: Task,
    @Relation(parentColumn = "tagId", entityColumn = "id")
    val tag: Tag?
)
