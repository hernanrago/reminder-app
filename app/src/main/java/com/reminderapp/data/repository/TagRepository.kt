package com.reminderapp.data.repository

import android.content.Context
import com.reminderapp.data.db.AppDatabase
import com.reminderapp.data.model.Tag
import kotlinx.coroutines.flow.Flow

class TagRepository(context: Context) {
    private val dao = AppDatabase.getInstance(context).tagDao()

    fun getAll(): Flow<List<Tag>> = dao.getAll()

    // Devuelve el tag existente si ya hay uno con ese nombre, o inserta uno nuevo.
    suspend fun save(name: String): Tag {
        val existing = dao.getByName(name)
        if (existing != null) return existing
        val id = dao.insert(Tag(name = name))
        return Tag(id = id.toInt(), name = name)
    }
}
