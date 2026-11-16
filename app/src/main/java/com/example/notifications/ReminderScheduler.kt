package com.example.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.R
import java.util.Calendar

object ReminderScheduler {
  const val CHANNEL_ID = "toymote_daily_reminder"
  const val NOTIFICATION_ID = 2030
  private const val REQUEST_CODE = 1001

  fun ensureChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      val channel =
        NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
          )
          .apply {
            description = context.getString(R.string.notification_channel_desc)
            enableVibration(true)
          }
      manager.createNotificationChannel(channel)
    }
  }

  fun scheduleNext(context: Context, hour: Int, minute: Int) {
    ensureChannel(context)
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pendingIntent = buildPendingIntent(context)

    val now = Calendar.getInstance()
    val target =
      Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
        set(Calendar.MINUTE, minute.coerceIn(0, 59))
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (timeInMillis <= now.timeInMillis) {
          add(Calendar.DAY_OF_YEAR, 1)
        }
      }

    alarmManager.cancel(pendingIntent)
    alarmManager.setAndAllowWhileIdle(
      AlarmManager.RTC_WAKEUP,
      target.timeInMillis,
      pendingIntent,
    )
  }

  fun cancel(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarmManager.cancel(buildPendingIntent(context))
  }

  private fun buildPendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, MoodReminderReceiver::class.java)
    return PendingIntent.getBroadcast(
      context,
      REQUEST_CODE,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }
}
