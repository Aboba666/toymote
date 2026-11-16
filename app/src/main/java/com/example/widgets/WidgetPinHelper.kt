package com.example.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build

enum class PinWidgetOutcome {
  PIN_DIALOG_LAUNCHED,
  MANUAL_FALLBACK_NEEDED,
}

object WidgetPinHelper {

  fun requestPinWidget(context: Context, kind: ToymoteWidgetKind): PinWidgetOutcome {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      return PinWidgetOutcome.MANUAL_FALLBACK_NEEDED
    }
    val manager = AppWidgetManager.getInstance(context)
    if (!manager.isRequestPinAppWidgetSupported) {
      return PinWidgetOutcome.MANUAL_FALLBACK_NEEDED
    }

    val providerClass =
      when (kind) {
        ToymoteWidgetKind.STREAK -> StreakWidgetProvider::class.java
        ToymoteWidgetKind.CALENDAR -> MoodCalendarWidgetProvider::class.java
        ToymoteWidgetKind.COMBO -> ComboMoodWidgetProvider::class.java
      }
    val componentName = ComponentName(context, providerClass)
    val callbackIntent =
      Intent(context, providerClass).apply {
        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
      }
    val successCallback =
      PendingIntent.getBroadcast(
        context,
        kind.ordinal + 500,
        callbackIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val requested =
      try {
        manager.requestPinAppWidget(componentName, null, successCallback)
      } catch (_: Exception) {
        false
      }
    return if (requested) {
      PinWidgetOutcome.PIN_DIALOG_LAUNCHED
    } else {
      PinWidgetOutcome.MANUAL_FALLBACK_NEEDED
    }
  }
}
