package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mood_entries")
data class MoodEntryEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val dateEpochDay: Long,
  val timestampMillis: Long,
  val emotionId: String,
  val comment: String = "",
)
