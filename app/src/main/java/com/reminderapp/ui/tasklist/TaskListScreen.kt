package com.reminderapp.ui.tasklist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reminderapp.data.model.Schedule
import com.reminderapp.data.model.Task
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    onNewTask: () -> Unit,
    onEditTask: (Int) -> Unit,
    viewModel: TaskListViewModel = viewModel()
) {
    val tasks by viewModel.tasks.collectAsState()
    var taskToDelete by remember { mutableStateOf<Task?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Recordatorios") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewTask) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo recordatorio")
            }
        }
    ) { padding ->
        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Sin recordatorios", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Toca + para crear uno", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onToggleActive = { viewModel.toggleActive(task) },
                        onEdit = { onEditTask(task.id) },
                        onDelete = { taskToDelete = task }
                    )
                }
            }
        }
    }

    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text("Eliminar recordatorio") },
            text = { Text("¿Eliminar \"${task.title}\"? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTask(task)
                    taskToDelete = null
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun TaskCard(
    task: Task,
    onToggleActive: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isActive)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (task.description.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (task.schedule != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = scheduleDescription(task.schedule),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    task.nextFireAtMillis?.let {
                        Text(
                            text = "Próximo: ${formatNextFire(it)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (task.schedule != null) {
                Switch(
                    checked = task.isActive,
                    onCheckedChange = { onToggleActive() }
                )
                Spacer(Modifier.width(4.dp))
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Editar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun scheduleDescription(schedule: Schedule): String = when (schedule) {
    is Schedule.OneTime -> "Una vez — ${formatDate(schedule.triggerAtMillis)}"
    is Schedule.Interval -> "Cada ${schedule.intervalMinutes} min"
    is Schedule.Daily -> "Diario — %02d:%02d".format(schedule.hourOfDay, schedule.minute)
    is Schedule.Weekly -> {
        val dayNames = schedule.daysOfWeek.sorted().joinToString(", ") { dayName(it) }
        "Semanal ($dayNames) — %02d:%02d".format(schedule.hourOfDay, schedule.minute)
    }
}

private fun dayName(calDay: Int): String = when (calDay) {
    Calendar.MONDAY -> "Lun"
    Calendar.TUESDAY -> "Mar"
    Calendar.WEDNESDAY -> "Mié"
    Calendar.THURSDAY -> "Jue"
    Calendar.FRIDAY -> "Vie"
    Calendar.SATURDAY -> "Sáb"
    Calendar.SUNDAY -> "Dom"
    else -> "?"
}

private val dateFormat = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
private val timeFormat = SimpleDateFormat("HH:mm dd/MM", Locale.getDefault())

private fun formatDate(millis: Long) = dateFormat.format(Date(millis))
private fun formatNextFire(millis: Long) = timeFormat.format(Date(millis))
