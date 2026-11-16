package com.example.widgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import com.example.domain.EmotionDef
import com.example.domain.FaceRender
import com.example.domain.VectorFaceStyle
import kotlin.math.max

/**
 * Draws any Toymote emotion (photo face or classic Roblox vector face) onto an Android Canvas
 * for responsive home screen widgets.
 */
object WidgetFaceCanvas {

  fun drawEmotionBadge(
    context: Context,
    canvas: Canvas,
    emotion: EmotionDef,
    bounds: RectF,
    showBorder: Boolean = true,
  ) {
    val w = bounds.width()
    val h = bounds.height()
    val radius = w * 0.28f
    val clipPath = Path().apply { addRoundRect(bounds, radius, radius, Path.Direction.CW) }

    canvas.save()
    canvas.clipPath(clipPath)

    when (val render = emotion.render) {
      is FaceRender.ImageFace -> {
        val bgPaint =
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (render.isDarkArtwork) 0xFF0B0812.toInt() else Color.WHITE
          }
        canvas.drawRect(bounds, bgPaint)

        val opts =
          BitmapFactory.Options().apply {
            inSampleSize = if (w <= 96f) 4 else 2
            inPreferredConfig = Bitmap.Config.ARGB_8888
          }
        val bmp = BitmapFactory.decodeResource(context.resources, render.resId, opts)
        if (bmp != null) {
          val pad = if (render.isDarkArtwork) w * 0.02f else w * 0.04f
          val dst = RectF(bounds.left + pad, bounds.top + pad, bounds.right - pad, bounds.bottom - pad)
          val imgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
          canvas.drawBitmap(bmp, null, dst, imgPaint)
          bmp.recycle()
        }
      }

      is FaceRender.VectorFace -> {
        val bgPaint =
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader =
              LinearGradient(
                bounds.left,
                bounds.top,
                bounds.left,
                bounds.bottom,
                0xFFFFFBF4.toInt(),
                0xFFF3EBFF.toInt(),
                Shader.TileMode.CLAMP,
              )
          }
        canvas.drawRect(bounds, bgPaint)
        drawVectorFace(canvas, bounds, render.style, (emotion.accentColorHex and 0xFFFFFFFFL).toInt())
      }
    }
    canvas.restore()

    if (showBorder) {
      val accent = (emotion.accentColorHex and 0xFFFFFFFFL).toInt()
      val borderAlphaColor = Color.argb(125, Color.red(accent), Color.green(accent), Color.blue(accent))
      val borderPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.STROKE
          strokeWidth = max(1.5f, w * 0.045f)
          color = borderAlphaColor
        }
      canvas.drawRoundRect(bounds, radius, radius, borderPaint)
    }
  }

  fun drawFlame(canvas: Canvas, bounds: RectF, outerColor: Int, innerColor: Int) {
    val l = bounds.left
    val t = bounds.top
    val w = bounds.width()
    val h = bounds.height()

    val outerPath =
      Path().apply {
        moveTo(l + w * 0.52f, t + h * 0.04f)
        cubicTo(
          l + w * 0.78f, t + h * 0.28f,
          l + w * 0.92f, t + h * 0.54f,
          l + w * 0.80f, t + h * 0.80f,
        )
        cubicTo(
          l + w * 0.68f, t + h * 0.98f,
          l + w * 0.30f, t + h * 0.98f,
          l + w * 0.18f, t + h * 0.76f,
        )
        cubicTo(
          l + w * 0.08f, t + h * 0.56f,
          l + w * 0.24f, t + h * 0.32f,
          l + w * 0.34f, t + h * 0.22f,
        )
        cubicTo(
          l + w * 0.36f, t + h * 0.34f,
          l + w * 0.44f, t + h * 0.38f,
          l + w * 0.52f, t + h * 0.04f,
        )
        close()
      }
    val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = outerColor }
    canvas.drawPath(outerPath, outerPaint)

    val innerPath =
      Path().apply {
        moveTo(l + w * 0.50f, t + h * 0.42f)
        cubicTo(
          l + w * 0.68f, t + h * 0.58f,
          l + w * 0.70f, t + h * 0.86f,
          l + w * 0.50f, t + h * 0.88f,
        )
        cubicTo(
          l + w * 0.32f, t + h * 0.86f,
          l + w * 0.34f, t + h * 0.58f,
          l + w * 0.50f, t + h * 0.42f,
        )
        close()
      }
    val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = innerColor }
    canvas.drawPath(innerPath, innerPaint)
  }

  private fun drawVectorFace(
    canvas: Canvas,
    bounds: RectF,
    style: VectorFaceStyle,
    accent: Int,
  ) {
    val l = bounds.left
    val t = bounds.top
    val w = bounds.width()
    val h = bounds.height()
    val ink = 0xFF1A122B.toInt()
    val strokeW = max(1.6f, w * 0.068f)
    val thinStroke = max(1.2f, w * 0.05f)

    val inkFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ink }
    val inkStroke =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ink
        this.style = Paint.Style.STROKE
        strokeWidth = strokeW
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
      }
    val thinPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ink
        this.style = Paint.Style.STROKE
        strokeWidth = thinStroke
        strokeCap = Paint.Cap.ROUND
      }

    val leftEyeX = l + w * 0.34f
    val rightEyeX = l + w * 0.66f
    val eyeY = t + h * 0.39f
    val eyeW = w * 0.10f
    val eyeH = h * 0.16f

    when (style) {
      VectorFaceStyle.VESELYI -> {
        drawOvalEye(canvas, leftEyeX, eyeY, eyeW, eyeH, inkFill)
        drawOvalEye(canvas, rightEyeX, eyeY, eyeW, eyeH, inkFill)
        val mouth =
          Path().apply {
            moveTo(l + w * 0.28f, t + h * 0.58f)
            lineTo(l + w * 0.72f, t + h * 0.58f)
            quadTo(l + w * 0.70f, t + h * 0.80f, l + w * 0.50f, t + h * 0.80f)
            quadTo(l + w * 0.30f, t + h * 0.80f, l + w * 0.28f, t + h * 0.58f)
            close()
          }
        canvas.drawPath(mouth, inkFill)
        val tongue = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF6D9D.toInt() }
        canvas.drawRoundRect(
          RectF(l + w * 0.39f, t + h * 0.67f, l + w * 0.61f, t + h * 0.77f),
          w * 0.05f,
          w * 0.05f,
          tongue,
        )
      }

      VectorFaceStyle.SCHASTLIVYI -> {
        canvas.drawArc(
          RectF(leftEyeX - eyeW * 1.05f, eyeY - eyeH * 0.4f, leftEyeX + eyeW * 1.05f, eyeY + eyeH * 0.7f),
          195f,
          150f,
          false,
          inkStroke,
        )
        canvas.drawArc(
          RectF(rightEyeX - eyeW * 1.05f, eyeY - eyeH * 0.4f, rightEyeX + eyeW * 1.05f, eyeY + eyeH * 0.7f),
          195f,
          150f,
          false,
          inkStroke,
        )
        val grin =
          Path().apply {
            moveTo(l + w * 0.27f, t + h * 0.60f)
            quadTo(l + w * 0.50f, t + h * 0.80f, l + w * 0.73f, t + h * 0.60f)
          }
        canvas.drawPath(grin, inkStroke)
      }

      VectorFaceStyle.VLYUBLENNYI -> {
        val heartPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF3D8B.toInt() }
        drawHeart(canvas, leftEyeX, eyeY, w * 0.14f, heartPaint)
        drawHeart(canvas, rightEyeX, eyeY, w * 0.14f, heartPaint)
        val smile =
          Path().apply {
            moveTo(l + w * 0.34f, t + h * 0.64f)
            quadTo(l + w * 0.50f, t + h * 0.76f, l + w * 0.66f, t + h * 0.64f)
          }
        canvas.drawPath(smile, inkStroke)
      }

      VectorFaceStyle.UDIVLENNYI -> {
        canvas.drawLine(l + w * 0.24f, t + h * 0.21f, l + w * 0.42f, t + h * 0.18f, thinPaint)
        canvas.drawLine(l + w * 0.58f, t + h * 0.18f, l + w * 0.76f, t + h * 0.21f, thinPaint)
        canvas.drawCircle(leftEyeX, eyeY + h * 0.02f, w * 0.085f, inkFill)
        canvas.drawCircle(rightEyeX, eyeY + h * 0.02f, w * 0.085f, inkFill)
        canvas.drawOval(RectF(l + w * 0.40f, t + h * 0.59f, l + w * 0.60f, t + h * 0.77f), inkFill)
      }

      VectorFaceStyle.USTALYI -> {
        canvas.drawLine(l + w * 0.24f, t + h * 0.34f, l + w * 0.44f, t + h * 0.36f, inkStroke)
        canvas.drawLine(l + w * 0.56f, t + h * 0.36f, l + w * 0.76f, t + h * 0.34f, inkStroke)
        canvas.drawArc(
          RectF(l + w * 0.25f, t + h * 0.31f, l + w * 0.43f, t + h * 0.45f),
          0f,
          180f,
          false,
          thinPaint,
        )
        canvas.drawArc(
          RectF(l + w * 0.57f, t + h * 0.31f, l + w * 0.75f, t + h * 0.45f),
          0f,
          180f,
          false,
          thinPaint,
        )
        val bagPaint =
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(150, Color.red(accent), Color.green(accent), Color.blue(accent))
            this.style = Paint.Style.STROKE
            strokeWidth = thinStroke * 0.8f
          }
        canvas.drawLine(l + w * 0.27f, t + h * 0.50f, l + w * 0.41f, t + h * 0.50f, bagPaint)
        canvas.drawLine(l + w * 0.59f, t + h * 0.50f, l + w * 0.73f, t + h * 0.50f, bagPaint)
        canvas.drawRoundRect(
          RectF(l + w * 0.37f, t + h * 0.65f, l + w * 0.63f, t + h * 0.73f),
          w * 0.04f,
          w * 0.04f,
          inkFill,
        )
      }

      VectorFaceStyle.TREVOZHNYI -> {
        canvas.drawLine(l + w * 0.22f, t + h * 0.29f, l + w * 0.42f, t + h * 0.22f, thinPaint)
        canvas.drawLine(l + w * 0.58f, t + h * 0.22f, l + w * 0.78f, t + h * 0.29f, thinPaint)
        drawOvalEye(canvas, leftEyeX, eyeY + h * 0.02f, eyeW * 0.9f, eyeH * 0.85f, inkFill)
        drawOvalEye(canvas, rightEyeX, eyeY + h * 0.02f, eyeW * 0.9f, eyeH * 0.85f, inkFill)
        val wave =
          Path().apply {
            moveTo(l + w * 0.27f, t + h * 0.67f)
            lineTo(l + w * 0.38f, t + h * 0.62f)
            lineTo(l + w * 0.50f, t + h * 0.68f)
            lineTo(l + w * 0.62f, t + h * 0.62f)
            lineTo(l + w * 0.73f, t + h * 0.67f)
          }
        canvas.drawPath(wave, thinPaint)
      }

      VectorFaceStyle.NEITRALNYI -> {
        drawOvalEye(canvas, leftEyeX, eyeY, eyeW * 0.9f, eyeH * 0.9f, inkFill)
        drawOvalEye(canvas, rightEyeX, eyeY, eyeW * 0.9f, eyeH * 0.9f, inkFill)
        canvas.drawLine(l + w * 0.32f, t + h * 0.67f, l + w * 0.68f, t + h * 0.67f, inkStroke)
      }

      VectorFaceStyle.RAZOCHAROVANNYI -> {
        canvas.drawLine(l + w * 0.23f, t + h * 0.28f, l + w * 0.42f, t + h * 0.25f, thinPaint)
        canvas.drawLine(l + w * 0.58f, t + h * 0.25f, l + w * 0.77f, t + h * 0.28f, thinPaint)
        drawOvalEye(canvas, leftEyeX, eyeY + h * 0.03f, eyeW * 0.85f, eyeH * 0.75f, inkFill)
        drawOvalEye(canvas, rightEyeX, eyeY + h * 0.03f, eyeW * 0.85f, eyeH * 0.75f, inkFill)
        val frown =
          Path().apply {
            moveTo(l + w * 0.30f, t + h * 0.72f)
            quadTo(l + w * 0.50f, t + h * 0.57f, l + w * 0.70f, t + h * 0.72f)
          }
        canvas.drawPath(frown, inkStroke)
      }

      VectorFaceStyle.RAZDRAZHENNYI -> {
        canvas.drawLine(l + w * 0.21f, t + h * 0.22f, l + w * 0.44f, t + h * 0.31f, inkStroke)
        canvas.drawLine(l + w * 0.79f, t + h * 0.22f, l + w * 0.56f, t + h * 0.31f, inkStroke)
        drawOvalEye(canvas, leftEyeX, eyeY + h * 0.04f, eyeW * 0.9f, eyeH * 0.75f, inkFill)
        drawOvalEye(canvas, rightEyeX, eyeY + h * 0.04f, eyeW * 0.9f, eyeH * 0.75f, inkFill)
        val mouth =
          Path().apply {
            moveTo(l + w * 0.28f, t + h * 0.70f)
            quadTo(l + w * 0.50f, t + h * 0.58f, l + w * 0.72f, t + h * 0.70f)
          }
        canvas.drawPath(mouth, inkStroke)
      }
    }
  }

  private fun drawOvalEye(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    halfW: Float,
    halfH: Float,
    fillPaint: Paint,
  ) {
    canvas.drawOval(RectF(cx - halfW, cy - halfH, cx + halfW, cy + halfH), fillPaint)
    val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    canvas.drawCircle(cx - halfW * 0.25f, cy - halfH * 0.35f, halfW * 0.34f, whitePaint)
  }

  private fun drawHeart(canvas: Canvas, cx: Float, cy: Float, r: Float, paint: Paint) {
    val p =
      Path().apply {
        moveTo(cx, cy + r * 0.85f)
        cubicTo(cx - r * 1.4f, cy + r * 0.1f, cx - r * 0.95f, cy - r * 1.0f, cx, cy - r * 0.3f)
        cubicTo(cx + r * 0.95f, cy - r * 1.0f, cx + r * 1.4f, cy + r * 0.1f, cx, cy + r * 0.85f)
        close()
      }
    canvas.drawPath(p, paint)
  }
}
