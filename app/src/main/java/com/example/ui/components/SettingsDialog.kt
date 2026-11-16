package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import com.example.R
import com.example.data.UserSettings
import com.example.ui.components.MattoCompanyLogo
import com.example.ui.theme.ThemeMode
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
  settings: UserSettings,
  onDismiss: () -> Unit,
  onToggleReminder: (Boolean) -> Unit,
  onSelectReminderTime: (Int, Int) -> Unit,
  onSelectThemeMode: (ThemeMode) -> Unit,
) {
  val context = LocalContext.current
  var showTimePicker by remember { mutableStateOf(false) }

  val notificationPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      if (granted) {
        onToggleReminder(true)
      } else {
        onToggleReminder(false)
      }
    }

  val requestNotificationAndEnable = {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
          PackageManager.PERMISSION_GRANTED
      if (granted) {
        onToggleReminder(true)
      } else {
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    } else {
      onToggleReminder(true)
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "Настройки",
        style = MaterialTheme.typography.titleLarge,
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        // Evening notification card
        Surface(
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          shape = MaterialTheme.shapes.medium,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
              ) {
                Icon(
                  imageVector = Icons.Rounded.NotificationsActive,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(
                    text = "Вечернее напоминание",
                    style = MaterialTheme.typography.titleSmall,
                  )
                  Text(
                    text = "«Какое у тебя настроение сегодня?»",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
              Switch(
                checked = settings.reminderEnabled,
                onCheckedChange = { checked ->
                  if (checked) {
                    requestNotificationAndEnable()
                  } else {
                    onToggleReminder(false)
                  }
                },
                modifier = Modifier.testTag("reminder_switch"),
              )
            }

            if (settings.reminderEnabled) {
              val formattedTime =
                String.format(
                  Locale.getDefault(),
                  "%02d:%02d",
                  settings.reminderHour,
                  settings.reminderMinute,
                )
              OutlinedButton(
                onClick = { showTimePicker = true },
                modifier = Modifier.fillMaxWidth().testTag("reminder_time_button"),
              ) {
                Icon(imageVector = Icons.Rounded.Schedule, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Время уведомления: $formattedTime")
              }
            }
          }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Theme selection
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Rounded.Palette,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Тема оформления", style = MaterialTheme.typography.titleSmall)
          }
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ThemeMode.entries.forEach { mode ->
              FilterChip(
                selected = settings.themeMode == mode,
                onClick = { onSelectThemeMode(mode) },
                label = { Text(text = mode.label) },
                modifier = Modifier.fillMaxWidth().testTag("theme_chip_${mode.name}"),
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(4.dp))
        MattoCompanyLogo(compact = true)
        Text(
          text = "${stringResource(id = R.string.brand_signature)} • Версия ${BuildConfig.VERSION_NAME}",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    },
    confirmButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("settings_done_button"),
      ) {
        Text("Готово")
      }
    },
  )

  if (showTimePicker) {
    val timePickerState =
      rememberTimePickerState(
        initialHour = settings.reminderHour,
        initialMinute = settings.reminderMinute,
        is24Hour = true,
      )
    AlertDialog(
      onDismissRequest = { showTimePicker = false },
      title = { Text("Выберите время") },
      text = { TimePicker(state = timePickerState) },
      confirmButton = {
        TextButton(
          onClick = {
            onSelectReminderTime(timePickerState.hour, timePickerState.minute)
            showTimePicker = false
          },
          modifier = Modifier.testTag("confirm_time_button"),
        ) {
          Text("Сохранить")
        }
      },
      dismissButton = {
        TextButton(onClick = { showTimePicker = false }) { Text("Отмена") }
      },
    )
  }
}
