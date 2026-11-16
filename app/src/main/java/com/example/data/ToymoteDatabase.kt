package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [MoodEntryEntity::class], version = 1, exportSchema = false)
abstract class ToymoteDatabase : RoomDatabase() {
  abstract fun moodDao(): MoodDao

  companion object {
    @Volatile
    private var INSTANCE: ToymoteDatabase? = null

    fun getInstance(context: Context): ToymoteDatabase {
      return INSTANCE
        ?: synchronized(this) {
          val instance =
            Room.databaseBuilder(
                context.applicationContext,
                ToymoteDatabase::class.java,
                "toymote_mood_db",
              )
              .build()
          INSTANCE = instance
          instance
        }
    }
  }
}
