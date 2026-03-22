package com.reminderapp.ui.taskform

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.reminderapp.data.db.AppDatabase
import com.reminderapp.data.model.Schedule
import com.reminderapp.data.model.Task
import com.reminderapp.data.repository.TaskRepository
import kotlinx.coroutines.launch
import java.util.Calendar

enum class ScheduleType { ONE_TIME, INTERVAL, DAILY, WEEKLY }

class TaskFormViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = TaskRepository(app.applicationContext)
    private val dao = AppDatabase.getInstance(app.applicationContext).taskDao()

    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var scheduleType by mutableStateOf(ScheduleType.DAILY)
    var hasAlarm by mutableStateOf(false)

    // ONE_TIME
    var oneTimeDateMillis by mutableStateOf(System.currentTimeMillis() + 60 * 60 * 1000L)

    // INTERVAL
    var intervalMinutes by mutableIntStateOf(15)

    // DAILY / WEEKLY
    var hourOfDay by mutableIntStateOf(9)
    var minute by mutableIntStateOf(0)

    // WEEKLY
    val selectedDays = mutableStateListOf<Int>()  // Calendar.MONDAY..SUNDAY

    var isSaving by mutableStateOf(false)
    var validationError by mutableStateOf<String?>(null)

    private var editingTaskId: Int = 0

    fun loadTask(taskId: Int) {
        viewModelScope.launch {
            val task = dao.getById(taskId) ?: return@launch
            editingTaskId = task.id
            title = task.title
            description = task.description
            hasAlarm = task.schedule != null
            val schedule = task.schedule ?: return@launch
            when (val s = schedule) {
                is Schedule.OneTime -> {
                    scheduleType = ScheduleType.ONE_TIME
                    oneTimeDateMillis = s.triggerAtMillis
                }
                is Schedule.Interval -> {
                    scheduleType = ScheduleType.INTERVAL
                    intervalMinutes = s.intervalMinutes
                }
                is Schedule.Daily -> {
                    scheduleType = ScheduleType.DAILY
                    hourOfDay = s.hourOfDay
                    minute = s.minute
                }
                is Schedule.Weekly -> {
                    scheduleType = ScheduleType.WEEKLY
                    hourOfDay = s.hourOfDay
                    minute = s.minute
                    selectedDays.clear()
                    selectedDays.addAll(s.daysOfWeek)
                }
            }
        }
    }

    fun save(onSuccess: () -> Unit) {
        validationError = null

        if (title.isBlank()) {
            validationError = "El título es obligatorio"
            return
        }
        if (hasAlarm) {
            if (scheduleType == ScheduleType.WEEKLY && selectedDays.isEmpty()) {
                validationError = "Selecciona al menos un día"
                return
            }
            if (scheduleType == ScheduleType.INTERVAL && intervalMinutes < 15) {
                validationError = "El intervalo mínimo es 15 minutos"
                return
            }
        }

        val schedule: Schedule? = if (!hasAlarm) null else when (scheduleType) {
            ScheduleType.ONE_TIME -> Schedule.OneTime(oneTimeDateMillis)
            ScheduleType.INTERVAL -> Schedule.Interval(intervalMinutes)
            ScheduleType.DAILY -> Schedule.Daily(hourOfDay, minute)
            ScheduleType.WEEKLY -> Schedule.Weekly(selectedDays.sorted(), hourOfDay, minute)
        }

        val task = Task(
            id = editingTaskId,
            title = title.trim(),
            description = description.trim(),
            schedule = schedule
        )

        isSaving = true
        viewModelScope.launch {
            repository.save(task)
            isSaving = false
            onSuccess()
        }
    }
}
