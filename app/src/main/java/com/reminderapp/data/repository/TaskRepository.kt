package com.reminderapp.data.repository

import android.content.Context
import com.reminderapp.data.db.AppDatabase
import com.reminderapp.data.model.Schedule
import com.reminderapp.data.model.Task
import com.reminderapp.scheduling.AlarmScheduler
import com.reminderapp.scheduling.IntervalWorkScheduler
import com.reminderapp.scheduling.NextFireTimeCalculator
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val context: Context) {

    private val dao = AppDatabase.getInstance(context).taskDao()

    fun getAll(): Flow<List<Task>> = dao.getAll()

    suspend fun save(task: Task) {
        val nextFire = task.schedule?.let { NextFireTimeCalculator.compute(it) }
        val taskToSave = task.copy(nextFireAtMillis = nextFire)

        val id = if (task.id == 0) {
            dao.insert(taskToSave).toInt()
        } else {
            cancelSchedule(task.id, task.schedule)
            dao.update(taskToSave)
            task.id
        }

        if (taskToSave.isActive && nextFire != null) {
            val savedTask = taskToSave.copy(id = id)
            scheduleTask(savedTask)
        }
    }

    suspend fun delete(task: Task) {
        cancelSchedule(task.id, task.schedule)
        dao.delete(task)
    }

    suspend fun setActive(task: Task, active: Boolean) {
        if (active) {
            val next = task.schedule?.let { NextFireTimeCalculator.compute(it) }
            val updated = task.copy(isActive = true, nextFireAtMillis = next)
            dao.update(updated)
            if (next != null) scheduleTask(updated)
        } else {
            cancelSchedule(task.id, task.schedule)
            dao.update(task.copy(isActive = false, nextFireAtMillis = null))
        }
    }

    private fun scheduleTask(task: Task) {
        when (task.schedule) {
            is Schedule.Interval -> IntervalWorkScheduler.schedule(context, task)
            null -> return
            else -> AlarmScheduler.schedule(context, task)
        }
    }

    private fun cancelSchedule(taskId: Int, schedule: Schedule?) {
        when (schedule) {
            is Schedule.Interval -> IntervalWorkScheduler.cancel(context, taskId)
            null -> return
            else -> AlarmScheduler.cancel(context, taskId)
        }
    }
}
