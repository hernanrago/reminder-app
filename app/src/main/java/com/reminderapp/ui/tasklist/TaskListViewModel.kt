package com.reminderapp.ui.tasklist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.reminderapp.data.model.Task
import com.reminderapp.data.repository.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskListViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = TaskRepository(app.applicationContext)

    val tasks: StateFlow<List<Task>> = repository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteTask(task: Task) {
        viewModelScope.launch { repository.delete(task) }
    }

    fun toggleActive(task: Task) {
        viewModelScope.launch { repository.setActive(task, !task.isActive) }
    }
}
