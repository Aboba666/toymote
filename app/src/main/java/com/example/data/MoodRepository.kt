package com.example.data

import kotlinx.coroutines.flow.Flow

class MoodRepository(private val moodDao: MoodDao) {
  val allEntries: Flow<List<MoodEntryEntity>> = moodDao.getAllEntries()

  suspend fun addEntry(
    dateEpochDay: Long,
    emotionId: String,
    comment: String,
    timestampMillis: Long = System.currentTimeMillis(),
  ) {
    moodDao.insertEntry(
      MoodEntryEntity(
        dateEpochDay = dateEpochDay,
        timestampMillis = timestampMillis,
        emotionId = emotionId,
        comment = comment.trim(),
      )
    )
  }

  suspend fun updateEntry(entry: MoodEntryEntity) {
    moodDao.updateEntry(entry.copy(comment = entry.comment.trim()))
  }

  suspend fun deleteEntry(id: Long) {
    moodDao.deleteEntryById(id)
  }
}
