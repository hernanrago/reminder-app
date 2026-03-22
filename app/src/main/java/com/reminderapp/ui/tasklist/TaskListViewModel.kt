package com.reminderapp.ui.tasklist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.reminderapp.data.db.AppDatabase
import com.reminderapp.data.db.TaskWithTag
import com.reminderapp.data.model.Tag
import com.reminderapp.data.model.Task
import com.reminderapp.data.repository.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskListViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = TaskRepository(app.applicationContext)
    private val dao = AppDatabase.getInstance(app.applicationContext).taskDao()

    // Lista agrupada: tags ordenados alfabéticamente, null al final.
    val groupedTasks: StateFlow<List<Pair<Tag?, List<TaskWithTag>>>> =
        dao.getTasksWithTags()
            .map { items ->
                items
                    .groupBy { it.tag }
                    .entries
                    .sortedWith(compareBy(nullsLast()) { it.key?.name })
                    .map { it.key to it.value }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteTask(task: Task) {
        viewModelScope.launch { repository.delete(task) }
    }

    fun toggleActive(task: Task) {
        viewModelScope.launch { repository.setActive(task, !task.isActive) }
    }
}
