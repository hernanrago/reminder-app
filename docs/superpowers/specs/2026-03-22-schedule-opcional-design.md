---
name: Schedule opcional — notas con y sin alarma
description: Hacer que el campo schedule de Task sea opcional, convirtiendo los ítems en notas con o sin alarma/aviso
type: project
date: 2026-03-22
status: approved
---

# Diseño: Schedule opcional (notas con/sin alarma)

## Contexto

Actualmente cada `Task` tiene un `Schedule` obligatorio, lo que significa que todo ítem genera una alarma del sistema. El objetivo es permitir que un ítem sea simplemente una nota sin alarma, o una nota con alarma/aviso.

## Cambios por capa

### 1. Modelo de datos

**`Task.kt`**
- `schedule: Schedule` → `schedule: Schedule?` (default `null`)
- `null` = nota sin alarma; `nextFireAtMillis` siempre null en este caso

**`Converters.kt`**
- El TypeConverter de Schedule ya trabaja con JSON. Agregar manejo de null: `null` ↔ `NULL` en columna TEXT.

**`AppDatabase.kt`**
- Incrementar versión 1 → 2
- Agregar `Migration(1, 2)`: migración vacía (la columna TEXT ya permite NULL en SQLite; Room solo necesita el objeto `Migration` para no crashear)

### 2. Lógica de negocio

**`TaskRepository.kt`**
- `save()`: calcular `nextFire` solo si `schedule != null` → `task.schedule?.let { NextFireTimeCalculator.compute(it) }`
- `scheduleTask()` / `cancelSchedule()`: guard `schedule ?: return` cuando sea null
- `setActive()`: mismo patrón

**`AlarmReceiver` / `ReminderWorker`**: sin cambios (nunca se programan para tareas sin schedule)

### 3. UI — Formulario

**`TaskFormViewModel.kt`**
- Agregar `var hasAlarm by mutableStateOf(false)` (default `false` → nueva nota sin alarma por defecto)
- En `loadTask()`: `hasAlarm = task.schedule != null`
- En `save()`: si `!hasAlarm`, pasar `schedule = null`

**`TaskFormScreen.kt`**
- Agregar `Switch` "Con aviso" encima del selector de tipo
- Ocultar selector de tipo y sub-forms cuando `hasAlarm == false`

### 4. UI — Lista

**`TaskListScreen.kt`**
- Tareas sin schedule: no mostrar etiqueta de schedule ni "Próximo:" (nada)
- Switch `isActive` visible solo si tiene schedule

## Decisiones

| Decisión | Elección |
|---|---|
| Representación "sin alarma" | `schedule = null` (opción A) |
| Default al crear nuevo ítem | `hasAlarm = false` (sin alarma) |
| Label en lista para notas sin alarma | No mostrar nada (campo vacío) |
| Switch activo/inactivo sin schedule | Ocultar |
