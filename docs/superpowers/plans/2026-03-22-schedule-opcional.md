# Schedule Opcional — Notas con/sin alarma

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Hacer que `Task.schedule` sea opcional (`Schedule?`) para que un ítem pueda ser una nota sin alarma o un recordatorio con alarma.

**Architecture:** Cambio bottom-up: primero modelo + BD, luego repositorio, luego UI. Cada tarea produce código compilable e independiente.

**Tech Stack:** Kotlin, Room, Jetpack Compose, AlarmManager, WorkManager, kotlinx.serialization

---

## Archivos a modificar

| Archivo | Cambio |
|---|---|
| `app/src/main/java/com/reminderapp/data/model/Task.kt` | `schedule: Schedule?` |
| `app/src/main/java/com/reminderapp/data/db/Converters.kt` | TypeConverters nullable |
| `app/src/main/java/com/reminderapp/data/db/AppDatabase.kt` | versión 2 + Migration(1,2) |
| `app/src/main/java/com/reminderapp/data/repository/TaskRepository.kt` | guard null schedule |
| `app/src/main/java/com/reminderapp/ui/taskform/TaskFormViewModel.kt` | `var hasAlarm` |
| `app/src/main/java/com/reminderapp/ui/taskform/TaskFormScreen.kt` | Switch "Con aviso" |
| `app/src/main/java/com/reminderapp/ui/tasklist/TaskListScreen.kt` | ocultar schedule/switch para notas |

---

### Task 1: Modelo + TypeConverter + BD (capas de datos)

**Files:**
- Modify: `app/src/main/java/com/reminderapp/data/model/Task.kt`
- Modify: `app/src/main/java/com/reminderapp/data/db/Converters.kt`
- Modify: `app/src/main/java/com/reminderapp/data/db/AppDatabase.kt`

- [ ] **Step 1: Hacer `schedule` nullable en `Task.kt`**

  Reemplazar:
  ```kotlin
  val schedule: Schedule,
  ```
  Por:
  ```kotlin
  val schedule: Schedule? = null,
  ```

- [ ] **Step 2: Actualizar TypeConverters para nullable**

  En `Converters.kt`, reemplazar las dos funciones por:
  ```kotlin
  @TypeConverter
  fun scheduleToJson(schedule: Schedule?): String? =
      schedule?.let { json.encodeToString(it) }

  @TypeConverter
  fun jsonToSchedule(value: String?): Schedule? =
      value?.let { json.decodeFromString(it) }
  ```

- [ ] **Step 3: Incrementar versión de BD y agregar migración**

  En `AppDatabase.kt`, el archivo completo quedaría así:
  ```kotlin
  package com.reminderapp.data.db

  import android.content.Context
  import androidx.room.Database
  import androidx.room.Room
  import androidx.room.RoomDatabase
  import androidx.room.TypeConverters
  import androidx.room.migration.Migration
  import androidx.sqlite.db.SupportSQLiteDatabase
  import com.reminderapp.data.model.Task

  @Database(entities = [Task::class], version = 2, exportSchema = false)
  @TypeConverters(Converters::class)
  abstract class AppDatabase : RoomDatabase() {

      abstract fun taskDao(): TaskDao

      companion object {
          @Volatile private var INSTANCE: AppDatabase? = null

          private val MIGRATION_1_2 = object : Migration(1, 2) {
              override fun migrate(database: SupportSQLiteDatabase) {
                  // La columna schedule ya es TEXT y permite NULL en SQLite.
                  // Room solo necesita este objeto para no invalidar la BD.
              }
          }

          fun getInstance(context: Context): AppDatabase =
              INSTANCE ?: synchronized(this) {
                  INSTANCE ?: Room.databaseBuilder(
                      context.applicationContext,
                      AppDatabase::class.java,
                      "reminder_db"
                  )
                  .addMigrations(MIGRATION_1_2)
                  .build().also { INSTANCE = it }
              }
      }
  }
  ```

- [ ] **Step 4: Verificar que compila**

  ```bash
  cd /Users/hernan.rago/etc/reminder-app
  ./gradlew :app:compileDebugKotlin
  ```
  Esperado: BUILD SUCCESSFUL (puede haber warnings por `schedule` nullable en otros archivos, son normales).

- [ ] **Step 5: Commit**

  ```bash
  git add app/src/main/java/com/reminderapp/data/model/Task.kt \
          app/src/main/java/com/reminderapp/data/db/Converters.kt \
          app/src/main/java/com/reminderapp/data/db/AppDatabase.kt
  git commit -m "feat: make Task.schedule nullable (Schedule?) with Room migration 1→2"
  ```

---

### Task 2: Repositorio — guard null schedule

**Files:**
- Modify: `app/src/main/java/com/reminderapp/data/repository/TaskRepository.kt`

- [ ] **Step 1: Actualizar `save()`**

  Reemplazar:
  ```kotlin
  val nextFire = NextFireTimeCalculator.compute(task.schedule)
  ```
  Por:
  ```kotlin
  val nextFire = task.schedule?.let { NextFireTimeCalculator.compute(it) }
  ```

- [ ] **Step 2: Actualizar `setActive()`**

  Reemplazar:
  ```kotlin
  val next = NextFireTimeCalculator.compute(task.schedule)
  ```
  Por:
  ```kotlin
  val next = task.schedule?.let { NextFireTimeCalculator.compute(it) }
  ```

- [ ] **Step 3: Actualizar `scheduleTask()` y `cancelSchedule()`**

  Reemplazar:
  ```kotlin
  private fun scheduleTask(task: Task) {
      when (task.schedule) {
          is Schedule.Interval -> IntervalWorkScheduler.schedule(context, task)
          else -> AlarmScheduler.schedule(context, task)
      }
  }

  private fun cancelSchedule(taskId: Int, schedule: Schedule) {
      when (schedule) {
          is Schedule.Interval -> IntervalWorkScheduler.cancel(context, taskId)
          else -> AlarmScheduler.cancel(context, taskId)
      }
  }
  ```
  Por:
  ```kotlin
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
          null -> return  // nada que cancelar
          else -> AlarmScheduler.cancel(context, taskId)
      }
  }

  // Nota: el parámetro pasó de `Schedule` a `Schedule?` para aceptar tareas sin schedule.
  // Los callers `cancelSchedule(task.id, task.schedule)` ya pasan un `Schedule?` porque
  // `task.schedule` es nullable desde Task 1.
  ```

- [ ] **Step 4: Verificar que compila**

  ```bash
  ./gradlew :app:compileDebugKotlin
  ```
  Esperado: BUILD SUCCESSFUL sin errores.

- [ ] **Step 5: Commit**

  ```bash
  git add app/src/main/java/com/reminderapp/data/repository/TaskRepository.kt
  git commit -m "feat: repository handles null schedule (no alarm tasks)"
  ```

---

### Task 3: ViewModel — estado `hasAlarm`

**Files:**
- Modify: `app/src/main/java/com/reminderapp/ui/taskform/TaskFormViewModel.kt`

- [ ] **Step 1: Agregar campo `hasAlarm`**

  Después de `var scheduleType by mutableStateOf(ScheduleType.DAILY)`, agregar:
  ```kotlin
  var hasAlarm by mutableStateOf(false)
  ```

- [ ] **Step 2: Cargar `hasAlarm` en `loadTask()`**

  Reemplazar el bloque `when` en `loadTask()` por una versión con null-check primero:
  ```kotlin
  editingTaskId = task.id
  title = task.title
  description = task.description
  hasAlarm = task.schedule != null
  if (task.schedule == null) return@launch
  when (val s = task.schedule) {
      is Schedule.OneTime -> { ... }  // igual que antes
      ...
  }
  ```

- [ ] **Step 3: Usar `hasAlarm` en `save()`**

  En el bloque que construye `schedule`, reemplazar:
  ```kotlin
  val schedule: Schedule = when (scheduleType) {
      ScheduleType.ONE_TIME -> Schedule.OneTime(oneTimeDateMillis)
      ScheduleType.INTERVAL -> Schedule.Interval(intervalMinutes)
      ScheduleType.DAILY -> Schedule.Daily(hourOfDay, minute)
      ScheduleType.WEEKLY -> Schedule.Weekly(selectedDays.sorted(), hourOfDay, minute)
  }
  ```
  Por:
  ```kotlin
  val schedule: Schedule? = if (!hasAlarm) null else when (scheduleType) {
      ScheduleType.ONE_TIME -> Schedule.OneTime(oneTimeDateMillis)
      ScheduleType.INTERVAL -> Schedule.Interval(intervalMinutes)
      ScheduleType.DAILY -> Schedule.Daily(hourOfDay, minute)
      ScheduleType.WEEKLY -> Schedule.Weekly(selectedDays.sorted(), hourOfDay, minute)
  }
  ```

  También mover las validaciones de schedule dentro de un guard `if (hasAlarm)`:
  ```kotlin
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
  ```

- [ ] **Step 4: Verificar que compila**

  ```bash
  ./gradlew :app:compileDebugKotlin
  ```
  Esperado: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

  ```bash
  git add app/src/main/java/com/reminderapp/ui/taskform/TaskFormViewModel.kt
  git commit -m "feat: TaskFormViewModel adds hasAlarm toggle state"
  ```

---

### Task 4: UI Formulario — Switch "Con aviso"

**Files:**
- Modify: `app/src/main/java/com/reminderapp/ui/taskform/TaskFormScreen.kt`

- [ ] **Step 1: Agregar Switch antes del selector de tipo**

  Reemplazar el bloque:
  ```kotlin
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
  ```
  Por:
  ```kotlin
  // Toggle alarma
  Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
  ) {
      Text("Con aviso", style = MaterialTheme.typography.labelLarge)
      Switch(
          checked = viewModel.hasAlarm,
          onCheckedChange = { viewModel.hasAlarm = it }
      )
  }

  // Tipo de schedule (solo si tiene alarma)
  if (viewModel.hasAlarm) {
      Text("Tipo de recordatorio", style = MaterialTheme.typography.labelLarge)
      ScheduleTypeSelector(
          selected = viewModel.scheduleType,
          onSelect = { viewModel.scheduleType = it }
      )

      when (viewModel.scheduleType) {
          ScheduleType.ONE_TIME -> OneTimeForm(viewModel)
          ScheduleType.INTERVAL -> IntervalForm(viewModel)
          ScheduleType.DAILY -> TimeForm(viewModel)
          ScheduleType.WEEKLY -> WeeklyForm(viewModel)
      }
  }
  ```

  Agregar `import androidx.compose.ui.Alignment` si no está ya.

- [ ] **Step 2: Verificar que compila**

  ```bash
  ./gradlew :app:compileDebugKotlin
  ```
  Esperado: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

  ```bash
  git add app/src/main/java/com/reminderapp/ui/taskform/TaskFormScreen.kt
  git commit -m "feat: add 'Con aviso' switch to task form, hide schedule fields when off"
  ```

---

### Task 5: UI Lista — ocultar schedule para notas sin alarma

**Files:**
- Modify: `app/src/main/java/com/reminderapp/ui/tasklist/TaskListScreen.kt`

- [ ] **Step 1: Ocultar etiqueta de schedule cuando es null**

  En `TaskCard`, dentro del `Column`, el bloque de schedule info es:
  ```kotlin
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
  ```
  Reemplazarlo por:
  ```kotlin
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
  ```

- [ ] **Step 2: Ocultar Switch cuando no tiene schedule**

  El `Switch` en `TaskCard` está en el `Row` exterior. Envolverlo en un `if`:
  ```kotlin
  if (task.schedule != null) {
      Switch(
          checked = task.isActive,
          onCheckedChange = { onToggleActive() }
      )
      Spacer(Modifier.width(4.dp))
  }
  ```

- [ ] **Step 3: Actualizar firma de `scheduleDescription`**

  La función `scheduleDescription` recibe `Schedule` no-null, pero ahora puede llamarse solo cuando `schedule != null`, así que no necesita cambios de firma. Verificar que el compilador no se queja.

- [ ] **Step 4: Verificar que compila**

  ```bash
  ./gradlew :app:compileDebugKotlin
  ```
  Esperado: BUILD SUCCESSFUL.

- [ ] **Step 5: Build completo y verificación final**

  ```bash
  ./gradlew :app:assembleDebug
  ```
  Esperado: BUILD SUCCESSFUL, APK generado.

- [ ] **Step 6: Commit**

  ```bash
  git add app/src/main/java/com/reminderapp/ui/tasklist/TaskListScreen.kt
  git commit -m "feat: hide schedule label and active switch for tasks without alarm"
  ```

---

### Task 6: Commit del spec y push final

- [ ] **Step 1: Commit del spec y plan**

  ```bash
  git add docs/
  git commit -m "docs: add spec and implementation plan for optional schedule feature"
  ```

- [ ] **Step 2: Push**

  ```bash
  git push
  ```
