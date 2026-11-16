package com.example

import com.example.data.MoodEntryEntity
import com.example.domain.AnalyticsEngine
import com.example.domain.EmotionCatalog
import com.example.domain.EmotionCategory
import com.example.domain.ToymoteDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun `catalog includes all 9 signature faces and 9 vector faces`() {
    assertEquals(18, EmotionCatalog.all.size)
    assertEquals(9, EmotionCatalog.filterBy(EmotionCategory.SIGNATURE).size)
  }

  @Test
  fun `streak counts unique consecutive days including multiple entries per day`() {
    val today = ToymoteDate(2026, 11, 16)
    val todayEpoch = today.epochDay
    val entries =
      listOf(
        MoodEntryEntity(id = 1, dateEpochDay = todayEpoch, timestampMillis = 100L, emotionId = "kaifyush"),
        MoodEntryEntity(id = 2, dateEpochDay = todayEpoch, timestampMillis = 200L, emotionId = "spokush"),
        MoodEntryEntity(id = 3, dateEpochDay = todayEpoch - 1, timestampMillis = 100L, emotionId = "sleinyush"),
        MoodEntryEntity(id = 4, dateEpochDay = todayEpoch - 2, timestampMillis = 100L, emotionId = "veselyi"),
        // gap at todayEpoch - 3
        MoodEntryEntity(id = 5, dateEpochDay = todayEpoch - 4, timestampMillis = 100L, emotionId = "grustyush"),
        MoodEntryEntity(id = 6, dateEpochDay = todayEpoch - 5, timestampMillis = 100L, emotionId = "ustalyi"),
        MoodEntryEntity(id = 7, dateEpochDay = todayEpoch - 6, timestampMillis = 100L, emotionId = "skuchnyush"),
        MoodEntryEntity(id = 8, dateEpochDay = todayEpoch - 7, timestampMillis = 100L, emotionId = "pohuyush"),
      )

    val stats = AnalyticsEngine.computeStreaks(entries, today)
    assertEquals(3, stats.currentStreak)
    assertEquals(4, stats.longestStreak)
    assertTrue(stats.hasEntryToday)
  }

  @Test
  fun `streak continues from yesterday before today is logged`() {
    val today = ToymoteDate(2026, 11, 16)
    val todayEpoch = today.epochDay
    val entries =
      listOf(
        MoodEntryEntity(id = 1, dateEpochDay = todayEpoch - 1, timestampMillis = 100L, emotionId = "kaifyush"),
        MoodEntryEntity(id = 2, dateEpochDay = todayEpoch - 2, timestampMillis = 100L, emotionId = "spokush"),
      )

    val stats = AnalyticsEngine.computeStreaks(entries, today)
    assertEquals(2, stats.currentStreak)
    assertFalse(stats.hasEntryToday)
  }

  @Test
  fun `russian streak pluralization works accurately`() {
    assertEquals("0 дней подряд", ToymoteDate.formatDaysStreakRu(0))
    assertEquals("1 день подряд", ToymoteDate.formatDaysStreakRu(1))
    assertEquals("2 дня подряд", ToymoteDate.formatDaysStreakRu(2))
    assertEquals("5 дней подряд", ToymoteDate.formatDaysStreakRu(5))
    assertEquals("11 дней подряд", ToymoteDate.formatDaysStreakRu(11))
    assertEquals("21 день подряд", ToymoteDate.formatDaysStreakRu(21))
  }
}
