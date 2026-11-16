package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.MoodEntryEntity
import com.example.domain.EmotionCatalog
import com.example.domain.ToymoteDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailSheet(
  date: ToymoteDate,
  entries: List<MoodEntryEntity>,
  onDismiss: () -> Unit,
  onAddAnother: (ToymoteDate) -> Unit,
  onEditEntry: (MoodEntryEntity) -> Unit,
  onDeleteEntry: (Long) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .navigationBarsPadding()
          .padding(horizontal = 20.dp)
          .padding(bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = date.fullDateRu(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = ToymoteDate.formatEntriesCountRu(entries.size),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Button(
          onClick = { onAddAnother(date) },
          modifier = Modifier.testTag("day_detail_add_button"),
        ) {
          Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
          Spacer(modifier = Modifier.width(6.dp))
          Text(text = "Добавить")
        }
      }

      if (entries.isEmpty()) {
        Surface(
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          shape = MaterialTheme.shapes.medium,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = "В этот день ещё нет записей. Нажми «Добавить», чтобы отметить настроение!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(20.dp),
          )
        }
      } else {
        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          items(entries.sortedByDescending { it.timestampMillis }, key = { it.id }) { entry ->
            val emotion = EmotionCatalog.findById(entry.emotionId)
            Card(
              colors =
                CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
              shape = MaterialTheme.shapes.medium,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
              ) {
                RobloxFaceBadge(emotion = emotion, size = 56.dp)
                Column(modifier = Modifier.weight(1f)) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Text(
                      text = emotion.name,
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                    )
                    Surface(
                      color = MaterialTheme.colorScheme.primaryContainer,
                      shape = MaterialTheme.shapes.extraSmall,
                    ) {
                      Text(
                        text = ToymoteDate.formatTimeRu(entry.timestampMillis),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                      )
                    }
                  }
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text =
                      entry.comment.ifBlank { emotion.subtitle },
                    style = MaterialTheme.typography.bodyMedium,
                    color =
                      if (entry.comment.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                      else MaterialTheme.colorScheme.onSurface,
                  )
                }
                IconButton(
                  onClick = { onEditEntry(entry) },
                  modifier = Modifier.testTag("edit_entry_${entry.id}"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Редактировать запись",
                    tint = MaterialTheme.colorScheme.primary,
                  )
                }
                IconButton(
                  onClick = { onDeleteEntry(entry.id) },
                  modifier = Modifier.testTag("delete_entry_${entry.id}"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Удалить запись",
                    tint = MaterialTheme.colorScheme.error,
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
