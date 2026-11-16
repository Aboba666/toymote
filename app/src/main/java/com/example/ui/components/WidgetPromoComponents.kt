package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.widgets.ToymoteWidgetKind

private data class WidgetOptionMeta(
  val kind: ToymoteWidgetKind,
  val title: String,
  val description: String,
  val icon: ImageVector,
  val chipLabel: String,
)

private val WIDGET_OPTIONS =
  listOf(
    WidgetOptionMeta(
      kind = ToymoteWidgetKind.STREAK,
      title = "Ударная серия",
      description = "Счётчик дней подряд с огоньком и эмоцией дня",
      icon = Icons.Rounded.LocalFireDepartment,
      chipLabel = "Серия",
    ),
    WidgetOptionMeta(
      kind = ToymoteWidgetKind.CALENDAR,
      title = "Календарь эмоций",
      description = "Лента последних дней или весь месяц с лицами",
      icon = Icons.Rounded.CalendarMonth,
      chipLabel = "Календарь",
    ),
    WidgetOptionMeta(
      kind = ToymoteWidgetKind.COMBO,
      title = "Комбо: Серия + Календарь",
      description = "Всё сразу в одном адаптивном виджете",
      icon = Icons.Rounded.DashboardCustomize,
      chipLabel = "Комбо",
    ),
  )

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WidgetPromoBannerCard(
  onPinWidget: (ToymoteWidgetKind) -> Unit,
  onOpenChooser: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val gradient =
    Brush.horizontalGradient(
      colors =
        listOf(
          MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
          MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
        )
    )

  Card(
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    modifier =
      modifier
        .fillMaxWidth()
        .border(
          1.dp,
          MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
          MaterialTheme.shapes.large,
        )
        .testTag("widget_promo_banner"),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().background(gradient).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Surface(
          color = MaterialTheme.colorScheme.primary,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.size(44.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Rounded.Widgets,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(24.dp),
            )
          }
        }

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Виджет на главный экран",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = "Следи за серией и эмоциями прямо с рабочего стола",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        WIDGET_OPTIONS.forEach { option ->
          FilledTonalButton(
            onClick = { onPinWidget(option.kind) },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.height(48.dp).testTag("promo_pin_${option.kind.name.lowercase()}"),
          ) {
            Icon(
              imageVector = option.icon,
              contentDescription = option.title,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = option.chipLabel, style = MaterialTheme.typography.labelLarge)
          }
        }

        TextButton(
          onClick = onOpenChooser,
          modifier = Modifier.height(48.dp).testTag("promo_open_widget_dialog"),
        ) {
          Icon(
            imageVector = Icons.Rounded.AddToHomeScreen,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(text = "Подробнее")
        }
      }
    }
  }
}

@Composable
fun WidgetSetupDialog(
  onPinWidget: (ToymoteWidgetKind) -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.size(52.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Rounded.AddToHomeScreen,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp),
          )
        }
      }
    },
    title = {
      Text(
        text = "Поставь виджет toymote",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text =
            "Выбери виджет для главного экрана — он подстраивается под любой размер от 2×1 до большого календаря:",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        WIDGET_OPTIONS.forEach { option ->
          Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier =
              Modifier.fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable { onPinWidget(option.kind) }
                .testTag("dialog_pin_${option.kind.name.lowercase()}"),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(40.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = option.icon,
                    contentDescription = option.title,
                    tint = MaterialTheme.colorScheme.primary,
                  )
                }
              }
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = option.title,
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = option.description,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onPinWidget(ToymoteWidgetKind.COMBO) },
        modifier = Modifier.testTag("widget_dialog_pin_default"),
      ) {
        Text("Поставить Комбо")
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("widget_dialog_dismiss"),
      ) {
        Text("Позже")
      }
    },
  )
}
