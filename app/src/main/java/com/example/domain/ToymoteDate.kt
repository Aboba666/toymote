package com.example.domain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * API-24-compatible immutable calendar date helper using UTC epoch days for DST-safe math.
 */
data class ToymoteDate(
  val year: Int,
  val month: Int, // 1..12
  val dayOfMonth: Int, // 1..31
) : Comparable<ToymoteDate> {

  val epochDay: Long
    get() {
      val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
      cal.clear()
      cal.set(year, month - 1, dayOfMonth, 0, 0, 0)
      return cal.timeInMillis / MILLIS_PER_DAY
    }

  fun daysInMonth(): Int {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    cal.clear()
    cal.set(year, month - 1, 1)
    return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
  }

  /**
   * Returns 0 for Monday, 1 for Tuesday, ..., 6 for Sunday.
   */
  fun dayOfWeekMondayZero(): Int {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    cal.clear()
    cal.set(year, month - 1, dayOfMonth)
    val dow = cal.get(Calendar.DAY_OF_WEEK) // Sun=1, Mon=2, ..., Sat=7
    return (dow + 5) % 7
  }

  fun plusMonths(delta: Int): ToymoteDate {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    cal.clear()
    cal.set(year, month - 1, 1)
    cal.add(Calendar.MONTH, delta)
    val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    return ToymoteDate(
      year = cal.get(Calendar.YEAR),
      month = cal.get(Calendar.MONTH) + 1,
      dayOfMonth = dayOfMonth.coerceAtMost(maxDay),
    )
  }

  fun plusDays(delta: Int): ToymoteDate = fromEpochDay(epochDay + delta)

  fun monthYearTitleRu(): String {
    val names =
      listOf(
        "Январь",
        "Февраль",
        "Март",
        "Апрель",
        "Май",
        "Июнь",
        "Июль",
        "Август",
        "Сентябрь",
        "Октябрь",
        "Ноябрь",
        "Декабрь",
      )
    return "${names[(month - 1).coerceIn(0, 11)]} $year"
  }

  fun shortDayMonthRu(): String {
    val namesGenitive =
      listOf(
        "янв",
        "фев",
        "мар",
        "апр",
        "мая",
        "июн",
        "июл",
        "авг",
        "сен",
        "окт",
        "ноя",
        "дек",
      )
    return "$dayOfMonth ${namesGenitive[(month - 1).coerceIn(0, 11)]}"
  }

  fun fullDateRu(): String {
    val namesGenitive =
      listOf(
        "января",
        "февраля",
        "марта",
        "апреля",
        "мая",
        "июня",
        "июля",
        "августа",
        "сентября",
        "октября",
        "ноября",
        "декабря",
      )
    return "$dayOfMonth ${namesGenitive[(month - 1).coerceIn(0, 11)]} $year"
  }

  override fun compareTo(other: ToymoteDate): Int = epochDay.compareTo(other.epochDay)

  companion object {
    private const val MILLIS_PER_DAY = 86_400_000L

    val WEEKDAYS_RU = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

    fun today(): ToymoteDate {
      val cal = Calendar.getInstance()
      return ToymoteDate(
        year = cal.get(Calendar.YEAR),
        month = cal.get(Calendar.MONTH) + 1,
        dayOfMonth = cal.get(Calendar.DAY_OF_MONTH),
      )
    }

    fun fromEpochDay(epochDay: Long): ToymoteDate {
      val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
      cal.timeInMillis = epochDay * MILLIS_PER_DAY
      return ToymoteDate(
        year = cal.get(Calendar.YEAR),
        month = cal.get(Calendar.MONTH) + 1,
        dayOfMonth = cal.get(Calendar.DAY_OF_MONTH),
      )
    }

    fun formatTimeRu(timestampMillis: Long): String {
      val fmt = SimpleDateFormat("HH:mm", Locale("ru"))
      return fmt.format(Date(timestampMillis))
    }

    fun formatDaysStreakRu(count: Int): String {
      val abs = kotlin.math.abs(count)
      val mod100 = abs % 100
      val mod10 = abs % 10
      val word =
        when {
          mod100 in 11..14 -> "дней"
          mod10 == 1 -> "день"
          mod10 in 2..4 -> "дня"
          else -> "дней"
        }
      return "$count $word подряд"
    }

    fun formatEntriesCountRu(count: Int): String {
      val abs = kotlin.math.abs(count)
      val mod100 = abs % 100
      val mod10 = abs % 10
      val word =
        when {
          mod100 in 11..14 -> "записей"
          mod10 == 1 -> "запись"
          mod10 in 2..4 -> "записи"
          else -> "записей"
        }
      return "$count $word"
    }
  }
}
