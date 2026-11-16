package com.example.widgets

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.MoodEntryEntity
import com.example.domain.AnalyticsEngine
import com.example.domain.EmotionCatalog
import com.example.domain.ToymoteDate
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private data class WidgetPalette(
  val isDark: Boolean,
  val cardBgTop: Int,
  val cardBgBottom: Int,
  val cardBorder: Int,
  val innerSurface: Int,
  val textPrimary: Int,
  val textSecondary: Int,
  val accentPrimary: Int,
  val accentSecondary: Int,
  val accentContainer: Int,
  val onAccentContainer: Int,
  val flameOuter: Int,
  val flameInner: Int,
  val emptyCellBg: Int,
)

object ToymoteWidgetRenderer {

  private fun palette(context: Context): WidgetPalette {
    val nightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    val isDark = nightMode == Configuration.UI_MODE_NIGHT_YES
    return if (isDark) {
      WidgetPalette(
        isDark = true,
        cardBgTop = 0xFF1A1429.toInt(),
        cardBgBottom = 0xFF120E1D.toInt(),
        cardBorder = 0xFF392D57.toInt(),
        innerSurface = 0xFF241C38.toInt(),
        textPrimary = 0xFFF4EFFF.toInt(),
        textSecondary = 0xFFB8AED6.toInt(),
        accentPrimary = 0xFF9F75FF.toInt(),
        accentSecondary = 0xFF7B4DFF.toInt(),
        accentContainer = 0xFF33235C.toInt(),
        onAccentContainer = 0xFFEBDCFF.toInt(),
        flameOuter = 0xFFFF7A33.toInt(),
        flameInner = 0xFFFFD166.toInt(),
        emptyCellBg = 0xFF28203E.toInt(),
      )
    } else {
      WidgetPalette(
        isDark = false,
        cardBgTop = 0xFFFFFFFF.toInt(),
        cardBgBottom = 0xFFF4EEFF.toInt(),
        cardBorder = 0xFFDFD2FF.toInt(),
        innerSurface = 0xFFF7F2FF.toInt(),
        textPrimary = 0xFF1D1333.toInt(),
        textSecondary = 0xFF6B5D8E.toInt(),
        accentPrimary = 0xFF6E38F5.toInt(),
        accentSecondary = 0xFF9E77FF.toInt(),
        accentContainer = 0xFFEDE4FF.toInt(),
        onAccentContainer = 0xFF4218A6.toInt(),
        flameOuter = 0xFFFF6B2B.toInt(),
        flameInner = 0xFFFFD056.toInt(),
        emptyCellBg = 0xFFF0E9FF.toInt(),
      )
    }
  }

  private fun font(context: Context, bold: Boolean = false): Typeface {
    val base = ResourcesCompat.getFont(context, R.font.montserrat) ?: Typeface.SANS_SERIF
    return if (bold) Typeface.create(base, Typeface.BOLD) else base
  }

  private fun createSafeCanvas(widthDp: Int, heightDp: Int): Pair<Bitmap, Canvas> {
    val safeWdp = widthDp.coerceIn(105, 460)
    val safeHdp = heightDp.coerceIn(44, 460)
    val maxPixels = 165_000f
    var scale = 2.0f
    while ((safeWdp * scale) * (safeHdp * scale) > maxPixels && scale > 1.0f) {
      scale -= 0.1f
    }
    val pxW = (safeWdp * scale).roundToInt().coerceIn(200, 520)
    val pxH = (safeHdp * scale).roundToInt().coerceIn(88, 520)
    val bmp = Bitmap.createBitmap(pxW, pxH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    return bmp to canvas
  }

  private fun drawCardBackground(canvas: Canvas, pal: WidgetPalette, cornerRadius: Float) {
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()
    val strokeW = max(2f, min(w, h) * 0.012f)
    val inset = strokeW / 2f
    val rect = RectF(inset, inset, w - inset, h - inset)
    val bgPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(0f, 0f, w, h, pal.cardBgTop, pal.cardBgBottom, Shader.TileMode.CLAMP)
      }
    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)

    val borderPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeW
        color = pal.cardBorder
      }
    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)
  }

  private fun drawEllipsizedText(
    canvas: Canvas,
    text: String,
    x: Float,
    baselineY: Float,
    maxWidth: Float,
    paint: Paint,
  ) {
    if (maxWidth <= 4f) return
    val tp = TextPaint(paint)
    val fitted = TextUtils.ellipsize(text, tp, maxWidth, TextUtils.TruncateAt.END).toString()
    canvas.drawText(fitted, x, baselineY, paint)
  }

  private fun daysWordShortRu(count: Int): String {
    val abs = kotlin.math.abs(count)
    val mod100 = abs % 100
    val mod10 = abs % 10
    return when {
      mod100 in 11..14 -> "дней"
      mod10 == 1 -> "день"
      mod10 in 2..4 -> "дня"
      else -> "дней"
    }
  }

  // ============================================================================================
  // WIDGET 1: STREAK WIDGET
  // ============================================================================================
  fun renderStreakWidget(
    context: Context,
    entries: List<MoodEntryEntity>,
    widthDp: Int,
    heightDp: Int,
  ): Bitmap {
    val pal = palette(context)
    val (bmp, canvas) = createSafeCanvas(widthDp, heightDp)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()
    val today = ToymoteDate.today()
    val streak = AnalyticsEngine.computeStreaks(entries, today)
    val todayEntry =
      entries.filter { it.dateEpochDay == today.epochDay }.maxByOrNull { it.timestampMillis }
    val todayEmotion = todayEntry?.let { EmotionCatalog.findById(it.emotionId) }

    val corner = min(w, h) * 0.22f
    drawCardBackground(canvas, pal, corner.coerceIn(18f, 44f))

    val boldTf = font(context, bold = true)
    val regTf = font(context, bold = false)

    val isCompact2x1 = widthDp < 225 && heightDp < 115
    val isTall = heightDp >= 120 && widthDp >= 150

    if (isCompact2x1) {
      // Crisp 2x1: Large purple flame badge + big streak digit + "дн." + large today face
      val pad = h * 0.14f
      val badgeSize = (h - pad * 2f).coerceAtMost(w * 0.27f)
      val badgeTop = (h - badgeSize) / 2f
      val flameBox = RectF(pad, badgeTop, pad + badgeSize, badgeTop + badgeSize)
      val pillPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          shader =
            LinearGradient(
              flameBox.left,
              flameBox.top,
              flameBox.right,
              flameBox.bottom,
              pal.accentPrimary,
              pal.accentSecondary,
              Shader.TileMode.CLAMP,
            )
        }
      canvas.drawRoundRect(flameBox, badgeSize * 0.30f, badgeSize * 0.30f, pillPaint)

      val fInset = badgeSize * 0.19f
      WidgetFaceCanvas.drawFlame(
        canvas,
        RectF(flameBox.left + fInset, flameBox.top + fInset, flameBox.right - fInset, flameBox.bottom - fInset),
        pal.flameOuter,
        pal.flameInner,
      )

      val faceSize = badgeSize
      val faceRect = RectF(w - pad - faceSize, badgeTop, w - pad, badgeTop + faceSize)
      if (todayEmotion != null) {
        WidgetFaceCanvas.drawEmotionBadge(context, canvas, todayEmotion, faceRect)
      } else {
        drawEmptyFaceSlot(canvas, faceRect, pal)
      }

      // Centered big streak digit + short "дней" label cleanly bounded between left & right badges
      val centerAreaLeft = flameBox.right + pad * 0.4f
      val centerAreaRight = faceRect.left - pad * 0.4f
      val centerCx = (centerAreaLeft + centerAreaRight) / 2f
      val maxTextW = (centerAreaRight - centerAreaLeft).coerceAtLeast(10f)

      val numPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          typeface = boldTf
          color = pal.textPrimary
          textSize = (h * 0.46f).coerceAtMost(maxTextW * 0.65f)
          textAlign = Paint.Align.CENTER
        }
      val subPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          typeface = boldTf
          color = pal.textSecondary
          textSize = (h * 0.18f).coerceAtMost(maxTextW * 0.28f)
          textAlign = Paint.Align.CENTER
        }

      canvas.drawText("${streak.currentStreak}", centerCx, h * 0.56f, numPaint)
      canvas.drawText(daysWordShortRu(streak.currentStreak), centerCx, h * 0.81f, subPaint)
    } else if (isTall) {
      // Square / Tall 2x2 or larger
      val pad = min(w, h) * 0.10f
      val tagPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          typeface = boldTf
          color = pal.accentPrimary
          textSize = (min(w, h) * 0.088f).coerceIn(14f, 22f)
        }
      drawEllipsizedText(canvas, "УДАРНАЯ СЕРИЯ", pad, pad + tagPaint.textSize, w - pad * 2f, tagPaint)

      val badgeSize = min(w, h) * 0.36f
      val badgeTop = h * 0.25f
      val flameBox = RectF(pad, badgeTop, pad + badgeSize, badgeTop + badgeSize)
      val gradPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          shader =
            LinearGradient(
              flameBox.left,
              flameBox.top,
              flameBox.right,
              flameBox.bottom,
              pal.accentPrimary,
              pal.accentSecondary,
              Shader.TileMode.CLAMP,
            )
        }
      canvas.drawRoundRect(flameBox, badgeSize * 0.28f, badgeSize * 0.28f, gradPaint)
      val fPad = badgeSize * 0.20f
      WidgetFaceCanvas.drawFlame(
        canvas,
        RectF(flameBox.left + fPad, flameBox.top + fPad, flameBox.right - fPad, flameBox.bottom - fPad),
        pal.flameOuter,
        pal.flameInner,
      )

      val faceSize = badgeSize * 0.94f
      val faceRect =
        RectF(
          w - pad - faceSize,
          flameBox.centerY() - faceSize / 2f,
          w - pad,
          flameBox.centerY() + faceSize / 2f,
        )
      if (todayEmotion != null) {
        WidgetFaceCanvas.drawEmotionBadge(context, canvas, todayEmotion, faceRect)
      } else {
        drawEmptyFaceSlot(canvas, faceRect, pal)
      }

      val bigNumPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          typeface = boldTf
          color = pal.textPrimary
          textSize = (min(w, h) * 0.165f).coerceIn(22f, 44f)
        }
      drawEllipsizedText(
        canvas,
        ToymoteDate.formatDaysStreakRu(streak.currentStreak),
        pad,
        h * 0.79f,
        w - pad * 2f,
        bigNumPaint,
      )
      val subPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          typeface = regTf
          color = pal.textSecondary
          textSize = (min(w, h) * 0.088f).coerceIn(14f, 22f)
        }
      val status =
        if (todayEmotion != null) "Сегодня: ${todayEmotion.name} • Рекорд ${streak.longestStreak}"
        else "Отметь сегодня • Рекорд ${streak.longestStreak}"
      drawEllipsizedText(canvas, status, pad, h * 0.91f, w - pad * 2f, subPaint)
    } else {
      // Wide 3x1 / 4x1
      val pad = h * 0.14f
      val flameSize = h - pad * 2f
      val flameBox = RectF(pad, pad, pad + flameSize, pad + flameSize)
      val gradPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          shader =
            LinearGradient(
              flameBox.left,
              flameBox.top,
              flameBox.right,
              flameBox.bottom,
              pal.accentPrimary,
              pal.accentSecondary,
              Shader.TileMode.CLAMP,
            )
        }
      canvas.drawRoundRect(flameBox, flameSize * 0.28f, flameSize * 0.28f, gradPaint)
      val fPad = flameSize * 0.19f
      WidgetFaceCanvas.drawFlame(
        canvas,
        RectF(flameBox.left + fPad, flameBox.top + fPad, flameBox.right - fPad, flameBox.bottom - fPad),
        pal.flameOuter,
        pal.flameInner,
      )

      val faceSize = h - pad * 2f
      val faceRect = RectF(w - pad - faceSize, (h - faceSize) / 2f, w - pad, (h + faceSize) / 2f)
      if (todayEmotion != null) {
        WidgetFaceCanvas.drawEmotionBadge(context, canvas, todayEmotion, faceRect)
      } else {
        drawEmptyFaceSlot(canvas, faceRect, pal)
      }

      val textLeft = flameBox.right + pad * 0.85f
      val maxTextW = (faceRect.left - textLeft - pad * 0.6f).coerceAtLeast(16f)
      val titlePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          typeface = boldTf
          color = pal.textPrimary
          textSize = h * 0.27f
        }
      val subPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          typeface = regTf
          color = pal.textSecondary
          textSize = h * 0.155f
        }
      drawEllipsizedText(
        canvas,
        ToymoteDate.formatDaysStreakRu(streak.currentStreak),
        textLeft,
        h * 0.45f,
        maxTextW,
        titlePaint,
      )
      val subText =
        if (todayEmotion != null) "Сегодня: ${todayEmotion.name} • Рекорд ${streak.longestStreak}"
        else "Отметь настроение • Рекорд ${streak.longestStreak}"
      drawEllipsizedText(canvas, subText, textLeft, h * 0.72f, maxTextW, subPaint)
    }

    return bmp
  }

  // ============================================================================================
  // WIDGET 2: MOOD CALENDAR WIDGET
  // ============================================================================================
  fun renderCalendarWidget(
    context: Context,
    entries: List<MoodEntryEntity>,
    widthDp: Int,
    heightDp: Int,
  ): Bitmap {
    val pal = palette(context)
    val (bmp, canvas) = createSafeCanvas(widthDp, heightDp)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()
    val today = ToymoteDate.today()
    val byDay = entries.groupBy { it.dateEpochDay }

    drawCardBackground(canvas, pal, min(w, h) * 0.14f.coerceAtMost(36f))

    val boldTf = font(context, bold = true)
    val regTf = font(context, bold = false)

    val isCompact2x1 = widthDp < 225 && heightDp < 115

    if (isCompact2x1) {
      // Crisp 2x1 Calendar: Large faces for last 4 days (or 3/5 depending on width) + minimal day digits
      val dayCount =
        when {
          widthDp < 140 -> 3
          widthDp >= 185 -> 5
          else -> 4
        }
      val pad = min(w, h) * 0.11f
      drawRecentLargeFacesRow(
        context = context,
        canvas = canvas,
        pal = pal,
        today = today,
        byDay = byDay,
        area = RectF(pad, pad, w - pad, h - pad),
        dayCount = dayCount,
        boldTf = boldTf,
        regTf = regTf,
      )
    } else if (heightDp < 150) {
      // Wide 3x1 / 4x1: Clean 7-day strip (Mon..Sun of current week)
      val pad = min(w, h) * 0.08f
      val titleSize = (h * 0.145f).coerceIn(15f, 26f)
      val titlePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          typeface = boldTf
          color = pal.textPrimary
          textSize = titleSize
        }
      val brandPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          typeface = boldTf
          color = pal.accentPrimary
          textSize = titleSize * 0.85f
          textAlign = Paint.Align.RIGHT
        }
      canvas.drawText("toymote", w - pad, pad + titleSize, brandPaint)
      val brandW = brandPaint.measureText("toymote") + pad
      drawEllipsizedText(
        canvas,
        today.monthYearTitleRu(),
        pad,
        pad + titleSize,
        (w - pad * 2f - brandW).coerceAtLeast(20f),
        titlePaint,
      )

      drawWeekStrip(
        context = context,
        canvas = canvas,
        pal = pal,
        today = today,
        byDay = byDay,
        area = RectF(pad, pad + titleSize + h * 0.06f, w - pad, h - pad),
        boldTf = boldTf,
        regTf = regTf,
      )
    } else if (widthDp < 215) {
      // Square / Narrow tall 2x2: Header + 2-week grid
      val pad = min(w, h) * 0.08f
      val titleSize = (min(w, h) * 0.095f).coerceIn(16f, 26f)
      val titlePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          typeface = boldTf
          color = pal.textPrimary
          textSize = titleSize
        }
      drawEllipsizedText(canvas, today.monthYearTitleRu(), pad, pad + titleSize, w - pad * 2f, titlePaint)
      drawTwoWeekGrid(
        context = context,
        canvas = canvas,
        pal = pal,
        today = today,
        byDay = byDay,
        area = RectF(pad, pad + titleSize + h * 0.05f, w - pad, h - pad),
        boldTf = boldTf,
        regTf = regTf,
      )
    } else {
      // Full month grid
      val pad = min(w, h) * 0.065f
      val headerSize = (h * 0.092f).coerceIn(18f, 34f)
      val titlePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          typeface = boldTf
          color = pal.textPrimary
          textSize = headerSize
        }
      val monthDays =
        (1..today.daysInMonth()).count { d ->
          val epoch = ToymoteDate(today.year, today.month, d).epochDay
          byDay[epoch]?.isNotEmpty() == true
        }
      val countStr = "$monthDays дн."
      val countPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          typeface = boldTf
          color = pal.accentPrimary
          textSize = headerSize * 0.78f
          textAlign = Paint.Align.RIGHT
        }
      canvas.drawText(countStr, w - pad, pad + headerSize, countPaint)
      val rightW = countPaint.measureText(countStr) + pad
      drawEllipsizedText(
        canvas,
        today.monthYearTitleRu(),
        pad,
        pad + headerSize,
        (w - pad * 2f - rightW).coerceAtLeast(24f),
        titlePaint,
      )

      val calTop = pad + headerSize + h * 0.045f
      drawMonthCalendarGrid(
        context = context,
        canvas = canvas,
        pal = pal,
        today = today,
        byDay = byDay,
        area = RectF(pad, calTop, w - pad, h - pad),
        boldTf = boldTf,
        regTf = regTf,
      )
    }

    return bmp
  }

  // ============================================================================================
  // WIDGET 3: COMBO WIDGET (STREAK + MOOD CALENDAR)
  // ============================================================================================
  fun renderComboWidget(
    context: Context,
    entries: List<MoodEntryEntity>,
    widthDp: Int,
    heightDp: Int,
  ): Bitmap {
    val pal = palette(context)
    val (bmp, canvas) = createSafeCanvas(widthDp, heightDp)
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()
    val today = ToymoteDate.today()
    val byDay = entries.groupBy { it.dateEpochDay }
    val streak = AnalyticsEngine.computeStreaks(entries, today)

    drawCardBackground(canvas, pal, min(w, h) * 0.14f.coerceAtMost(36f))

    val boldTf = font(context, bold = true)
    val regTf = font(context, bold = false)

    val isCompact2x1 = widthDp < 225 && heightDp < 115

    if (isCompact2x1) {
      // Crisp 2x1 Combo: Left compact purple flame+number block, Right 3 (or 4) large recent faces
      val pad = min(w, h) * 0.11f
      val innerH = h - pad * 2f
      val leftW = (innerH * 1.05f).coerceAtMost(w * 0.30f)
      val leftRect = RectF(pad, pad, pad + leftW, h - pad)

      val gradPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          shader =
            LinearGradient(
              leftRect.left,
              leftRect.top,
              leftRect.right,
              leftRect.bottom,
              pal.accentPrimary,
              pal.accentSecondary,
              Shader.TileMode.CLAMP,
            )
        }
      canvas.drawRoundRect(leftRect, leftW * 0.28f, leftW * 0.28f, gradPaint)

      val fSize = min(leftRect.width(), leftRect.height()) * 0.44f
      val fCx = leftRect.centerX()
      val fTop = leftRect.top + leftRect.height() * 0.12f
      WidgetFaceCanvas.drawFlame(
        canvas,
        RectF(fCx - fSize / 2f, fTop, fCx + fSize / 2f, fTop + fSize),
        pal.flameOuter,
        pal.flameInner,
      )

      val numPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          typeface = boldTf
          color = Color.WHITE
          textSize = (leftRect.height() * 0.33f).coerceAtMost(leftRect.width() * 0.55f)
          textAlign = Paint.Align.CENTER
        }
      canvas.drawText(
        "${streak.currentStreak}",
        leftRect.centerX(),
        leftRect.bottom - leftRect.height() * 0.12f,
        numPaint,
      )

      val faceCount = if (widthDp >= 185) 4 else 3
      drawRecentLargeFacesRow(
        context = context,
        canvas = canvas,
        pal = pal,
        today = today,
        byDay = byDay,
        area = RectF(leftRect.right + pad * 0.8f, pad, w - pad, h - pad),
        dayCount = faceCount,
        boldTf = boldTf,
        regTf = regTf,
      )
    } else if (widthDp >= 225 && heightDp < 155) {
      // Wide & Short (3x1 / 4x1 / 4x2 short): Side-by-side Streak + 7-day Strip
      val pad = min(w, h) * 0.08f
      val leftW = w * 0.31f
      val leftRect = RectF(pad, pad, pad + leftW, h - pad)
      val gradPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          shader =
            LinearGradient(
              leftRect.left,
              leftRect.top,
              leftRect.right,
              leftRect.bottom,
              pal.accentPrimary,
              pal.accentSecondary,
              Shader.TileMode.CLAMP,
            )
        }
      canvas.drawRoundRect(leftRect, min(leftRect.width(), leftRect.height()) * 0.24f, min(leftRect.width(), leftRect.height()) * 0.24f, gradPaint)

      val fSize = min(leftRect.width(), leftRect.height()) * 0.36f
      WidgetFaceCanvas.drawFlame(
        canvas,
        RectF(
          leftRect.left + leftRect.width() * 0.14f,
          leftRect.top + leftRect.height() * 0.16f,
          leftRect.left + leftRect.width() * 0.14f + fSize,
          leftRect.top + leftRect.height() * 0.16f + fSize,
        ),
        pal.flameOuter,
        pal.flameInner,
      )
      val numPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          typeface = boldTf
          color = Color.WHITE
          textSize = min(leftRect.height() * 0.30f, leftRect.width() * 0.34f)
        }
      val lblPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          typeface = regTf
          color = Color.argb(225, 255, 255, 255)
          textSize = min(leftRect.height() * 0.15f, leftRect.width() * 0.16f)
        }
      canvas.drawText(
        "${streak.currentStreak}",
        leftRect.left + leftRect.width() * 0.14f + fSize + 6f,
        leftRect.top + leftRect.height() * 0.44f,
        numPaint,
      )
      drawEllipsizedText(
        canvas,
        daysWordShortRu(streak.currentStreak) + " подряд",
        leftRect.left + leftRect.width() * 0.14f,
        leftRect.bottom - leftRect.height() * 0.16f,
        leftRect.width() * 0.76f,
        lblPaint,
      )

      val rightArea = RectF(leftRect.right + pad, pad, w - pad, h - pad)
      val monthPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          typeface = boldTf
          color = pal.textPrimary
          textSize = (h * 0.14f).coerceIn(14f, 24f)
        }
      drawEllipsizedText(
        canvas,
        today.monthYearTitleRu(),
        rightArea.left,
        rightArea.top + monthPaint.textSize,
        rightArea.width(),
        monthPaint,
      )
      drawWeekStrip(
        context = context,
        canvas = canvas,
        pal = pal,
        today = today,
        byDay = byDay,
        area = RectF(rightArea.left, rightArea.top + monthPaint.textSize + h * 0.07f, rightArea.right, rightArea.bottom),
        boldTf = boldTf,
        regTf = regTf,
      )
    } else if (widthDp < 225) {
      // Compact square (2x2 / 2x3): Top Streak Pill + Recent Strip or 2-Week Grid
      val pad = min(w, h) * 0.075f
      val bannerH = (h * 0.24f).coerceIn(40f, 68f)
      val bannerRect = RectF(pad, pad, w - pad, pad + bannerH)
      val gradPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          shader =
            LinearGradient(
              bannerRect.left,
              bannerRect.top,
              bannerRect.right,
              bannerRect.bottom,
              pal.accentPrimary,
              pal.accentSecondary,
              Shader.TileMode.CLAMP,
            )
        }
      canvas.drawRoundRect(bannerRect, bannerH * 0.34f, bannerH * 0.34f, gradPaint)
      val fSize = bannerH * 0.62f
      val fTop = bannerRect.centerY() - fSize / 2f
      WidgetFaceCanvas.drawFlame(
        canvas,
        RectF(bannerRect.left + pad * 0.9f, fTop, bannerRect.left + pad * 0.9f + fSize, fTop + fSize),
        pal.flameOuter,
        pal.flameInner,
      )
      val bannerTextPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          typeface = boldTf
          color = Color.WHITE
          textSize = bannerH * 0.40f
        }
      val tx = bannerRect.left + pad * 1.4f + fSize
      val ty = bannerRect.centerY() - (bannerTextPaint.descent() + bannerTextPaint.ascent()) / 2f
      drawEllipsizedText(
        canvas,
        ToymoteDate.formatDaysStreakRu(streak.currentStreak),
        tx,
        ty,
        (bannerRect.right - tx - pad * 0.8f).coerceAtLeast(16f),
        bannerTextPaint,
      )

      val bottomArea = RectF(pad, bannerRect.bottom + pad * 0.85f, w - pad, h - pad)
      if (heightDp < 155) {
        drawRecentLargeFacesRow(
          context = context,
          canvas = canvas,
          pal = pal,
          today = today,
          byDay = byDay,
          area = bottomArea,
          dayCount = 4,
          boldTf = boldTf,
          regTf = regTf,
        )
      } else {
        drawTwoWeekGrid(
          context = context,
          canvas = canvas,
          pal = pal,
          today = today,
          byDay = byDay,
          area = bottomArea,
          boldTf = boldTf,
          regTf = regTf,
        )
      }
    } else {
      // Full Combo: Top Streak Banner + Full Month Calendar Grid
      val pad = min(w, h) * 0.06f
      val bannerH = (h * 0.20f).coerceIn(54f, 88f)
      val bannerRect = RectF(pad, pad, w - pad, pad + bannerH)
      val gradPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          shader =
            LinearGradient(
              bannerRect.left,
              bannerRect.top,
              bannerRect.right,
              bannerRect.bottom,
              pal.accentPrimary,
              pal.accentSecondary,
              Shader.TileMode.CLAMP,
            )
        }
      canvas.drawRoundRect(bannerRect, bannerH * 0.30f, bannerH * 0.30f, gradPaint)

      val fSize = bannerH * 0.60f
      val fTop = bannerRect.centerY() - fSize / 2f
      WidgetFaceCanvas.drawFlame(
        canvas,
        RectF(bannerRect.left + pad * 1.0f, fTop, bannerRect.left + pad * 1.0f + fSize, fTop + fSize),
        pal.flameOuter,
        pal.flameInner,
      )

      val todayEntry =
        entries.filter { it.dateEpochDay == today.epochDay }.maxByOrNull { it.timestampMillis }
      val todayEmotion = todayEntry?.let { EmotionCatalog.findById(it.emotionId) }
      val miniSize = bannerH * 0.68f
      val miniRect =
        RectF(
          bannerRect.right - pad - miniSize,
          bannerRect.centerY() - miniSize / 2f,
          bannerRect.right - pad,
          bannerRect.centerY() + miniSize / 2f,
        )
      if (todayEmotion != null) {
        WidgetFaceCanvas.drawEmotionBadge(context, canvas, todayEmotion, miniRect, showBorder = false)
      }

      val tx = bannerRect.left + pad * 1.6f + fSize
      val maxTextW =
        ((if (todayEmotion != null) miniRect.left else bannerRect.right) - tx - pad * 0.8f)
          .coerceAtLeast(24f)

      val streakTitlePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          typeface = boldTf
          color = Color.WHITE
          textSize = bannerH * 0.33f
        }
      val streakSubPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          typeface = regTf
          color = Color.argb(225, 255, 255, 255)
          textSize = bannerH * 0.21f
        }
      drawEllipsizedText(
        canvas,
        ToymoteDate.formatDaysStreakRu(streak.currentStreak),
        tx,
        bannerRect.top + bannerH * 0.47f,
        maxTextW,
        streakTitlePaint,
      )
      drawEllipsizedText(
        canvas,
        "${today.monthYearTitleRu()} • Рекорд ${streak.longestStreak} дн.",
        tx,
        bannerRect.top + bannerH * 0.77f,
        maxTextW,
        streakSubPaint,
      )

      drawMonthCalendarGrid(
        context = context,
        canvas = canvas,
        pal = pal,
        today = today,
        byDay = byDay,
        area = RectF(pad, bannerRect.bottom + pad * 0.7f, w - pad, h - pad),
        boldTf = boldTf,
        regTf = regTf,
      )
    }

    return bmp
  }

  // ============================================================================================
  // SHARED CALENDAR DRAWING HELPERS
  // ============================================================================================
  private fun drawRecentLargeFacesRow(
    context: Context,
    canvas: Canvas,
    pal: WidgetPalette,
    today: ToymoteDate,
    byDay: Map<Long, List<MoodEntryEntity>>,
    area: RectF,
    dayCount: Int,
    boldTf: Typeface,
    regTf: Typeface,
  ) {
    val gap = (area.width() * 0.03f).coerceIn(4f, 12f)
    val colW = (area.width() - gap * (dayCount - 1)) / dayCount
    val numSize = (area.height() * 0.18f).coerceIn(12f, 20f)
    val labelPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = boldTf
        textSize = numSize
        textAlign = Paint.Align.CENTER
      }

    val cellTop = area.top + numSize + 5f
    val cellSize = min(colW, (area.bottom - cellTop).coerceAtLeast(16f))
    val verticalOffset = ((area.bottom - cellTop) - cellSize).coerceAtLeast(0f) / 2f
    val yTop = cellTop + verticalOffset

    for (i in 0 until dayCount) {
      val date = today.plusDays(i - (dayCount - 1))
      val isToday = date.epochDay == today.epochDay
      val left = area.left + i * (colW + gap)
      val cx = left + colW / 2f

      labelPaint.color = if (isToday) pal.accentPrimary else pal.textSecondary
      canvas.drawText("${date.dayOfMonth}", cx, area.top + numSize, labelPaint)

      val cellRect = RectF(cx - cellSize / 2f, yTop, cx + cellSize / 2f, yTop + cellSize)
      val dayEntry = byDay[date.epochDay]?.maxByOrNull { it.timestampMillis }
      drawCalendarDayCell(context, canvas, pal, date, isToday, dayEntry, cellRect, boldTf, regTf)
    }
  }

  private fun drawWeekStrip(
    context: Context,
    canvas: Canvas,
    pal: WidgetPalette,
    today: ToymoteDate,
    byDay: Map<Long, List<MoodEntryEntity>>,
    area: RectF,
    boldTf: Typeface,
    regTf: Typeface,
  ) {
    val monday = today.plusDays(-today.dayOfWeekMondayZero())
    val colW = area.width() / 7f
    val labelPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = boldTf
        color = pal.textSecondary
        textSize = (area.height() * 0.19f).coerceIn(13f, 22f)
        textAlign = Paint.Align.CENTER
      }
    val cellTop = area.top + labelPaint.textSize + 6f
    val cellSize = min(colW * 0.84f, (area.bottom - cellTop) * 0.90f)

    for (i in 0 until 7) {
      val date = monday.plusDays(i)
      val cx = area.left + colW * i + colW / 2f
      val isToday = date.epochDay == today.epochDay
      labelPaint.color = if (isToday) pal.accentPrimary else pal.textSecondary
      canvas.drawText(ToymoteDate.WEEKDAYS_RU[i], cx, area.top + labelPaint.textSize, labelPaint)

      val cellRect = RectF(cx - cellSize / 2f, cellTop, cx + cellSize / 2f, cellTop + cellSize)
      val dayEntry = byDay[date.epochDay]?.maxByOrNull { it.timestampMillis }
      drawCalendarDayCell(context, canvas, pal, date, isToday, dayEntry, cellRect, boldTf, regTf)
    }
  }

  private fun drawTwoWeekGrid(
    context: Context,
    canvas: Canvas,
    pal: WidgetPalette,
    today: ToymoteDate,
    byDay: Map<Long, List<MoodEntryEntity>>,
    area: RectF,
    boldTf: Typeface,
    regTf: Typeface,
  ) {
    val thisMonday = today.plusDays(-today.dayOfWeekMondayZero())
    val startDate = thisMonday.plusDays(-7)
    val colW = area.width() / 7f
    val dowPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = boldTf
        color = pal.textSecondary
        textSize = (area.height() * 0.11f).coerceIn(11f, 19f)
        textAlign = Paint.Align.CENTER
      }
    for (c in 0 until 7) {
      val cx = area.left + colW * c + colW / 2f
      canvas.drawText(ToymoteDate.WEEKDAYS_RU[c], cx, area.top + dowPaint.textSize, dowPaint)
    }

    val gridTop = area.top + dowPaint.textSize + 6f
    val rowH = (area.bottom - gridTop) / 2f
    val cellSize = min(colW * 0.84f, rowH * 0.86f)

    for (r in 0 until 2) {
      for (c in 0 until 7) {
        val date = startDate.plusDays(r * 7 + c)
        val cx = area.left + colW * c + colW / 2f
        val cy = gridTop + rowH * r + rowH / 2f
        val cellRect = RectF(cx - cellSize / 2f, cy - cellSize / 2f, cx + cellSize / 2f, cy + cellSize / 2f)
        val dayEntry = byDay[date.epochDay]?.maxByOrNull { it.timestampMillis }
        drawCalendarDayCell(
          context,
          canvas,
          pal,
          date,
          date.epochDay == today.epochDay,
          dayEntry,
          cellRect,
          boldTf,
          regTf,
        )
      }
    }
  }

  private fun drawMonthCalendarGrid(
    context: Context,
    canvas: Canvas,
    pal: WidgetPalette,
    today: ToymoteDate,
    byDay: Map<Long, List<MoodEntryEntity>>,
    area: RectF,
    boldTf: Typeface,
    regTf: Typeface,
  ) {
    val firstDay = ToymoteDate(today.year, today.month, 1)
    val leadingBlanks = firstDay.dayOfWeekMondayZero()
    val daysCount = firstDay.daysInMonth()
    val rows = ceil((leadingBlanks + daysCount) / 7f).toInt().coerceIn(4, 6)

    val colW = area.width() / 7f
    val dowPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = boldTf
        color = pal.textSecondary
        textSize = (area.height() * 0.068f).coerceIn(12f, 20f)
        textAlign = Paint.Align.CENTER
      }
    for (c in 0 until 7) {
      val cx = area.left + colW * c + colW / 2f
      canvas.drawText(ToymoteDate.WEEKDAYS_RU[c], cx, area.top + dowPaint.textSize, dowPaint)
    }

    val gridTop = area.top + dowPaint.textSize + 6f
    val rowH = (area.bottom - gridTop) / rows
    val cellSize = min(colW * 0.84f, rowH * 0.86f)

    for (day in 1..daysCount) {
      val idx = leadingBlanks + (day - 1)
      val r = idx / 7
      val c = idx % 7
      val date = ToymoteDate(today.year, today.month, day)
      val cx = area.left + colW * c + colW / 2f
      val cy = gridTop + rowH * r + rowH / 2f
      val cellRect = RectF(cx - cellSize / 2f, cy - cellSize / 2f, cx + cellSize / 2f, cy + cellSize / 2f)
      val dayEntry = byDay[date.epochDay]?.maxByOrNull { it.timestampMillis }
      drawCalendarDayCell(
        context,
        canvas,
        pal,
        date,
        date.epochDay == today.epochDay,
        dayEntry,
        cellRect,
        boldTf,
        regTf,
      )
    }
  }

  private fun drawCalendarDayCell(
    context: Context,
    canvas: Canvas,
    pal: WidgetPalette,
    date: ToymoteDate,
    isToday: Boolean,
    latestEntry: MoodEntryEntity?,
    rect: RectF,
    boldTf: Typeface,
    regTf: Typeface,
  ) {
    if (latestEntry != null) {
      val emotion = EmotionCatalog.findById(latestEntry.emotionId)
      WidgetFaceCanvas.drawEmotionBadge(context, canvas, emotion, rect, showBorder = !isToday)
    } else {
      val bgPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = if (isToday) pal.accentContainer else pal.emptyCellBg
        }
      val rad = rect.width() * 0.28f
      canvas.drawRoundRect(rect, rad, rad, bgPaint)

      val numPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          typeface = if (isToday) boldTf else regTf
          color = if (isToday) pal.onAccentContainer else pal.textSecondary
          textSize = rect.height() * 0.42f
          textAlign = Paint.Align.CENTER
        }
      val ty = rect.centerY() - (numPaint.descent() + numPaint.ascent()) / 2f
      canvas.drawText("${date.dayOfMonth}", rect.centerX(), ty, numPaint)
    }

    if (isToday) {
      val ringPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.STROKE
          strokeWidth = max(2.2f, rect.width() * 0.08f)
          color = pal.accentPrimary
        }
      val rad = rect.width() * 0.28f
      canvas.drawRoundRect(rect, rad, rad, ringPaint)
    }
  }

  private fun drawEmptyFaceSlot(canvas: Canvas, rect: RectF, pal: WidgetPalette) {
    val rad = rect.width() * 0.28f
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pal.emptyCellBg }
    canvas.drawRoundRect(rect, rad, rad, fillPaint)

    val dashPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = max(2f, rect.width() * 0.05f)
        color = pal.accentPrimary
        pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f)
      }
    canvas.drawRoundRect(rect, rad, rad, dashPaint)

    val plusPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = pal.accentPrimary
        strokeWidth = max(2.5f, rect.width() * 0.08f)
        strokeCap = Paint.Cap.ROUND
      }
    val arm = rect.width() * 0.18f
    canvas.drawLine(rect.centerX() - arm, rect.centerY(), rect.centerX() + arm, rect.centerY(), plusPaint)
    canvas.drawLine(rect.centerX(), rect.centerY() - arm, rect.centerX(), rect.centerY() + arm, plusPaint)
  }
}
