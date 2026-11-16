package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.MoodEntryEntity
import com.example.domain.EmotionCatalog
import com.example.domain.ToymoteDate
import com.example.ui.ToymoteUiState
import com.example.ui.components.MattoCompanyLogo
import com.example.ui.components.RobloxFaceBadge
import com.example.ui.components.WidgetPromoBannerCard
import com.example.ui.components.WidgetSetupDialog
import com.example.ui.theme.LocalToymoteColors
import com.example.widgets.PinWidgetOutcome
import com.example.widgets.ToymoteWidgetKind
import com.example.widgets.WidgetPinHelper

@Composable
fun CalendarScreen(
  uiState: ToymoteUiState,
  onPrevMonth: () -> Unit,
  onNextMonth: () -> Unit,
  onJumpToTodayMonth: () -> Unit,
  onDayClick: (ToymoteDate, List<MoodEntryEntity>) -> Unit,
  onOpenSettings: () -> Unit,
  onMarkWidgetPromptShown: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val today = ToymoteDate.today()
  val extended = LocalToymoteColors.current
  val context = LocalContext.current

  var manualChooserOpen by rememberSaveable { mutableStateOf(false) }

  val showAutoOneTimeDialog = uiState.isSettingsLoaded && !uiState.settings.widgetPromptShown
  val showWidgetDialog = showAutoOneTimeDialog || manualChooserOpen

  val triggerPinWidget: (ToymoteWidgetKind) -> Unit = { kind ->
    if (showAutoOneTimeDialog) {
      onMarkWidgetPromptShown()
    }
    manualChooserOpen = false
    val outcome = WidgetPinHelper.requestPinWidget(context, kind)
    if (outcome == PinWidgetOutcome.MANUAL_FALLBACK_NEEDED) {
      Toast.makeText(
        context,
        "Удерживайте пустое место на главном экране → Виджеты → toymote",
        Toast.LENGTH_LONG,
      ).show()
    }
  }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 640.dp).fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Brand Header
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Surface(
              color = MaterialTheme.colorScheme.primary,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.size(40.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Rounded.AutoAwesome,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onPrimary,
                  modifier = Modifier.size(22.dp),
                )
              }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
              )
              MattoCompanyLogo(compact = true)
            }
          }

          IconButton(
            onClick = onOpenSettings,
            modifier = Modifier.testTag("settings_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Settings,
              contentDescription = "Настройки уведомлений и темы",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      // Hero Streak Card
      item {
        val streak = uiState.streakStats
        val gradientBrush =
          Brush.linearGradient(
            colors =
              listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.tertiary,
              )
          )

        Card(
          shape = MaterialTheme.shapes.large,
          elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
          modifier = Modifier.fillMaxWidth().testTag("streak_hero_card"),
        ) {
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .background(gradientBrush)
                .padding(horizontal = 20.dp, vertical = 18.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Text(
                    text = "🔥",
                    style = MaterialTheme.typography.titleMedium,
                  )
                  Text(
                    text = stringResource(id = R.string.streak_title).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                  )
                }

                Text(
                  text = ToymoteDate.formatDaysStreakRu(streak.currentStreak),
                  style = MaterialTheme.typography.headlineLarge,
                  fontWeight = FontWeight.ExtraBold,
                  color = MaterialTheme.colorScheme.onPrimary,
                )

                Text(
                  text =
                    when {
                      streak.currentStreak == 0 ->
                        "Нажми «+», чтобы отметить первое настроение дня"
                      streak.hasEntryToday ->
                        "Сегодняшняя эмоция в дневнике! Так держать"
                      else ->
                        "Отметь настроение сегодня, чтобы продлить серию"
                    },
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                )
              }

              Spacer(modifier = Modifier.width(12.dp))

              Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.18f),
                modifier = Modifier.size(68.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Rounded.LocalFireDepartment,
                    contentDescription = "Огонь ударной серии",
                    tint = extended.streakStart,
                    modifier = Modifier.size(40.dp),
                  )
                }
              }
            }
          }
        }
      }

      // Main Month Calendar Card
      item {
        val monthAnchor = uiState.currentMonthAnchor
        val isCurrentMonth =
          monthAnchor.year == today.year && monthAnchor.month == today.month

        Card(
          colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = MaterialTheme.shapes.large,
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          modifier =
            Modifier.fillMaxWidth()
              .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
                MaterialTheme.shapes.large,
              ),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            // Month Navigator Row
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              IconButton(
                onClick = onPrevMonth,
                modifier = Modifier.testTag("prev_month_button"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                  contentDescription = "Предыдущий месяц",
                )
              }

              Surface(
                color =
                  if (isCurrentMonth) MaterialTheme.colorScheme.surfaceContainerLow
                  else MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(14.dp),
                modifier =
                  Modifier.clip(RoundedCornerShape(14.dp))
                    .clickable(enabled = !isCurrentMonth, onClick = onJumpToTodayMonth)
                    .testTag("month_title_button"),
              ) {
                Text(
                  text = monthAnchor.monthYearTitleRu(),
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color =
                    if (isCurrentMonth) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
              }

              IconButton(
                onClick = onNextMonth,
                modifier = Modifier.testTag("next_month_button"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                  contentDescription = "Следующий месяц",
                )
              }
            }

            // Weekday Headers
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              ToymoteDate.WEEKDAYS_RU.forEachIndexed { idx, dayLabel ->
                Text(
                  text = dayLabel,
                  style = MaterialTheme.typography.labelMedium,
                  color =
                    if (idx >= 5) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                  textAlign = TextAlign.Center,
                  modifier = Modifier.weight(1f),
                )
              }
            }

            // Month Grid
            val firstDay = ToymoteDate(monthAnchor.year, monthAnchor.month, 1)
            val leadingBlanks = firstDay.dayOfWeekMondayZero()
            val totalDays = firstDay.daysInMonth()
            val totalCells = leadingBlanks + totalDays
            val rows = (totalCells + 6) / 7

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              for (row in 0 until rows) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNum = cellIndex - leadingBlanks + 1
                    if (dayNum in 1..totalDays) {
                      val cellDate = ToymoteDate(monthAnchor.year, monthAnchor.month, dayNum)
                      val dayEntries =
                        uiState.entriesByDay[cellDate.epochDay]
                          .orEmpty()
                          .sortedByDescending { it.timestampMillis }
                      val isToday = cellDate == today

                      CalendarDayCell(
                        date = cellDate,
                        entries = dayEntries,
                        isToday = isToday,
                        onClick = { onDayClick(cellDate, dayEntries) },
                        modifier = Modifier.weight(1f),
                      )
                    } else {
                      Spacer(modifier = Modifier.weight(1f).aspectRatio(0.84f))
                    }
                  }
                }
              }
            }
          }
        }
      }

      // Permanent Home Screen Widget Suggestion Card below calendar
      item {
        WidgetPromoBannerCard(
          onPinWidget = triggerPinWidget,
          onOpenChooser = { manualChooserOpen = true },
        )
      }

      // Helpful hint below calendar
      item {
        val total = uiState.totalEntries
        Surface(
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          shape = MaterialTheme.shapes.medium,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text =
                if (total == 0) "Нажми на любой день или кнопку «+», чтобы начать"
                else "Нажми на день с лицом, чтобы открыть записи и заметки",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.weight(1f),
            )
            if (total > 0) {
              Spacer(modifier = Modifier.width(8.dp))
              Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(10.dp),
              ) {
                Text(
                  text = ToymoteDate.formatEntriesCountRu(total),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
              }
            }
          }
        }
      }
    }
  }

  if (showWidgetDialog) {
    WidgetSetupDialog(
      onPinWidget = triggerPinWidget,
      onDismiss = {
        if (showAutoOneTimeDialog) {
          onMarkWidgetPromptShown()
        }
        manualChooserOpen = false
      },
    )
  }
}

@Composable
private fun CalendarDayCell(
  date: ToymoteDate,
  entries: List<MoodEntryEntity>,
  isToday: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val latestEntry = entries.firstOrNull()
  val primaryEmotion = latestEntry?.let { EmotionCatalog.findById(it.emotionId) }
  val cellShape = RoundedCornerShape(14.dp)

  val containerColor =
    when {
      primaryEmotion != null -> MaterialTheme.colorScheme.surfaceContainerLow
      isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
      else -> MaterialTheme.colorScheme.surfaceContainerLowest
    }

  val borderColor =
    when {
      isToday -> MaterialTheme.colorScheme.primary
      primaryEmotion != null -> Color(primaryEmotion.accentColorHex).copy(alpha = 0.4f)
      else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    }

  val scale by
    animateFloatAsState(
      targetValue = if (primaryEmotion != null) 1f else 0.98f,
      animationSpec = spring(),
      label = "cell_scale",
    )

  Box(
    modifier =
      modifier
        .aspectRatio(0.84f)
        .scale(scale)
        .clip(cellShape)
        .background(containerColor)
        .border(
          width = if (isToday) 2.dp else 1.dp,
          color = borderColor,
          shape = cellShape,
        )
        .clickable(onClick = onClick)
        .padding(3.dp)
        .testTag("calendar_day_${date.epochDay}"),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = date.dayOfMonth.toString(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
        color =
          if (isToday) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.onSurfaceVariant,
      )

      if (primaryEmotion != null) {
        Box(contentAlignment = Alignment.TopEnd) {
          RobloxFaceBadge(
            emotion = primaryEmotion,
            size = 30.dp,
            showBorder = false,
          )
          if (entries.size > 1) {
            Box(
              modifier =
                Modifier.size(14.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primary),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = entries.size.toString(),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimary,
              )
            }
          }
        }
      } else {
        Box(
          modifier =
            Modifier.size(8.dp)
              .clip(CircleShape)
              .background(
                if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
              )
        )
      }

      Spacer(modifier = Modifier.height(1.dp))
    }
  }
}
