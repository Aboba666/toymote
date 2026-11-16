package com.example.ui.screens

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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.EmotionCatalog
import com.example.domain.EmotionCategory
import com.example.domain.EmotionDef
import com.example.domain.ToymoteDate
import com.example.ui.components.RobloxFaceBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmotionPickerScreen(
  targetDate: ToymoteDate,
  initialEmotionId: String?,
  initialComment: String,
  isEditing: Boolean,
  onNavigateBack: () -> Unit,
  onSave: (emotionId: String, comment: String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var selectedCategory by rememberSaveable { mutableStateOf(EmotionCategory.ALL) }
  var selectedEmotionId by rememberSaveable {
    mutableStateOf(initialEmotionId ?: EmotionCatalog.all.first().id)
  }
  var comment by rememberSaveable { mutableStateOf(initialComment) }

  val filteredEmotions =
    remember(selectedCategory) { EmotionCatalog.filterBy(selectedCategory) }
  val selectedEmotion =
    remember(selectedEmotionId) { EmotionCatalog.findById(selectedEmotionId) }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    topBar = {
      TopAppBar(
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
          ),
        navigationIcon = {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("picker_back_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = "Назад",
            )
          }
        },
        title = {
          Column {
            Text(
              text = if (isEditing) "Редактировать эмоцию" else "Как ты себя чувствуешь?",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = targetDate.fullDateRu(),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        },
      )
    },
    bottomBar = {
      Surface(
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier =
          Modifier.fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
      ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
          Column(
            modifier =
              Modifier.widthIn(max = 640.dp)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              RobloxFaceBadge(emotion = selectedEmotion, size = 44.dp)
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = selectedEmotion.name,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = selectedEmotion.subtitle,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }

            OutlinedTextField(
              value = comment,
              onValueChange = { if (it.length <= 140) comment = it },
              placeholder = { Text("Короткий комментарий (необязательно)") },
              singleLine = true,
              shape = MaterialTheme.shapes.medium,
              modifier = Modifier.fillMaxWidth().testTag("comment_input"),
            )

            Button(
              onClick = { onSave(selectedEmotionId, comment) },
              shape = RoundedCornerShape(18.dp),
              modifier =
                Modifier.fillMaxWidth()
                  .height(54.dp)
                  .testTag("save_emotion_button"),
            ) {
              Icon(imageVector = Icons.Rounded.Check, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Сохранить",
                style = MaterialTheme.typography.titleMedium,
              )
            }
          }
        }
      }
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier = Modifier.widthIn(max = 640.dp).fillMaxSize(),
      ) {
        LazyRow(
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(EmotionCategory.entries) { category ->
            FilterChip(
              selected = selectedCategory == category,
              onClick = { selectedCategory = category },
              label = { Text(text = category.label) },
              modifier = Modifier.testTag("category_chip_${category.name}"),
            )
          }
        }

        LazyVerticalGrid(
          columns = GridCells.Adaptive(minSize = 104.dp),
          contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 20.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxSize(),
        ) {
          items(filteredEmotions, key = { it.id }) { emotion ->
            EmotionGridCard(
              emotion = emotion,
              isSelected = emotion.id == selectedEmotionId,
              onClick = { selectedEmotionId = emotion.id },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun EmotionGridCard(
  emotion: EmotionDef,
  isSelected: Boolean,
  onClick: () -> Unit,
) {
  val scale by
    animateFloatAsState(
      targetValue = if (isSelected) 1.03f else 1f,
      animationSpec = spring(),
      label = "emotion_select_scale",
    )
  val cardShape = RoundedCornerShape(20.dp)
  val bg =
    if (isSelected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surface
  val borderColor =
    if (isSelected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outlineVariant

  Column(
    modifier =
      Modifier.scale(scale)
        .clip(cardShape)
        .background(bg)
        .border(
          width = if (isSelected) 2.5.dp else 1.dp,
          color = borderColor,
          shape = cardShape,
        )
        .clickable(onClick = onClick)
        .padding(vertical = 12.dp, horizontal = 8.dp)
        .testTag("emotion_card_${emotion.id}"),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    RobloxFaceBadge(emotion = emotion, size = 64.dp)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = emotion.name,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
        color =
          if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
          else MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
      )
      Box(
        modifier =
          Modifier.padding(top = 3.dp)
            .size(width = 20.dp, height = 3.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color(emotion.accentColorHex).copy(alpha = 0.7f))
      )
    }
  }
}
