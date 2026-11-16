package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.DailyMoodPoint
import com.example.domain.EmotionFrequency
import com.example.domain.ToymoteDate
import com.example.ui.StatsPeriod
import com.example.ui.ToymoteUiState
import com.example.ui.components.RobloxFaceBadge
import com.example.ui.theme.LocalToymoteColors
import kotlin.math.roundToInt

@Composable
fun StatsScreen(
  uiState: ToymoteUiState,
  onSelectPeriod: (StatsPeriod) -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 640.dp).fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 104.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text(
              text = "Статистика",
              style = MaterialTheme.typography.headlineMedium,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "Динамика эмоций и рекорды",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatsPeriod.entries.forEach { period ->
              FilterChip(
                selected = uiState.statsPeriod == period,
                onClick = { onSelectPeriod(period) },
                label = { Text(period.label) },
                modifier = Modifier.testTag("period_${period.name}"),
              )
            }
          }
        }
      }

      // Summary KPI Cards
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          StatKpiCard(
            title = "Всего записей",
            value = uiState.totalEntries.toString(),
            icon = Icons.Rounded.FormatListNumbered,
            accent = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
          )
          StatKpiCard(
            title = "Текущая серия",
            value = "${uiState.streakStats.currentStreak} дн.",
            icon = Icons.Rounded.LocalFireDepartment,
            accent = LocalToymoteColors.current.streakStart,
            modifier = Modifier.weight(1f),
          )
          StatKpiCard(
            title = "Лучшая серия",
            value = "${uiState.streakStats.longestStreak} дн.",
            icon = Icons.Rounded.EmojiEvents,
            accent = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f),
          )
        }
      }

      // Mood Trajectory Chart
      item {
        Card(
          colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = MaterialTheme.shapes.large,
          modifier =
            Modifier.fillMaxWidth()
              .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
                MaterialTheme.shapes.large,
              ),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.AutoGraph,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
              Text(
                text = "График настроения (${uiState.statsPeriod.label.lowercase()})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
            }

            MoodTrajectoryChart(
              points = uiState.chartPoints,
              modifier = Modifier.fillMaxWidth().height(190.dp),
            )
          }
        }
      }

      // Frequent Emotions - Active Period
      item {
        val activeList =
          if (uiState.statsPeriod == StatsPeriod.WEEK) uiState.weeklyTopEmotions
          else uiState.monthlyTopEmotions
        val otherList =
          if (uiState.statsPeriod == StatsPeriod.WEEK) uiState.monthlyTopEmotions
          else uiState.weeklyTopEmotions
        val otherTitle =
          if (uiState.statsPeriod == StatsPeriod.WEEK) "Самые частые за месяц"
          else "Самые частые за неделю"

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
          TopEmotionsCard(
            title = "Топ эмоций (${uiState.statsPeriod.label.lowercase()})",
            frequencies = activeList,
          )
          TopEmotionsCard(
            title = otherTitle,
            frequencies = otherList.take(3),
          )
        }
      }
    }
  }
}

@Composable
private fun StatKpiCard(
  title: String,
  value: String,
  icon: ImageVector,
  accent: Color,
  modifier: Modifier = Modifier,
) {
  Card(
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    shape = MaterialTheme.shapes.medium,
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        tint = accent,
      )
      Text(
        text = value,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
      )
      Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun MoodTrajectoryChart(
  points: List<DailyMoodPoint>,
  modifier: Modifier = Modifier,
) {
  val validPoints =
    points.mapIndexedNotNull { idx, point ->
      point.averageValence?.let { idx to it }
    }

  if (validPoints.isEmpty()) {
    Surface(
      color = MaterialTheme.colorScheme.surfaceContainerLow,
      shape = MaterialTheme.shapes.medium,
      modifier = modifier,
    ) {
      Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
          text = "Здесь появится плавная кривая настроения после первых отметок в календаре",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    return
  }

  val ext = LocalToymoteColors.current
  val gridColor = MaterialTheme.colorScheme.outlineVariant
  val surfaceColor = MaterialTheme.colorScheme.surface
  val firstLabel = points.firstOrNull()?.date?.shortDayMonthRu().orEmpty()
  val midLabel = points.getOrNull(points.size / 2)?.date?.shortDayMonthRu().orEmpty()
  val lastLabel = points.lastOrNull()?.date?.shortDayMonthRu().orEmpty()

  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
      Column(
        modifier = Modifier.fillMaxHeight().padding(end = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        Text("5", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("3", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("1", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }

      Canvas(modifier = Modifier.weight(1f).fillMaxHeight()) {
        val w = size.width
        val h = size.height
        val padY = 12f
        val usableH = (h - padY * 2).coerceAtLeast(1f)
        val count = (points.size - 1).coerceAtLeast(1)

        // Horizontal reference lines (levels 1, 3, 5)
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        listOf(0f, 0.5f, 1f).forEach { fraction ->
          val y = padY + usableH * fraction
          drawLine(
            color = gridColor.copy(alpha = 0.7f),
            start = Offset(0f, y),
            end = Offset(w, y),
            strokeWidth = 2f,
            pathEffect = dashEffect,
          )
        }

        val coords =
          validPoints.map { (idx, valence) ->
            val x = (idx.toFloat() / count) * w
            val normY = ((5f - valence) / 4f).coerceIn(0f, 1f)
            val y = padY + normY * usableH
            Offset(x, y)
          }

        if (coords.size >= 2) {
          val linePath = Path().apply { moveTo(coords.first().x, coords.first().y) }
          for (i in 0 until coords.lastIndex) {
            val p0 = coords[i]
            val p1 = coords[i + 1]
            val midX = (p0.x + p1.x) / 2f
            linePath.cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
          }

          val fillPath =
            Path().apply {
              addPath(linePath)
              lineTo(coords.last().x, h)
              lineTo(coords.first().x, h)
              close()
            }

          drawPath(
            path = fillPath,
            brush =
              Brush.verticalGradient(
                colors = listOf(ext.chartFillTop, ext.chartFillBottom)
              ),
          )
          drawPath(
            path = linePath,
            color = ext.chartLine,
            style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round),
          )
        }

        coords.forEach { pt ->
          drawCircle(color = ext.chartLine, radius = 9f, center = pt)
          drawCircle(color = surfaceColor, radius = 4.5f, center = pt)
        }
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth().padding(start = 18.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(firstLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Text(midLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Text(lastLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}

@Composable
private fun TopEmotionsCard(
  title: String,
  frequencies: List<EmotionFrequency>,
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = MaterialTheme.shapes.large,
    modifier =
      Modifier.fillMaxWidth()
        .border(
          1.dp,
          MaterialTheme.colorScheme.outlineVariant,
          MaterialTheme.shapes.large,
        ),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )

      if (frequencies.isEmpty()) {
        Text(
          text = "За этот период ещё нет записей.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        frequencies.take(5).forEach { freq ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            RobloxFaceBadge(emotion = freq.emotion, size = 42.dp)
            Column(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = freq.emotion.name,
                  style = MaterialTheme.typography.titleSmall,
                )
                Text(
                  text = "${freq.count} (${(freq.share * 100f).roundToInt()}%)",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
              Box(
                modifier =
                  Modifier.fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
              ) {
                Box(
                  modifier =
                    Modifier.fillMaxWidth(freq.share.coerceIn(0.06f, 1f))
                      .fillMaxHeight()
                      .clip(RoundedCornerShape(4.dp))
                      .background(
                        Brush.horizontalGradient(
                          colors =
                            listOf(
                              MaterialTheme.colorScheme.primary,
                              Color(freq.emotion.accentColorHex),
                            )
                        )
                      )
                )
              }
            }
          }
        }
      }
    }
  }
}
