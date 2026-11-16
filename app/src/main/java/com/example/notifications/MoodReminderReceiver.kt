package com.example.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MoodReminderReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    ReminderScheduler.ensureChannel(context)

    val canPost =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
          PackageManager.PERMISSION_GRANTED
      } else {
        true
      }

    if (canPost) {
      val tapIntent =
        Intent(context, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
      val tapPendingIntent =
        PendingIntent.getActivity(
          context,
          0,
          tapIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

      val notification =
        NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
          .setSmallIcon(R.mipmap.ic_launcher_round)
          .setContentTitle(context.getString(R.string.notification_title))
          .setContentText(context.getString(R.string.notification_body))
          .setStyle(
            NotificationCompat.BigTextStyle()
              .bigText(context.getString(R.string.notification_body))
          )
          .setPriority(NotificationCompat.PRIORITY_DEFAULT)
          .setContentIntent(tapPendingIntent)
          .setAutoCancel(true)
          .build()

      try {
        NotificationManagerCompat.from(context)
          .notify(ReminderScheduler.NOTIFICATION_ID, notification)
      } catch (_: SecurityException) {
        // Permission revoked between check and notify
      }
    }

    val pendingResult = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val settings = SettingsRepository(context).getCurrentSettings()
        if (settings.reminderEnabled) {
          ReminderScheduler.scheduleNext(context, settings.reminderHour, settings.reminderMinute)
        }
      } finally {
        pendingResult.finish()
      }
    }
  }
}
