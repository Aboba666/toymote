package com.example.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.MoodEntryEntity
import com.example.data.ToymoteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

enum class ToymoteWidgetKind {
  STREAK,
  CALENDAR,
  COMBO,
}

abstract class BaseToymoteWidgetProvider(
  private val kind: ToymoteWidgetKind,
  private val layoutRes: Int,
  private val defaultWidthDp: Int,
  private val defaultHeightDp: Int,
) : AppWidgetProvider() {

  override fun onUpdate(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray,
  ) {
    val pendingResult = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
      try {
        val entries = ToymoteDatabase.getInstance(context).moodDao().getAllEntriesOnce()
        for (appWidgetId in appWidgetIds) {
          val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
          renderAndPush(context, appWidgetManager, appWidgetId, options, entries)
        }
      } finally {
        pendingResult.finish()
      }
    }
  }

  override fun onAppWidgetOptionsChanged(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    newOptions: Bundle,
  ) {
    val pendingResult = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
      try {
        val entries = ToymoteDatabase.getInstance(context).moodDao().getAllEntriesOnce()
        renderAndPush(context, appWidgetManager, appWidgetId, newOptions, entries)
      } finally {
        pendingResult.finish()
      }
    }
  }

  internal fun renderAndPush(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    options: Bundle?,
    entries: List<MoodEntryEntity>,
  ) {
    val minW = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0) ?: 0
    val minH = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) ?: 0
    val widthDp = if (minW > 0) minW else defaultWidthDp
    val heightDp = if (minH > 0) minH else defaultHeightDp

    val bitmap: Bitmap =
      when (kind) {
        ToymoteWidgetKind.STREAK ->
          ToymoteWidgetRenderer.renderStreakWidget(context, entries, widthDp, heightDp)
        ToymoteWidgetKind.CALENDAR ->
          ToymoteWidgetRenderer.renderCalendarWidget(context, entries, widthDp, heightDp)
        ToymoteWidgetKind.COMBO ->
          ToymoteWidgetRenderer.renderComboWidget(context, entries, widthDp, heightDp)
      }

    val launchIntent =
      Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
    val pendingIntent =
      PendingIntent.getActivity(
        context,
        appWidgetId,
        launchIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val views = RemoteViews(context.packageName, layoutRes)
    views.setImageViewBitmap(R.id.widget_image, bitmap)
    views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
    appWidgetManager.updateAppWidget(appWidgetId, views)
  }
}

class StreakWidgetProvider :
  BaseToymoteWidgetProvider(
    kind = ToymoteWidgetKind.STREAK,
    layoutRes = R.layout.widget_streak,
    defaultWidthDp = 160,
    defaultHeightDp = 76,
  )

class MoodCalendarWidgetProvider :
  BaseToymoteWidgetProvider(
    kind = ToymoteWidgetKind.CALENDAR,
    layoutRes = R.layout.widget_calendar,
    defaultWidthDp = 280,
    defaultHeightDp = 190,
  )

class ComboMoodWidgetProvider :
  BaseToymoteWidgetProvider(
    kind = ToymoteWidgetKind.COMBO,
    layoutRes = R.layout.widget_combo,
    defaultWidthDp = 290,
    defaultHeightDp = 230,
  )

object ToymoteWidgetUpdater {
  suspend fun refreshAll(context: Context) {
    val appContext = context.applicationContext
    val manager = AppWidgetManager.getInstance(appContext)
    val entries = ToymoteDatabase.getInstance(appContext).moodDao().getAllEntriesOnce()

    val providers =
      listOf(
        StreakWidgetProvider(),
        MoodCalendarWidgetProvider(),
        ComboMoodWidgetProvider(),
      )

    providers.forEach { provider ->
      val ids = manager.getAppWidgetIds(ComponentName(appContext, provider::class.java))
      ids.forEach { id ->
        val options = manager.getAppWidgetOptions(id)
        provider.renderAndPush(appContext, manager, id, options, entries)
      }
    }
  }
}
