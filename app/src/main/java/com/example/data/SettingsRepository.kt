package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.toymoteDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "toymote_settings")

data class UserSettings(
  val reminderEnabled: Boolean = true,
  val reminderHour: Int = 20,
  val reminderMinute: Int = 30,
  val themeMode: ThemeMode = ThemeMode.SYSTEM,
  val widgetPromptShown: Boolean = false,
)

class SettingsRepository(private val context: Context) {
  private object Keys {
    val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
    val REMINDER_HOUR = intPreferencesKey("reminder_hour")
    val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val WIDGET_PROMPT_SHOWN = booleanPreferencesKey("widget_prompt_shown")
  }

  val settings: Flow<UserSettings> =
    context.toymoteDataStore.data.map { prefs ->
      val modeName = prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
      val parsedMode =
        ThemeMode.entries.firstOrNull { it.name == modeName } ?: ThemeMode.SYSTEM
      UserSettings(
        reminderEnabled = prefs[Keys.REMINDER_ENABLED] ?: true,
        reminderHour = (prefs[Keys.REMINDER_HOUR] ?: 20).coerceIn(0, 23),
        reminderMinute = (prefs[Keys.REMINDER_MINUTE] ?: 30).coerceIn(0, 59),
        themeMode = parsedMode,
        widgetPromptShown = prefs[Keys.WIDGET_PROMPT_SHOWN] ?: false,
      )
    }

  suspend fun getCurrentSettings(): UserSettings = settings.first()

  suspend fun setReminderEnabled(enabled: Boolean) {
    context.toymoteDataStore.edit { prefs -> prefs[Keys.REMINDER_ENABLED] = enabled }
  }

  suspend fun setReminderTime(hour: Int, minute: Int) {
    context.toymoteDataStore.edit { prefs ->
      prefs[Keys.REMINDER_HOUR] = hour.coerceIn(0, 23)
      prefs[Keys.REMINDER_MINUTE] = minute.coerceIn(0, 59)
    }
  }

  suspend fun setThemeMode(mode: ThemeMode) {
    context.toymoteDataStore.edit { prefs -> prefs[Keys.THEME_MODE] = mode.name }
  }

  suspend fun setWidgetPromptShown(shown: Boolean) {
    context.toymoteDataStore.edit { prefs -> prefs[Keys.WIDGET_PROMPT_SHOWN] = shown }
  }
}
