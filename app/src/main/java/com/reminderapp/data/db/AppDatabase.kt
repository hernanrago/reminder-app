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
                // SQLite no permite ALTER COLUMN, hay que recrear la tabla.
                // Cambia schedule de TEXT NOT NULL a TEXT (nullable).
                database.execSQL("""
                    CREATE TABLE tasks_new (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL,
                        schedule TEXT,
                        isActive INTEGER NOT NULL,
                        nextFireAtMillis INTEGER,
                        createdAtMillis INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("""
                    INSERT INTO tasks_new
                    SELECT id, title, description, schedule, isActive, nextFireAtMillis, createdAtMillis
                    FROM tasks
                """.trimIndent())
                database.execSQL("DROP TABLE tasks")
                database.execSQL("ALTER TABLE tasks_new RENAME TO tasks")
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
