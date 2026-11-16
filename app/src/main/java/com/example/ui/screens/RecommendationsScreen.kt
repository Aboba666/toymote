package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.AdviceCard
import com.example.ui.ToymoteUiState
import com.example.ui.components.MattoCompanyLogo
import com.example.ui.components.RobloxFaceBadge

@Composable
fun RecommendationsScreen(
  uiState: ToymoteUiState,
  onNextQuote: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 640.dp).fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 104.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item {
        Column {
          Text(
            text = "Рекомендации",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "Персональные советы на основе последних эмоций",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      // Motivational Quote Hero Card
      item {
        val quoteGradient =
          Brush.linearGradient(
            colors =
              listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.tertiary,
              )
          )

        Card(
          shape = MaterialTheme.shapes.large,
          elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .background(quoteGradient)
                .padding(20.dp)
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.FormatQuote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                  )
                  Text(
                    text = "ФРАЗА ДНЯ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                  )
                }

                IconButton(
                  onClick = onNextQuote,
                  modifier = Modifier.testTag("refresh_quote_button"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Следующая мотивирующая фраза",
                    tint = MaterialTheme.colorScheme.onPrimary,
                  )
                }
              }

              AnimatedContent(
                targetState = uiState.currentQuote,
                label = "quote_transition",
              ) { quote ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  Text(
                    text = "«${quote.text}»",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary,
                  )
                  Text(
                    text = quote.subtext,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                  )
                }
              }
            }
          }
        }
      }

      // Personalized Advice Section Title
      item {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = "Для тебя сейчас",
            style = MaterialTheme.typography.titleLarge,
          )
        }
      }

      items(uiState.adviceCards, key = { it.id }) { card ->
        AdviceCardItem(card = card)
      }

      // Offline Privacy & MATTO Company Logo Footer
      item {
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          shape = MaterialTheme.shapes.medium,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Lock,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              MattoCompanyLogo()
              Text(
                text = stringResource(id = R.string.brand_signature) +
                  " • данные дневника хранятся локально",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AdviceCardItem(card: AdviceCard) {
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
    Row(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalAlignment = Alignment.Top,
    ) {
      if (card.emotionHint != null) {
        RobloxFaceBadge(emotion = card.emotionHint, size = 50.dp)
      }
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Surface(
          color =
            card.emotionHint?.let { Color(it.accentColorHex).copy(alpha = 0.14f) }
              ?: MaterialTheme.colorScheme.primaryContainer,
          shape = RoundedCornerShape(8.dp),
        ) {
          Text(
            text = card.tag,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
          )
        }
        Text(
          text = card.title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = card.body,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
