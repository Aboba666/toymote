package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class RobloxIconType {
  CALENDAR,
  STATS,
  ADVICE,
  AI_SUPPORT,
  ADD,
  SETTINGS,
  BACK,
  PREV,
  NEXT,
  MIC,
  MIC_OFF,
  SEND,
  VOLUME,
  REFRESH,
  EDIT,
  DELETE,
  FIRE,
  TROPHY,
  LIST,
  CHECK,
  LOCK,
  CLOCK,
  PALETTE,
  STOP,
  ROBOT,
}

/**
 * Custom vector recreation of the MATTO company logo (4-petal swirl pinwheel + bold MATTO mark).
 */
@Composable
fun MattoCompanyLogo(
  modifier: Modifier = Modifier,
  compact: Boolean = false,
) {
  val emblemSize = if (compact) 24.dp else 32.dp
  val fillTint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
  val strokeTint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
  val wordColor = MaterialTheme.colorScheme.onSurfaceVariant

  Row(
    modifier = modifier.semantics { contentDescription = "Логотип компании MATTO" },
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 8.dp),
  ) {
    Canvas(modifier = Modifier.size(emblemSize)) {
      val cx = size.width / 2f
      val cy = size.height / 2f
      val r = size.minDimension * 0.24f
      val offset = size.minDimension * 0.22f
      val strokeW = (size.minDimension * 0.045f).coerceAtLeast(1.2f)

      // 4 swirling overlapping petals reproducing the MATTO emblem
      for (i in 0 until 4) {
        rotate(degrees = i * 90f, pivot = Offset(cx, cy)) {
          val petalCenter = Offset(cx, cy - offset)
          drawCircle(
            color = fillTint,
            radius = r,
            center = petalCenter,
          )
          drawArc(
            color = strokeTint,
            startAngle = -140f,
            sweepAngle = 225f,
            useCenter = false,
            topLeft = Offset(petalCenter.x - r, petalCenter.y - r),
            size = Size(r * 2f, r * 2f),
            style = Stroke(width = strokeW, cap = StrokeCap.Round),
          )
        }
      }
    }

    Text(
      text = "MATTO",
      style =
        MaterialTheme.typography.titleSmall.copy(
          fontWeight = FontWeight.ExtraBold,
          fontSize = if (compact) 13.sp else 17.sp,
          letterSpacing = 0.8.sp,
        ),
      color = wordColor,
    )
  }
}

/**
 * Blocky, Roblox-styled geometric vector icons for all buttons and navigation items.
 */
@Composable
fun RobloxIcon(
  type: RobloxIconType,
  contentDescription: String?,
  modifier: Modifier = Modifier,
  size: Dp = 24.dp,
  tint: Color = LocalContentColor.current,
) {
  val semanticsModifier =
    if (contentDescription != null) {
      modifier.semantics { this.contentDescription = contentDescription }
    } else {
      modifier
    }

  Canvas(modifier = semanticsModifier.size(size)) {
    drawRobloxIcon(type, tint)
  }
}

private fun DrawScope.drawRobloxIcon(type: RobloxIconType, tint: Color) {
  val w = size.width
  val h = size.height
  val sw = (w * 0.10f).coerceAtLeast(2.2f)
  val cr = CornerRadius(w * 0.10f, w * 0.10f)
  val stroke = Stroke(width = sw, cap = StrokeCap.Square, join = StrokeJoin.Miter)

  when (type) {
    RobloxIconType.CALENDAR -> {
      // Blocky calendar brick with top studs & 2x2 pixel blocks inside
      drawRoundRect(
        color = tint,
        topLeft = Offset(w * 0.14f, h * 0.22f),
        size = Size(w * 0.72f, h * 0.64f),
        cornerRadius = cr,
        style = stroke,
      )
      drawRect(color = tint, topLeft = Offset(w * 0.26f, h * 0.10f), size = Size(w * 0.14f, h * 0.12f))
      drawRect(color = tint, topLeft = Offset(w * 0.60f, h * 0.10f), size = Size(w * 0.14f, h * 0.12f))
      drawRect(color = tint, topLeft = Offset(w * 0.14f, h * 0.36f), size = Size(w * 0.72f, sw * 0.8f))
      // 4 blocky day tiles
      drawRect(color = tint, topLeft = Offset(w * 0.27f, h * 0.50f), size = Size(w * 0.16f, h * 0.12f))
      drawRect(color = tint, topLeft = Offset(w * 0.57f, h * 0.50f), size = Size(w * 0.16f, h * 0.12f))
      drawRect(color = tint, topLeft = Offset(w * 0.27f, h * 0.68f), size = Size(w * 0.16f, h * 0.10f))
      drawRect(color = tint, topLeft = Offset(w * 0.57f, h * 0.68f), size = Size(w * 0.16f, h * 0.10f))
    }

    RobloxIconType.STATS -> {
      // 3 chunky Roblox bar blocks
      drawRoundRect(
        color = tint,
        topLeft = Offset(w * 0.12f, h * 0.54f),
        size = Size(w * 0.20f, h * 0.34f),
        cornerRadius = CornerRadius(w * 0.05f),
      )
      drawRoundRect(
        color = tint,
        topLeft = Offset(w * 0.40f, h * 0.20f),
        size = Size(w * 0.20f, h * 0.68f),
        cornerRadius = CornerRadius(w * 0.05f),
      )
      drawRoundRect(
        color = tint,
        topLeft = Offset(w * 0.68f, h * 0.38f),
        size = Size(w * 0.20f, h * 0.50f),
        cornerRadius = CornerRadius(w * 0.05f),
      )
    }

    RobloxIconType.ADVICE -> {
      // Roblox tilted-diamond / Robux-inspired star spark
      rotate(degrees = 15f, pivot = Offset(w * 0.5f, h * 0.5f)) {
        drawRoundRect(
          color = tint,
          topLeft = Offset(w * 0.20f, h * 0.20f),
          size = Size(w * 0.60f, h * 0.60f),
          cornerRadius = CornerRadius(w * 0.12f),
          style = stroke,
        )
        drawRect(
          color = tint,
          topLeft = Offset(w * 0.39f, h * 0.39f),
          size = Size(w * 0.22f, h * 0.22f),
        )
      }
    }

    RobloxIconType.AI_SUPPORT, RobloxIconType.ROBOT -> {
      // Classic Roblox blocky head silhouette with visor eyes & antenna stud
      drawRect(color = tint, topLeft = Offset(w * 0.44f, h * 0.09f), size = Size(w * 0.12f, h * 0.12f))
      drawRoundRect(
        color = tint,
        topLeft = Offset(w * 0.16f, h * 0.21f),
        size = Size(w * 0.68f, h * 0.62f),
        cornerRadius = CornerRadius(w * 0.16f),
        style = stroke,
      )
      // Blocky eyes
      drawRect(color = tint, topLeft = Offset(w * 0.31f, h * 0.40f), size = Size(w * 0.12f, h * 0.15f))
      drawRect(color = tint, topLeft = Offset(w * 0.57f, h * 0.40f), size = Size(w * 0.12f, h * 0.15f))
      // Classic Roblox smile bar
      drawRect(color = tint, topLeft = Offset(w * 0.34f, h * 0.66f), size = Size(w * 0.32f, h * 0.09f))
    }

    RobloxIconType.ADD -> {
      // Blocky stud-cross "+"
      drawRoundRect(
        color = tint,
        topLeft = Offset(w * 0.41f, h * 0.14f),
        size = Size(w * 0.18f, h * 0.72f),
        cornerRadius = CornerRadius(w * 0.06f),
      )
      drawRoundRect(
        color = tint,
        topLeft = Offset(w * 0.14f, h * 0.41f),
        size = Size(w * 0.72f, h * 0.18f),
        cornerRadius = CornerRadius(w * 0.06f),
      )
    }

    RobloxIconType.SETTINGS -> {
      // Chunky Roblox octagonal cog with square hole
      for (deg in listOf(0f, 45f)) {
        rotate(degrees = deg, pivot = Offset(w * 0.5f, h * 0.5f)) {
          drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.18f, h * 0.18f),
            size = Size(w * 0.64f, h * 0.64f),
            cornerRadius = CornerRadius(w * 0.08f),
            style = stroke,
          )
        }
      }
      drawRect(
        color = tint,
        topLeft = Offset(w * 0.38f, h * 0.38f),
        size = Size(w * 0.24f, h * 0.24f),
      )
    }

    RobloxIconType.BACK, RobloxIconType.PREV -> {
      val path =
        Path().apply {
          moveTo(w * 0.64f, h * 0.18f)
          lineTo(w * 0.30f, h * 0.50f)
          lineTo(w * 0.64f, h * 0.82f)
        }
      drawPath(path, color = tint, style = stroke)
      if (type == RobloxIconType.BACK) {
        drawRect(color = tint, topLeft = Offset(w * 0.34f, h * 0.44f), size = Size(w * 0.48f, h * 0.12f))
      }
    }

    RobloxIconType.NEXT -> {
      val path =
        Path().apply {
          moveTo(w * 0.36f, h * 0.18f)
          lineTo(w * 0.70f, h * 0.50f)
          lineTo(w * 0.36f, h * 0.82f)
        }
      drawPath(path, color = tint, style = stroke)
    }

    RobloxIconType.MIC -> {
      // Blocky Roblox microphone capsule + stand
      drawRoundRect(
        color = tint,
        topLeft = Offset(w * 0.34f, h * 0.12f),
        size = Size(w * 0.32f, h * 0.46f),
        cornerRadius = CornerRadius(w * 0.12f),
      )
      drawArc(
        color = tint,
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(w * 0.22f, h * 0.34f),
        size = Size(w * 0.56f, h * 0.36f),
        style = stroke,
      )
      drawRect(color = tint, topLeft = Offset(w * 0.45f, h * 0.70f), size = Size(w * 0.10f, h * 0.14f))
      drawRect(color = tint, topLeft = Offset(w * 0.30f, h * 0.82f), size = Size(w * 0.40f, h * 0.10f))
    }

    RobloxIconType.MIC_OFF -> {
      drawRoundRect(
        color = tint,
        topLeft = Offset(w * 0.34f, h * 0.14f),
        size = Size(w * 0.32f, h * 0.42f),
        cornerRadius = CornerRadius(w * 0.12f),
        style = stroke,
      )
      drawLine(
        color = tint,
        start = Offset(w * 0.16f, h * 0.18f),
        end = Offset(w * 0.84f, h * 0.82f),
        strokeWidth = sw,
        cap = StrokeCap.Square,
      )
    }

    RobloxIconType.SEND -> {
      // Chunky Roblox paper-jet arrow
      val path =
        Path().apply {
          moveTo(w * 0.14f, h * 0.18f)
          lineTo(w * 0.88f, h * 0.50f)
          lineTo(w * 0.14f, h * 0.82f)
          lineTo(w * 0.28f, h * 0.56f)
          lineTo(w * 0.56f, h * 0.50f)
          lineTo(w * 0.28f, h * 0.44f)
          close()
        }
      drawPath(path, color = tint)
    }

    RobloxIconType.VOLUME -> {
      val speaker =
        Path().apply {
          moveTo(w * 0.14f, h * 0.38f)
          lineTo(w * 0.34f, h * 0.38f)
          lineTo(w * 0.54f, h * 0.20f)
          lineTo(w * 0.54f, h * 0.80f)
          lineTo(w * 0.34f, h * 0.62f)
          lineTo(w * 0.14f, h * 0.62f)
          close()
        }
      drawPath(speaker, color = tint)
      drawRect(color = tint, topLeft = Offset(w * 0.65f, h * 0.36f), size = Size(w * 0.09f, h * 0.28f))
      drawRect(color = tint, topLeft = Offset(w * 0.80f, h * 0.26f), size = Size(w * 0.09f, h * 0.48f))
    }

    RobloxIconType.REFRESH -> {
      drawArc(
        color = tint,
        startAngle = 35f,
        sweepAngle = 290f,
        useCenter = false,
        topLeft = Offset(w * 0.18f, h * 0.18f),
        size = Size(w * 0.64f, h * 0.64f),
        style = stroke,
      )
      drawRect(color = tint, topLeft = Offset(w * 0.64f, h * 0.16f), size = Size(w * 0.18f, h * 0.18f))
    }

    RobloxIconType.EDIT -> {
      rotate(degrees = -45f, pivot = Offset(w * 0.5f, h * 0.5f)) {
        drawRoundRect(
          color = tint,
          topLeft = Offset(w * 0.20f, h * 0.39f),
          size = Size(w * 0.58f, h * 0.22f),
          cornerRadius = CornerRadius(w * 0.04f),
        )
      }
      drawRect(color = tint, topLeft = Offset(w * 0.18f, h * 0.78f), size = Size(w * 0.64f, h * 0.09f))
    }

    RobloxIconType.DELETE -> {
      drawRect(color = tint, topLeft = Offset(w * 0.36f, h * 0.12f), size = Size(w * 0.28f, h * 0.10f))
      drawRect(color = tint, topLeft = Offset(w * 0.18f, h * 0.22f), size = Size(w * 0.64f, h * 0.10f))
      drawRoundRect(
        color = tint,
        topLeft = Offset(w * 0.24f, h * 0.34f),
        size = Size(w * 0.52f, h * 0.52f),
        cornerRadius = CornerRadius(w * 0.06f),
        style = stroke,
      )
      drawRect(color = tint, topLeft = Offset(w * 0.44f, h * 0.46f), size = Size(w * 0.12f, h * 0.28f))
    }

    RobloxIconType.FIRE -> {
      val flame =
        Path().apply {
          moveTo(w * 0.50f, h * 0.10f)
          lineTo(w * 0.78f, h * 0.48f)
          lineTo(w * 0.72f, h * 0.84f)
          lineTo(w * 0.28f, h * 0.84f)
          lineTo(w * 0.22f, h * 0.48f)
          close()
        }
      drawPath(flame, color = tint)
      drawRect(
        color = Color.White.copy(alpha = 0.45f),
        topLeft = Offset(w * 0.42f, h * 0.52f),
        size = Size(w * 0.16f, h * 0.22f),
      )
    }

    RobloxIconType.TROPHY -> {
      drawRoundRect(
        color = tint,
        topLeft = Offset(w * 0.26f, h * 0.16f),
        size = Size(w * 0.48f, h * 0.40f),
        cornerRadius = CornerRadius(w * 0.08f),
      )
      drawRect(color = tint, topLeft = Offset(w * 0.14f, h * 0.22f), size = Size(w * 0.12f, h * 0.22f))
      drawRect(color = tint, topLeft = Offset(w * 0.74f, h * 0.22f), size = Size(w * 0.12f, h * 0.22f))
      drawRect(color = tint, topLeft = Offset(w * 0.43f, h * 0.56f), size = Size(w * 0.14f, h * 0.18f))
      drawRect(color = tint, topLeft = Offset(w * 0.28f, h * 0.74f), size = Size(w * 0.44f, h * 0.12f))
    }

    RobloxIconType.LIST -> {
      drawRect(color = tint, topLeft = Offset(w * 0.16f, h * 0.22f), size = Size(w * 0.14f, h * 0.14f))
      drawRect(color = tint, topLeft = Offset(w * 0.38f, h * 0.24f), size = Size(w * 0.46f, h * 0.10f))
      drawRect(color = tint, topLeft = Offset(w * 0.16f, h * 0.44f), size = Size(w * 0.14f, h * 0.14f))
      drawRect(color = tint, topLeft = Offset(w * 0.38f, h * 0.46f), size = Size(w * 0.46f, h * 0.10f))
      drawRect(color = tint, topLeft = Offset(w * 0.16f, h * 0.66f), size = Size(w * 0.14f, h * 0.14f))
      drawRect(color = tint, topLeft = Offset(w * 0.38f, h * 0.68f), size = Size(w * 0.46f, h * 0.10f))
    }

    RobloxIconType.CHECK -> {
      val path =
        Path().apply {
          moveTo(w * 0.18f, h * 0.52f)
          lineTo(w * 0.42f, h * 0.76f)
          lineTo(w * 0.84f, h * 0.26f)
        }
      drawPath(path, color = tint, style = stroke)
    }

    RobloxIconType.LOCK -> {
      drawRoundRect(
        color = tint,
        topLeft = Offset(w * 0.30f, h * 0.14f),
        size = Size(w * 0.40f, h * 0.34f),
        cornerRadius = CornerRadius(w * 0.14f),
        style = stroke,
      )
      drawRoundRect(
        color = tint,
        topLeft = Offset(w * 0.20f, h * 0.42f),
        size = Size(w * 0.60f, h * 0.44f),
        cornerRadius = CornerRadius(w * 0.08f),
      )
    }

    RobloxIconType.CLOCK -> {
      drawRoundRect(
        color = tint,
        topLeft = Offset(w * 0.14f, h * 0.14f),
        size = Size(w * 0.72f, h * 0.72f),
        cornerRadius = CornerRadius(w * 0.18f),
        style = stroke,
      )
      drawRect(color = tint, topLeft = Offset(w * 0.45f, h * 0.28f), size = Size(w * 0.10f, h * 0.26f))
      drawRect(color = tint, topLeft = Offset(w * 0.45f, h * 0.48f), size = Size(w * 0.24f, h * 0.10f))
    }

    RobloxIconType.PALETTE -> {
      drawRoundRect(
        color = tint,
        topLeft = Offset(w * 0.14f, h * 0.18f),
        size = Size(w * 0.72f, h * 0.64f),
        cornerRadius = CornerRadius(w * 0.16f),
        style = stroke,
      )
      drawRect(color = tint, topLeft = Offset(w * 0.26f, h * 0.32f), size = Size(w * 0.14f, h * 0.14f))
      drawRect(color = tint, topLeft = Offset(w * 0.48f, h * 0.28f), size = Size(w * 0.14f, h * 0.14f))
      drawRect(color = tint, topLeft = Offset(w * 0.62f, h * 0.46f), size = Size(w * 0.14f, h * 0.14f))
    }

    RobloxIconType.STOP -> {
      drawRoundRect(
        color = tint,
        topLeft = Offset(w * 0.22f, h * 0.22f),
        size = Size(w * 0.56f, h * 0.56f),
        cornerRadius = CornerRadius(w * 0.10f),
      )
    }
  }
}
