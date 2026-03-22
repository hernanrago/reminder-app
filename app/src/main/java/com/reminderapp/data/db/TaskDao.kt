package com.reminderapp.data.db

import androidx.room.*
import com.reminderapp.data.model.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY isActive DESC, nextFireAtMillis ASC")
    fun getAll(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isActive = 1")
    fun getAllActive(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Int): Task?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Transaction
    @Query("SELECT * FROM tasks")
    fun getTasksWithTags(): Flow<List<TaskWithTag>>
}
