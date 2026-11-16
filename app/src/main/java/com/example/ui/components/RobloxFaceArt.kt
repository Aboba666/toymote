package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.EmotionDef
import com.example.domain.FaceRender
import com.example.domain.VectorFaceStyle

@Composable
fun RobloxFaceBadge(
  emotion: EmotionDef,
  size: Dp,
  modifier: Modifier = Modifier,
  showBorder: Boolean = true,
) {
  val shape = RoundedCornerShape(percent = 28)
  val borderColor =
    if (showBorder) Color(emotion.accentColorHex).copy(alpha = 0.45f)
    else Color.Transparent

  when (val render = emotion.render) {
    is FaceRender.ImageFace -> {
      val bgColor = if (render.isDarkArtwork) Color(0xFF0B0812) else Color.White
      Box(
        modifier =
          modifier
            .size(size)
            .clip(shape)
            .background(bgColor)
            .then(
              if (showBorder) Modifier.border(1.5.dp, borderColor, shape) else Modifier
            ),
        contentAlignment = Alignment.Center,
      ) {
        Image(
          painter = painterResource(id = render.resId),
          contentDescription = emotion.name,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize().padding(if (render.isDarkArtwork) 1.dp else 2.dp),
        )
      }
    }
    is FaceRender.VectorFace -> {
      val headTint =
        Brush.verticalGradient(
          colors = listOf(Color(0xFFFFFBF4), Color(0xFFF3EBFF))
        )
      Box(
        modifier =
          modifier
            .size(size)
            .clip(shape)
            .background(headTint)
            .then(
              if (showBorder) {
                Modifier.border(
                  1.5.dp,
                  MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                  shape,
                )
              } else {
                Modifier
              }
            ),
        contentAlignment = Alignment.Center,
      ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          drawClassicRobloxVectorFace(render.style, Color(emotion.accentColorHex))
        }
      }
    }
  }
}

private fun DrawScope.drawClassicRobloxVectorFace(style: VectorFaceStyle, accent: Color) {
  val w = size.width
  val h = size.height
  val ink = Color(0xFF1A122B)
  val strokeW = (w * 0.068f).coerceAtLeast(1.8f)
  val thinStroke = (w * 0.05f).coerceAtLeast(1.4f)

  // Subtle top glossy Roblox-toy highlight
  drawRoundRect(
    color = Color.White.copy(alpha = 0.55f),
    topLeft = Offset(w * 0.16f, h * 0.06f),
    size = Size(w * 0.68f, h * 0.10f),
    cornerRadius = CornerRadius(w * 0.05f, w * 0.05f),
  )

  val leftEyeX = w * 0.34f
  val rightEyeX = w * 0.66f
  val eyeY = h * 0.39f
  val eyeW = w * 0.10f
  val eyeH = h * 0.16f

  when (style) {
    VectorFaceStyle.VESELYI -> {
      // Classic Roblox ":D" wide cheerful eyes + big open smile
      drawOvalEye(leftEyeX, eyeY, eyeW, eyeH, ink)
      drawOvalEye(rightEyeX, eyeY, eyeW, eyeH, ink)
      // Rosy pixel cheeks
      drawRect(
        color = Color(0xFFFF88B8).copy(alpha = 0.45f),
        topLeft = Offset(w * 0.14f, h * 0.48f),
        size = Size(w * 0.12f, h * 0.07f),
      )
      drawRect(
        color = Color(0xFFFF88B8).copy(alpha = 0.45f),
        topLeft = Offset(w * 0.74f, h * 0.48f),
        size = Size(w * 0.12f, h * 0.07f),
      )
      // Big classic D smile
      val mouthPath =
        Path().apply {
          moveTo(w * 0.28f, h * 0.58f)
          lineTo(w * 0.72f, h * 0.58f)
          quadraticTo(w * 0.70f, h * 0.80f, w * 0.50f, h * 0.80f)
          quadraticTo(w * 0.30f, h * 0.80f, w * 0.28f, h * 0.58f)
          close()
        }
      drawPath(mouthPath, color = ink)
      // Tongue / inner smile accent
      drawRoundRect(
        color = Color(0xFFFF6D9D),
        topLeft = Offset(w * 0.39f, h * 0.67f),
        size = Size(w * 0.22f, h * 0.10f),
        cornerRadius = CornerRadius(w * 0.05f, w * 0.05f),
      )
    }

    VectorFaceStyle.SCHASTLIVYI -> {
      // Happy arched "^ ^" eyes + radiant smile
      drawArc(
        color = ink,
        startAngle = 195f,
        sweepAngle = 150f,
        useCenter = false,
        topLeft = Offset(leftEyeX - eyeW * 1.05f, eyeY - eyeH * 0.4f),
        size = Size(eyeW * 2.1f, eyeH * 1.1f),
        style = Stroke(width = strokeW, cap = StrokeCap.Round),
      )
      drawArc(
        color = ink,
        startAngle = 195f,
        sweepAngle = 150f,
        useCenter = false,
        topLeft = Offset(rightEyeX - eyeW * 1.05f, eyeY - eyeH * 0.4f),
        size = Size(eyeW * 2.1f, eyeH * 1.1f),
        style = Stroke(width = strokeW, cap = StrokeCap.Round),
      )
      // Classic Roblox curved grin
      val grin =
        Path().apply {
          moveTo(w * 0.27f, h * 0.60f)
          quadraticTo(w * 0.50f, h * 0.80f, w * 0.73f, h * 0.60f)
        }
      drawPath(grin, color = ink, style = Stroke(width = strokeW, cap = StrokeCap.Round))
      // Smile dimples
      drawLine(
        color = ink,
        start = Offset(w * 0.24f, h * 0.56f),
        end = Offset(w * 0.30f, h * 0.63f),
        strokeWidth = thinStroke,
        cap = StrokeCap.Round,
      )
      drawLine(
        color = ink,
        start = Offset(w * 0.76f, h * 0.56f),
        end = Offset(w * 0.70f, h * 0.63f),
        strokeWidth = thinStroke,
        cap = StrokeCap.Round,
      )
    }

    VectorFaceStyle.VLYUBLENNYI -> {
      // Pixel heart eyes + sweet smile
      val heartColor = Color(0xFFFF3D8B)
      drawPixelHeart(Offset(leftEyeX, eyeY), w * 0.14f, heartColor)
      drawPixelHeart(Offset(rightEyeX, eyeY), w * 0.14f, heartColor)
      val smile =
        Path().apply {
          moveTo(w * 0.34f, h * 0.64f)
          quadraticTo(w * 0.50f, h * 0.76f, w * 0.66f, h * 0.64f)
        }
      drawPath(smile, color = ink, style = Stroke(width = strokeW, cap = StrokeCap.Round))
    }

    VectorFaceStyle.UDIVLENNYI -> {
      // Raised brows + wide round eyes + surprised "O" mouth
      drawLine(
        color = ink,
        start = Offset(w * 0.24f, h * 0.21f),
        end = Offset(w * 0.42f, h * 0.18f),
        strokeWidth = thinStroke,
        cap = StrokeCap.Round,
      )
      drawLine(
        color = ink,
        start = Offset(w * 0.58f, h * 0.18f),
        end = Offset(w * 0.76f, h * 0.21f),
        strokeWidth = thinStroke,
        cap = StrokeCap.Round,
      )
      drawCircle(color = ink, radius = w * 0.085f, center = Offset(leftEyeX, eyeY + h * 0.02f))
      drawCircle(color = ink, radius = w * 0.085f, center = Offset(rightEyeX, eyeY + h * 0.02f))
      drawCircle(
        color = Color.White,
        radius = w * 0.025f,
        center = Offset(leftEyeX - w * 0.02f, eyeY - h * 0.01f),
      )
      drawCircle(
        color = Color.White,
        radius = w * 0.025f,
        center = Offset(rightEyeX - w * 0.02f, eyeY - h * 0.01f),
      )
      drawOval(
        color = ink,
        topLeft = Offset(w * 0.40f, h * 0.59f),
        size = Size(w * 0.20f, h * 0.18f),
      )
      drawOval(
        color = Color(0xFFFF7AA2),
        topLeft = Offset(w * 0.44f, h * 0.64f),
        size = Size(w * 0.12f, h * 0.09f),
      )
    }

    VectorFaceStyle.USTALYI -> {
      // Half-lidded blocky Roblox eyes + droopy bags + flat wavy mouth
      drawLine(
        color = ink,
        start = Offset(w * 0.24f, h * 0.34f),
        end = Offset(w * 0.44f, h * 0.36f),
        strokeWidth = strokeW,
        cap = StrokeCap.Square,
      )
      drawLine(
        color = ink,
        start = Offset(w * 0.56f, h * 0.36f),
        end = Offset(w * 0.76f, h * 0.34f),
        strokeWidth = strokeW,
        cap = StrokeCap.Square,
      )
      drawArc(
        color = ink,
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(w * 0.25f, h * 0.31f),
        size = Size(w * 0.18f, h * 0.14f),
        style = Stroke(width = thinStroke),
      )
      drawArc(
        color = ink,
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(w * 0.57f, h * 0.31f),
        size = Size(w * 0.18f, h * 0.14f),
        style = Stroke(width = thinStroke),
      )
      // Under-eye tiredness lines
      drawLine(
        color = accent.copy(alpha = 0.65f),
        start = Offset(w * 0.27f, h * 0.50f),
        end = Offset(w * 0.41f, h * 0.50f),
        strokeWidth = thinStroke * 0.8f,
        cap = StrokeCap.Round,
      )
      drawLine(
        color = accent.copy(alpha = 0.65f),
        start = Offset(w * 0.59f, h * 0.50f),
        end = Offset(w * 0.73f, h * 0.50f),
        strokeWidth = thinStroke * 0.8f,
        cap = StrokeCap.Round,
      )
      // Small yawn/flat mouth
      drawRoundRect(
        color = ink,
        topLeft = Offset(w * 0.37f, h * 0.65f),
        size = Size(w * 0.26f, h * 0.08f),
        cornerRadius = CornerRadius(w * 0.04f, w * 0.04f),
      )
    }

    VectorFaceStyle.TREVOZHNYI -> {
      // Worried angled brows + wide eyes + zigzag mouth + sweat drop
      drawLine(
        color = ink,
        start = Offset(w * 0.22f, h * 0.29f),
        end = Offset(w * 0.42f, h * 0.22f),
        strokeWidth = thinStroke,
        cap = StrokeCap.Round,
      )
      drawLine(
        color = ink,
        start = Offset(w * 0.58f, h * 0.22f),
        end = Offset(w * 0.78f, h * 0.29f),
        strokeWidth = thinStroke,
        cap = StrokeCap.Round,
      )
      drawOvalEye(leftEyeX, eyeY + h * 0.02f, eyeW * 0.9f, eyeH * 0.85f, ink)
      drawOvalEye(rightEyeX, eyeY + h * 0.02f, eyeW * 0.9f, eyeH * 0.85f, ink)
      // Sweat drop on side
      val drop =
        Path().apply {
          moveTo(w * 0.82f, h * 0.24f)
          quadraticTo(w * 0.89f, h * 0.36f, w * 0.82f, h * 0.40f)
          quadraticTo(w * 0.75f, h * 0.36f, w * 0.82f, h * 0.24f)
          close()
        }
      drawPath(drop, color = Color(0xFF56B4FF))
      // Wavy worried mouth
      val wave =
        Path().apply {
          moveTo(w * 0.27f, h * 0.67f)
          lineTo(w * 0.38f, h * 0.62f)
          lineTo(w * 0.50f, h * 0.68f)
          lineTo(w * 0.62f, h * 0.62f)
          lineTo(w * 0.73f, h * 0.67f)
        }
      drawPath(
        wave,
        color = ink,
        style = Stroke(width = thinStroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
      )
    }

    VectorFaceStyle.NEITRALNYI -> {
      // Classic Roblox ":/" or straight pixel eyes + flat line mouth
      drawOvalEye(leftEyeX, eyeY, eyeW * 0.9f, eyeH * 0.9f, ink)
      drawOvalEye(rightEyeX, eyeY, eyeW * 0.9f, eyeH * 0.9f, ink)
      drawLine(
        color = ink,
        start = Offset(w * 0.32f, h * 0.67f),
        end = Offset(w * 0.68f, h * 0.67f),
        strokeWidth = strokeW,
        cap = StrokeCap.Round,
      )
    }

    VectorFaceStyle.RAZOCHAROVANNYI -> {
      // Droopy brows + flat eyes + downturned frown
      drawLine(
        color = ink,
        start = Offset(w * 0.23f, h * 0.28f),
        end = Offset(w * 0.42f, h * 0.25f),
        strokeWidth = thinStroke,
        cap = StrokeCap.Round,
      )
      drawLine(
        color = ink,
        start = Offset(w * 0.58f, h * 0.25f),
        end = Offset(w * 0.77f, h * 0.28f),
        strokeWidth = thinStroke,
        cap = StrokeCap.Round,
      )
      drawOvalEye(leftEyeX, eyeY + h * 0.03f, eyeW * 0.85f, eyeH * 0.75f, ink)
      drawOvalEye(rightEyeX, eyeY + h * 0.03f, eyeW * 0.85f, eyeH * 0.75f, ink)
      val frown =
        Path().apply {
          moveTo(w * 0.30f, h * 0.72f)
          quadraticTo(w * 0.50f, h * 0.57f, w * 0.70f, h * 0.72f)
        }
      drawPath(frown, color = ink, style = Stroke(width = strokeW, cap = StrokeCap.Round))
    }

    VectorFaceStyle.RAZDRAZHENNYI -> {
      // Angry V-shaped Roblox eyebrows + sharp eyes + clenched jagged frown
      drawLine(
        color = ink,
        start = Offset(w * 0.21f, h * 0.22f),
        end = Offset(w * 0.44f, h * 0.31f),
        strokeWidth = strokeW,
        cap = StrokeCap.Square,
      )
      drawLine(
        color = ink,
        start = Offset(w * 0.79f, h * 0.22f),
        end = Offset(w * 0.56f, h * 0.31f),
        strokeWidth = strokeW,
        cap = StrokeCap.Square,
      )
      drawOvalEye(leftEyeX, eyeY + h * 0.04f, eyeW * 0.9f, eyeH * 0.75f, ink)
      drawOvalEye(rightEyeX, eyeY + h * 0.04f, eyeW * 0.9f, eyeH * 0.75f, ink)
      // Clenched angry mouth
      val mouth =
        Path().apply {
          moveTo(w * 0.28f, h * 0.70f)
          quadraticTo(w * 0.50f, h * 0.58f, w * 0.72f, h * 0.70f)
        }
      drawPath(mouth, color = ink, style = Stroke(width = strokeW, cap = StrokeCap.Round))
    }
  }
}

private fun DrawScope.drawOvalEye(
  cx: Float,
  cy: Float,
  halfW: Float,
  halfH: Float,
  color: Color,
) {
  drawOval(
    color = color,
    topLeft = Offset(cx - halfW, cy - halfH),
    size = Size(halfW * 2f, halfH * 2f),
  )
  drawCircle(
    color = Color.White,
    radius = halfW * 0.34f,
    center = Offset(cx - halfW * 0.25f, cy - halfH * 0.35f),
  )
}

private fun DrawScope.drawPixelHeart(center: Offset, radius: Float, color: Color) {
  val path =
    Path().apply {
      moveTo(center.x, center.y + radius * 0.85f)
      cubicTo(
        center.x - radius * 1.4f,
        center.y + radius * 0.1f,
        center.x - radius * 0.95f,
        center.y - radius * 1.0f,
        center.x,
        center.y - radius * 0.3f,
      )
      cubicTo(
        center.x + radius * 0.95f,
        center.y - radius * 1.0f,
        center.x + radius * 1.4f,
        center.y + radius * 0.1f,
        center.x,
        center.y + radius * 0.85f,
      )
      close()
    }
  drawPath(path, color = color)
}
