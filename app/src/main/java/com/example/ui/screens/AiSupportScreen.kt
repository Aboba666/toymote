package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.ai.AiSupportUiState
import com.example.ai.LiveVoiceStatus
import com.example.ui.components.MattoCompanyLogo
import kotlinx.coroutines.delay

@Composable
fun AiSupportScreen(
  aiState: AiSupportUiState,
  onConnect: () -> Unit,
  onDisconnect: () -> Unit,
  onToggleMute: () -> Unit,
  onToggleSpeakerphone: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current

  val micPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      if (granted) {
        onConnect()
      }
    }

  val isConnected = aiState.status == LiveVoiceStatus.CONNECTED
  val isConnecting = aiState.status == LiveVoiceStatus.CONNECTING

  val handleConnectToggle = {
    if (isConnected || isConnecting) {
      onDisconnect()
    } else {
      val granted =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
          PackageManager.PERMISSION_GRANTED
      if (granted) {
        onConnect()
      } else {
        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
      }
    }
  }

  val statusText =
    when {
      isConnecting -> "Подключаюсь к ГАЗели..."
      isConnected && aiState.isAiSpeaking -> "ГАЗель говорит..."
      isConnected && aiState.isMicMuted -> "В эфире • Микрофон выключен"
      isConnected -> "В эфире • ГАЗель слушает тебя"
      aiState.status == LiveVoiceStatus.ERROR -> "Не удалось подключиться"
      else -> "Спит • Нажми «Подключиться»"
    }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(
      modifier =
        Modifier.widthIn(max = 600.dp)
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 14.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      // Top Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = "ГАЗель",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Text(
            text = "Живой голосовой собеседник",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        MattoCompanyLogo(compact = true)
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Central Talking Avatar Stage (eyes closed when disconnected, open when connected)
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        GazelleTalkingAvatar(
          isConnected = isConnected,
          isSpeaking = aiState.isAiSpeaking,
          speakingAmplitude = aiState.aiSpeakingAmplitude,
          isMicMuted = aiState.isMicMuted,
        )

        // Live Status Pill
        Surface(
          color =
            when {
              isConnected && aiState.isAiSpeaking -> MaterialTheme.colorScheme.primaryContainer
              isConnected && aiState.isMicMuted -> MaterialTheme.colorScheme.errorContainer
              isConnected -> MaterialTheme.colorScheme.secondaryContainer
              aiState.status == LiveVoiceStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
              else -> MaterialTheme.colorScheme.surfaceContainerHigh
            },
          shape = RoundedCornerShape(50),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            if (isConnected) {
              Icon(
                imageVector =
                  if (aiState.isMicMuted && !aiState.isAiSpeaking) Icons.Rounded.MicOff
                  else Icons.Rounded.GraphicEq,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint =
                  if (aiState.isMicMuted && !aiState.isAiSpeaking) {
                    MaterialTheme.colorScheme.onErrorContainer
                  } else {
                    MaterialTheme.colorScheme.primary
                  },
              )
            }
            Text(
              text = statusText,
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold,
              color =
                when {
                  aiState.status == LiveVoiceStatus.ERROR ||
                    (isConnected && aiState.isMicMuted && !aiState.isAiSpeaking) ->
                    MaterialTheme.colorScheme.onErrorContainer
                  isConnected -> MaterialTheme.colorScheme.onPrimaryContainer
                  else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
          }
        }

        AnimatedVisibility(visible = !aiState.errorMessage.isNullOrBlank()) {
          Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = aiState.errorMessage.orEmpty(),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onErrorContainer,
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Live Call Controls (Connect/Disconnect + Mic Mute + Speakerphone Loud Mode)
      Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(28.dp),
        modifier =
          Modifier.fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(28.dp)),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Button(
            onClick = handleConnectToggle,
            shape = RoundedCornerShape(18.dp),
            colors =
              if (isConnected || isConnecting) {
                ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.error,
                  contentColor = MaterialTheme.colorScheme.onError,
                )
              } else {
                ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.primary,
                  contentColor = MaterialTheme.colorScheme.onPrimary,
                )
              },
            modifier = Modifier.fillMaxWidth().height(56.dp).testTag("gazelle_connect_button"),
          ) {
            if (isConnecting) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.5.dp,
                color = MaterialTheme.colorScheme.onError,
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "Отменить",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
            } else {
              Icon(
                imageVector = if (isConnected) Icons.Rounded.CallEnd else Icons.Rounded.Call,
                contentDescription = if (isConnected) "Отключиться" else "Подключиться",
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = if (isConnected) "Отключиться" else "Подключиться",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            FilledTonalButton(
              onClick = onToggleMute,
              shape = RoundedCornerShape(18.dp),
              colors =
                if (aiState.isMicMuted) {
                  ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                  )
                } else {
                  ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                  )
                },
              modifier = Modifier.weight(1f).height(54.dp).testTag("gazelle_mute_button"),
            ) {
              Icon(
                imageVector = if (aiState.isMicMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                contentDescription =
                  if (aiState.isMicMuted) "Включить микрофон" else "Выключить микрофон",
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (aiState.isMicMuted) "Микрофон выкл" else "Микрофон вкл",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }

            FilledTonalButton(
              onClick = onToggleSpeakerphone,
              shape = RoundedCornerShape(18.dp),
              colors =
                if (aiState.isSpeakerphoneOn) {
                  ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                  )
                } else {
                  ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                },
              modifier = Modifier.weight(1f).height(54.dp).testTag("gazelle_speaker_button"),
            ) {
              Icon(
                imageVector =
                  if (aiState.isSpeakerphoneOn) Icons.Rounded.VolumeUp else Icons.Rounded.Hearing,
                contentDescription =
                  if (aiState.isSpeakerphoneOn) "Выключить громкую связь" else "Включить громкую связь",
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (aiState.isSpeakerphoneOn) "Громкая связь" else "Обычный звук",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun GazelleTalkingAvatar(
  isConnected: Boolean,
  isSpeaking: Boolean,
  speakingAmplitude: Float,
  isMicMuted: Boolean,
) {
  // Subtle natural blinking while connected
  var isBlinking by remember { mutableStateOf(false) }
  LaunchedEffect(isConnected) {
    if (!isConnected) {
      isBlinking = false
    } else {
      while (true) {
        delay(4200L)
        isBlinking = true
        delay(130L)
        isBlinking = false
      }
    }
  }

  // eyeClosedProgress: 1f = fully closed (when disconnected or blinking), 0f = wide open
  val eyeClosedProgress by
    animateFloatAsState(
      targetValue = if (!isConnected || isBlinking) 1f else 0f,
      animationSpec = tween(durationMillis = if (isBlinking) 95 else 240, easing = FastOutSlowInEasing),
      label = "gazelle_eyes_closed",
    )

  val smoothAmp by
    animateFloatAsState(
      targetValue = if (isConnected && isSpeaking) speakingAmplitude.coerceIn(0.14f, 1f) else 0f,
      animationSpec = spring(dampingRatio = 0.58f, stiffness = 480f),
      label = "voice_amp",
    )

  val infiniteTransition = rememberInfiniteTransition(label = "gazelle_idle_talk")
  val idleBreath by
    infiniteTransition.animateFloat(
      initialValue = 1f,
      targetValue = 1.024f,
      animationSpec = infiniteRepeatable(tween(2100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
      label = "idle_breath",
    )
  val lipFlutter by
    infiniteTransition.animateFloat(
      initialValue = 0.38f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(tween(135), RepeatMode.Reverse),
      label = "lip_flutter",
    )

  val activeMouthOpen =
    if (isConnected && isSpeaking) (smoothAmp * 0.72f + smoothAmp * 0.28f * lipFlutter).coerceIn(0f, 1f)
    else 0f

  val headScale = if (isConnected) (1f + smoothAmp * 0.042f) else idleBreath
  val primaryColor = MaterialTheme.colorScheme.primary
  val tertiaryColor = MaterialTheme.colorScheme.tertiary

  Box(
    modifier = Modifier.size(252.dp),
    contentAlignment = Alignment.Center,
  ) {
    // Outer pulsing voice aura rings
    Canvas(modifier = Modifier.fillMaxSize()) {
      val center = Offset(size.width / 2f, size.height / 2f)
      val baseRadius = size.minDimension * 0.39f
      if (isConnected) {
        drawCircle(
          brush =
            Brush.radialGradient(
              colors =
                listOf(
                  primaryColor.copy(alpha = 0.24f + activeMouthOpen * 0.22f),
                  tertiaryColor.copy(alpha = 0.08f),
                  Color.Transparent,
                ),
              center = center,
              radius = baseRadius * (1.28f + activeMouthOpen * 0.28f),
            ),
          radius = baseRadius * (1.28f + activeMouthOpen * 0.28f),
          center = center,
        )
        drawCircle(
          color = primaryColor.copy(alpha = 0.32f + activeMouthOpen * 0.45f),
          radius = baseRadius * (1.06f + activeMouthOpen * 0.14f),
          center = center,
          style = Stroke(width = 4.dp.toPx()),
        )
      } else {
        drawCircle(
          color = primaryColor.copy(alpha = 0.16f),
          radius = baseRadius * 1.07f,
          center = center,
          style = Stroke(width = 2.5.dp.toPx()),
        )
      }
    }

    // Face Card with user's attached face (face_spokush) + eyelid & mouth animation overlays
    Box(
      modifier =
        Modifier.size(204.dp)
          .graphicsLayer {
            scaleX = headScale
            scaleY = headScale
            translationY = -activeMouthOpen * 5f
          }
          .clip(RoundedCornerShape(56.dp))
          .background(Color.White)
          .border(
            width = if (isConnected) 3.dp else 2.dp,
            color =
              if (isConnected) primaryColor else MaterialTheme.colorScheme.outlineVariant,
            shape = RoundedCornerShape(56.dp),
          ),
      contentAlignment = Alignment.Center,
    ) {
      Image(
        painter = painterResource(id = R.drawable.face_spokush),
        contentDescription =
          if (isConnected) "Лицо ГАЗели с открытыми глазами" else "Лицо ГАЗели с закрытыми глазами",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize().padding(6.dp),
      )

      // Closed-eyes eyelid overlay when disconnected (or briefly blinking)
      if (eyeClosedProgress > 0.02f) {
        Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
          val w = size.width
          val h = size.height
          val inkColor = Color(0xFF14111D)
          val lidStroke = (w * 0.026f).coerceAtLeast(3.5f)
          val lashStroke = (w * 0.017f).coerceAtLeast(2.2f)

          // Mask left & right open-eye regions smoothly as eyelids close (eyebrows stay visible above y = 0.21f)
          val maskH = h * 0.23f * eyeClosedProgress
          drawOval(
            color = Color.White,
            topLeft = Offset(w * 0.24f, h * 0.215f),
            size = Size(w * 0.23f, maskH),
          )
          drawOval(
            color = Color.White,
            topLeft = Offset(w * 0.53f, h * 0.215f),
            size = Size(w * 0.23f, maskH),
          )

          if (eyeClosedProgress > 0.55f) {
            val lineAlpha = ((eyeClosedProgress - 0.55f) / 0.45f).coerceIn(0f, 1f)
            val eyeColor = inkColor.copy(alpha = lineAlpha)

            // Left closed eye arc + delicate side eyelashes
            val leftLid =
              Path().apply {
                moveTo(w * 0.305f, h * 0.345f)
                quadraticTo(w * 0.388f, h * 0.398f, w * 0.452f, h * 0.345f)
              }
            drawPath(
              path = leftLid,
              color = eyeColor,
              style = Stroke(width = lidStroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            drawLine(
              color = eyeColor,
              start = Offset(w * 0.305f, h * 0.345f),
              end = Offset(w * 0.278f, h * 0.322f),
              strokeWidth = lashStroke,
              cap = StrokeCap.Round,
            )
            drawLine(
              color = eyeColor,
              start = Offset(w * 0.328f, h * 0.365f),
              end = Offset(w * 0.308f, h * 0.395f),
              strokeWidth = lashStroke,
              cap = StrokeCap.Round,
            )

            // Right closed eye arc + delicate side eyelashes
            val rightLid =
              Path().apply {
                moveTo(w * 0.548f, h * 0.345f)
                quadraticTo(w * 0.612f, h * 0.398f, w * 0.695f, h * 0.345f)
              }
            drawPath(
              path = rightLid,
              color = eyeColor,
              style = Stroke(width = lidStroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            drawLine(
              color = eyeColor,
              start = Offset(w * 0.695f, h * 0.345f),
              end = Offset(w * 0.722f, h * 0.322f),
              strokeWidth = lashStroke,
              cap = StrokeCap.Round,
            )
            drawLine(
              color = eyeColor,
              start = Offset(w * 0.672f, h * 0.365f),
              end = Offset(w * 0.692f, h * 0.395f),
              strokeWidth = lashStroke,
              cap = StrokeCap.Round,
            )
          }
        }
      }

      // Animated talking mouth synced to Gemini Live audio output
      if (activeMouthOpen > 0.02f) {
        Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
          val w = size.width
          val h = size.height
          val mouthCx = w * 0.50f
          val mouthCy = h * 0.768f
          val openH = h * (0.018f + 0.092f * activeMouthOpen)
          val openW = w * (0.14f + 0.045f * activeMouthOpen)

          drawOval(
            color = Color(0xFFF31E5C),
            topLeft = Offset(mouthCx - openW * 0.72f, mouthCy - openH * 0.85f),
            size = Size(openW * 1.44f, openH * 1.70f),
          )
          drawOval(
            color = Color(0xFF230B16),
            topLeft = Offset(mouthCx - openW * 0.50f, mouthCy - openH * 0.55f),
            size = Size(openW, openH * 1.10f),
          )
          if (activeMouthOpen > 0.35f) {
            drawArc(
              color = Color.White,
              startAngle = 200f,
              sweepAngle = 140f,
              useCenter = false,
              topLeft = Offset(mouthCx - openW * 0.34f, mouthCy - openH * 0.42f),
              size = Size(openW * 0.68f, openH * 0.42f),
              style = Stroke(width = 2.4f, cap = StrokeCap.Round),
            )
          }
        }
      }

      // Small corner mic-muted badge if muted while connected
      if (isConnected && isMicMuted) {
        Surface(
          color = MaterialTheme.colorScheme.error,
          shape = CircleShape,
          modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp).size(34.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Rounded.MicOff,
              contentDescription = "Микрофон выключен",
              tint = MaterialTheme.colorScheme.onError,
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }
    }
  }
}
