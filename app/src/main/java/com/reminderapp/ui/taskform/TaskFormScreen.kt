package com.reminderapp.ui.taskform

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFormScreen(
    taskId: Int?,
    onBack: () -> Unit,
    viewModel: TaskFormViewModel = viewModel()
) {
    LaunchedEffect(taskId) {
        if (taskId != null && taskId != 0) viewModel.loadTask(taskId)
    }

    val isEditing = taskId != null && taskId != 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Editar recordatorio" else "Nuevo recordatorio") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Título
            OutlinedTextField(
                value = viewModel.title,
                onValueChange = { viewModel.title = it },
                label = { Text("Título *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Descripción
            OutlinedTextField(
                value = viewModel.description,
                onValueChange = { viewModel.description = it },
                label = { Text("Descripción (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            // Tipo de schedule
            Text("Tipo de recordatorio", style = MaterialTheme.typography.labelLarge)
            ScheduleTypeSelector(
                selected = viewModel.scheduleType,
                onSelect = { viewModel.scheduleType = it }
            )

            // Sub-form por tipo
            when (viewModel.scheduleType) {
                ScheduleType.ONE_TIME -> OneTimeForm(viewModel)
                ScheduleType.INTERVAL -> IntervalForm(viewModel)
                ScheduleType.DAILY -> TimeForm(viewModel)
                ScheduleType.WEEKLY -> WeeklyForm(viewModel)
            }

            // Error de validación
            viewModel.validationError?.let {
                Text(it, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }

            // Botón guardar
            Button(
                onClick = { viewModel.save(onSuccess = onBack) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.isSaving
            ) {
                if (viewModel.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (isEditing) "Guardar cambios" else "Crear recordatorio")
                }
            }
        }
    }
}

@Composable
private fun ScheduleTypeSelector(selected: ScheduleType, onSelect: (ScheduleType) -> Unit) {
    val types = listOf(
        ScheduleType.ONE_TIME to "Una vez",
        ScheduleType.INTERVAL to "Intervalo",
        ScheduleType.DAILY to "Diario",
        ScheduleType.WEEKLY to "Semanal"
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        types.forEach { (type, label) ->
            FilterChip(
                selected = selected == type,
                onClick = { onSelect(type) },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OneTimeForm(vm: TaskFormViewModel) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val cal = Calendar.getInstance().apply { timeInMillis = vm.oneTimeDateMillis }

    OutlinedButton(
        onClick = { showDatePicker = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Fecha y hora: %02d/%02d/%04d %02d:%02d".format(
            cal.get(Calendar.DAY_OF_MONTH),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.YEAR),
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE)
        ))
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = vm.oneTimeDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { utcMillis ->
                        val prevCal = Calendar.getInstance().apply { timeInMillis = vm.oneTimeDateMillis }
                        // DatePicker devuelve UTC midnight — extraer campos en UTC para evitar desfase de timezone
                        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
                        val newCal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                            set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                            set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                            set(Calendar.HOUR_OF_DAY, prevCal.get(Calendar.HOUR_OF_DAY))
                            set(Calendar.MINUTE, prevCal.get(Calendar.MINUTE))
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        vm.oneTimeDateMillis = newCal.timeInMillis
                    }
                    showDatePicker = false
                    showTimePicker = true
                }) { Text("Siguiente") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = state) }
    }

    if (showTimePicker) {
        val prevCal = Calendar.getInstance().apply { timeInMillis = vm.oneTimeDateMillis }
        val timeState = rememberTimePickerState(
            initialHour = prevCal.get(Calendar.HOUR_OF_DAY),
            initialMinute = prevCal.get(Calendar.MINUTE)
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val newCal = Calendar.getInstance().apply {
                        timeInMillis = vm.oneTimeDateMillis
                        set(Calendar.HOUR_OF_DAY, timeState.hour)
                        set(Calendar.MINUTE, timeState.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    vm.oneTimeDateMillis = newCal.timeInMillis
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancelar") } },
            title = { Text("Seleccionar hora") },
            text = { TimePicker(state = timeState) }
        )
    }
}

@Composable
private fun IntervalForm(vm: TaskFormViewModel) {
    OutlinedTextField(
        value = if (vm.intervalMinutes > 0) vm.intervalMinutes.toString() else "",
        onValueChange = { vm.intervalMinutes = it.toIntOrNull() ?: 0 },
        label = { Text("Cada (minutos, mínimo 15)") },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
    )
}

@Composable
private fun TimeForm(vm: TaskFormViewModel) {
    TimePickerRow(hour = vm.hourOfDay, minute = vm.minute) { h, m ->
        vm.hourOfDay = h; vm.minute = m
    }
}

@Composable
private fun WeeklyForm(vm: TaskFormViewModel) {
    TimePickerRow(hour = vm.hourOfDay, minute = vm.minute) { h, m ->
        vm.hourOfDay = h; vm.minute = m
    }

    Text("Días de la semana", style = MaterialTheme.typography.labelLarge)
    val days = listOf(
        Calendar.MONDAY to "Lun",
        Calendar.TUESDAY to "Mar",
        Calendar.WEDNESDAY to "Mié",
        Calendar.THURSDAY to "Jue",
        Calendar.FRIDAY to "Vie",
        Calendar.SATURDAY to "Sáb",
        Calendar.SUNDAY to "Dom"
    )
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        days.forEach { (day, label) ->
            val selected = day in vm.selectedDays
            FilterChip(
                selected = selected,
                onClick = {
                    if (selected) vm.selectedDays.remove(day)
                    else vm.selectedDays.add(day)
                },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerRow(hour: Int, minute: Int, onChange: (Int, Int) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { showPicker = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Hora: %02d:%02d".format(hour, minute))
    }

    if (showPicker) {
        val state = rememberTimePickerState(initialHour = hour, initialMinute = minute)
        AlertDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onChange(state.hour, state.minute)
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancelar") } },
            title = { Text("Seleccionar hora") },
            text = { TimePicker(state = state) }
        )
    }
}
