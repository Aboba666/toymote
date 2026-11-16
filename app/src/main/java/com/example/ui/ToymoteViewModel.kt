package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MoodEntryEntity
import com.example.data.MoodRepository
import com.example.data.SettingsRepository
import com.example.data.ToymoteDatabase
import com.example.data.UserSettings
import com.example.domain.AdviceCard
import com.example.domain.AnalyticsEngine
import com.example.domain.DailyMoodPoint
import com.example.domain.EmotionFrequency
import com.example.domain.MotivationalQuote
import com.example.domain.StreakStats
import com.example.domain.ToymoteDate
import com.example.notifications.ReminderScheduler
import com.example.ui.theme.ThemeMode
import com.example.widgets.ToymoteWidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class StatsPeriod(val label: String, val days: Int) {
  WEEK("Неделя", 7),
  MONTH("Месяц", 30),
}

data class ToymoteUiState(
  val currentMonthAnchor: ToymoteDate = ToymoteDate.today(),
  val entriesByDay: Map<Long, List<MoodEntryEntity>> = emptyMap(),
  val totalEntries: Int = 0,
  val streakStats: StreakStats = StreakStats(0, 0, false),
  val statsPeriod: StatsPeriod = StatsPeriod.WEEK,
  val weeklyTopEmotions: List<EmotionFrequency> = emptyList(),
  val monthlyTopEmotions: List<EmotionFrequency> = emptyList(),
  val chartPoints: List<DailyMoodPoint> = emptyList(),
  val adviceCards: List<AdviceCard> = emptyList(),
  val currentQuote: MotivationalQuote = AnalyticsEngine.quotes.first(),
  val settings: UserSettings = UserSettings(),
  val isSettingsLoaded: Boolean = false,
  val selectedDayForSheet: ToymoteDate? = null,
)

class ToymoteViewModel(application: Application) : AndroidViewModel(application) {

  private val moodRepository: MoodRepository
  private val settingsRepository: SettingsRepository

  private val monthAnchorFlow = MutableStateFlow(ToymoteDate.today())
  private val statsPeriodFlow = MutableStateFlow(StatsPeriod.WEEK)
  private val quoteIndexFlow = MutableStateFlow(0)
  private val selectedDayFlow = MutableStateFlow<ToymoteDate?>(null)
  private val settingsLoadedFlow = MutableStateFlow(false)

  init {
    val db = ToymoteDatabase.getInstance(application)
    moodRepository = MoodRepository(db.moodDao())
    settingsRepository = SettingsRepository(application)

    viewModelScope.launch {
      val initialSettings = settingsRepository.getCurrentSettings()
      settingsLoadedFlow.value = true
      if (initialSettings.reminderEnabled) {
        ReminderScheduler.scheduleNext(
          application,
          initialSettings.reminderHour,
          initialSettings.reminderMinute,
        )
      }
    }
  }

  val uiState: StateFlow<ToymoteUiState> =
    combine(
        moodRepository.allEntries,
        settingsRepository.settings.onEach { settingsLoadedFlow.value = true },
        monthAnchorFlow,
        statsPeriodFlow,
        quoteIndexFlow,
        selectedDayFlow,
        settingsLoadedFlow,
      ) { values ->
        @Suppress("UNCHECKED_CAST")
        val entries = values[0] as List<MoodEntryEntity>
        val settings = values[1] as UserSettings
        val monthAnchor = values[2] as ToymoteDate
        val period = values[3] as StatsPeriod
        val quoteIdx = values[4] as Int
        val selectedDay = values[5] as ToymoteDate?
        val loaded = values[6] as Boolean

        val today = ToymoteDate.today()
        val byDay = entries.groupBy { it.dateEpochDay }
        val streakStats = AnalyticsEngine.computeStreaks(entries, today)
        val weeklyTop = AnalyticsEngine.topEmotionsForDays(entries, 7, today)
        val monthlyTop = AnalyticsEngine.topEmotionsForDays(entries, 30, today)
        val chartPoints = AnalyticsEngine.moodChartPoints(entries, period.days, today)
        val advice = AnalyticsEngine.generateAdvice(entries, streakStats, today)
        val quote = AnalyticsEngine.quotes[quoteIdx % AnalyticsEngine.quotes.size]

        ToymoteUiState(
          currentMonthAnchor = monthAnchor,
          entriesByDay = byDay,
          totalEntries = entries.size,
          streakStats = streakStats,
          statsPeriod = period,
          weeklyTopEmotions = weeklyTop,
          monthlyTopEmotions = monthlyTop,
          chartPoints = chartPoints,
          adviceCards = advice,
          currentQuote = quote,
          settings = settings,
          isSettingsLoaded = loaded,
          selectedDayForSheet = selectedDay,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ToymoteUiState(),
      )

  fun shiftMonth(delta: Int) {
    monthAnchorFlow.update { it.plusMonths(delta) }
  }

  fun jumpToCurrentMonth() {
    monthAnchorFlow.value = ToymoteDate.today()
  }

  fun openDaySheet(date: ToymoteDate) {
    selectedDayFlow.value = date
  }

  fun closeDaySheet() {
    selectedDayFlow.value = null
  }

  fun setStatsPeriod(period: StatsPeriod) {
    statsPeriodFlow.value = period
  }

  fun nextQuote() {
    quoteIndexFlow.update { (it + 1) % AnalyticsEngine.quotes.size }
  }

  fun saveMoodEntry(
    date: ToymoteDate,
    emotionId: String,
    comment: String,
    editingEntryId: Long? = null,
  ) {
    viewModelScope.launch {
      if (editingEntryId != null && editingEntryId > 0L) {
        val existing =
          uiState.value.entriesByDay[date.epochDay]?.firstOrNull { it.id == editingEntryId }
        if (existing != null) {
          moodRepository.updateEntry(existing.copy(emotionId = emotionId, comment = comment))
          withContext(Dispatchers.IO) { ToymoteWidgetUpdater.refreshAll(getApplication()) }
          return@launch
        }
      }
      moodRepository.addEntry(
        dateEpochDay = date.epochDay,
        emotionId = emotionId,
        comment = comment,
      )
      withContext(Dispatchers.IO) { ToymoteWidgetUpdater.refreshAll(getApplication()) }
    }
  }

  fun deleteEntry(entryId: Long) {
    viewModelScope.launch {
      moodRepository.deleteEntry(entryId)
      withContext(Dispatchers.IO) { ToymoteWidgetUpdater.refreshAll(getApplication()) }
    }
  }

  fun updateReminderEnabled(enabled: Boolean) {
    viewModelScope.launch {
      settingsRepository.setReminderEnabled(enabled)
      val current = settingsRepository.getCurrentSettings()
      if (enabled) {
        ReminderScheduler.scheduleNext(
          getApplication(),
          current.reminderHour,
          current.reminderMinute,
        )
      } else {
        ReminderScheduler.cancel(getApplication())
      }
    }
  }

  fun updateReminderTime(hour: Int, minute: Int) {
    viewModelScope.launch {
      settingsRepository.setReminderTime(hour, minute)
      settingsRepository.setReminderEnabled(true)
      ReminderScheduler.scheduleNext(getApplication(), hour, minute)
    }
  }

  fun updateThemeMode(mode: ThemeMode) {
    viewModelScope.launch { settingsRepository.setThemeMode(mode) }
  }

  fun markWidgetPromptShown() {
    viewModelScope.launch { settingsRepository.setWidgetPromptShown(true) }
  }
}
