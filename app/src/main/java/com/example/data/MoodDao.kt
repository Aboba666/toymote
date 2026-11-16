package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MoodDao {
  @Query("SELECT * FROM mood_entries ORDER BY dateEpochDay DESC, timestampMillis DESC")
  fun getAllEntries(): Flow<List<MoodEntryEntity>>

  @Query("SELECT * FROM mood_entries ORDER BY dateEpochDay DESC, timestampMillis DESC")
  suspend fun getAllEntriesOnce(): List<MoodEntryEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertEntry(entry: MoodEntryEntity): Long

  @Update
  suspend fun updateEntry(entry: MoodEntryEntity)

  @Query("DELETE FROM mood_entries WHERE id = :id")
  suspend fun deleteEntryById(id: Long)
}
