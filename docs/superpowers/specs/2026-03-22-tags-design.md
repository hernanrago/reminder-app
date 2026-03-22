---
name: Tags para recordatorios
description: Agregar tags opcionales a los recordatorios con tabla independiente, selección/creación inline y agrupación en la lista principal
type: project
date: 2026-03-22
status: approved
---

# Diseño: Tags para recordatorios

## Contexto

Los ítems (recordatorios/notas) pueden tener un tag opcional. El tag se elige de los ya existentes o se crea nuevo al momento de guardar. Los tags persisten aunque no tengan recordatorios asociados. La lista principal agrupa los ítems por tag.

## Modelo de datos

### Nueva entidad `Tag`

```kotlin
@Entity(tableName = "tags", indices = [Index(value = ["name"], unique = true)])
data class Tag(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String
)
```

### `Task` — nuevo campo

```kotlin
val tagId: Int? = null  // FK soft a Tag.id
```

### `AppDatabase` — cambios

- `version = 3`
- `entities = [Task::class, Tag::class]`
- `abstract fun tagDao(): TagDao`
- `MIGRATION_2_3`

### Migración BD v2 → v3

```sql
CREATE TABLE tags (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL
)
CREATE UNIQUE INDEX index_tags_name ON tags(name)
ALTER TABLE tasks ADD COLUMN tagId INTEGER
```

FK es soft (sin `REFERENCES`) — SQLite no la enforcea por defecto y simplifica la migración.

## Capas

### DAO

**`TagDao`:**
- `insert(tag: Tag): Long`
- `getAll(): Flow<List<Tag>>`
- `getById(id: Int): Tag?`
- `getByName(name: String): Tag?`

**`TaskDao`** — nuevo query:
```kotlin
data class TaskWithTag(
    @Embedded val task: Task,
    @Relation(parentColumn = "tagId", entityColumn = "id")
    val tag: Tag?
)

@Transaction
@Query("SELECT * FROM tasks")
fun getTasksWithTags(): Flow<List<TaskWithTag>>
```

### Repositorio

**`TagRepository`** (nuevo):
- `getAll(): Flow<List<Tag>>`
- `save(name: String): Tag` — devuelve existente si ya hay uno con ese nombre, inserta si no

### ViewModel lista

`TaskListViewModel` usa `getTasksWithTags()` y agrupa:
```
Map<Tag?, List<TaskWithTag>>
```
- Tags ordenados alfabéticamente por nombre
- `null` al final (sin header en la UI)

### ViewModel formulario

`TaskFormViewModel` agrega:
- `var tagInput by mutableStateOf("")` — texto del campo
- `val tags: StateFlow<List<Tag>>` — lista completa de tags para el dropdown
- Al guardar: si `tagInput.isNotBlank()`, llama `tagRepository.save(tagInput.trim())` y usa el ID resultante

## UI

### Formulario (`TaskFormScreen`)

`ExposedDropdownMenuBox` con:
- Lista filtrada de tags existentes mientras el usuario escribe
- Opción "Crear: [texto]" cuando el texto no coincide exactamente con ningún tag existente
- Campo vacío = sin tag

### Lista (`TaskListScreen`)

```
─── Trabajo ──────────────
  [ recordatorio A ]
  [ recordatorio B ]
─── Personal ─────────────
  [ recordatorio C ]
  [ recordatorio E ]   ← sin tag, sin header
  [ recordatorio F ]
```

- Header por cada tag (nombre del tag como separador)
- Ítems sin tag al final, sin header

## Decisiones

| Decisión | Elección |
|---|---|
| Tags sin recordatorios | Persisten |
| Tags por recordatorio | Uno |
| Representación | Tabla `tags` + `tagId` en `tasks` |
| Tags duplicados | `save()` devuelve existente por nombre |
| Orden en lista | Tags alfabético, sin-tag al final |
