package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.repository.DailyForecastUi
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.FreshEmerald
import com.example.ui.theme.HarvestGold
import com.example.ui.theme.HarvestGoldContainer
import com.example.ui.theme.SkyBlue
import com.example.util.AppLanguage
import kotlin.math.roundToInt

@Composable
fun AiNoticeBanner(
  message: String,
  isLiveAi: Boolean = true,
  modifier: Modifier = Modifier,
) {
  val bg = if (isLiveAi) MaterialTheme.colorScheme.primaryContainer else HarvestGoldContainer
  val border = if (isLiveAi) FreshEmerald.copy(alpha = 0.45f) else HarvestGold.copy(alpha = 0.45f)
  val tint = if (isLiveAi) ForestGreen else HarvestGold
  val textColor = if (isLiveAi) MaterialTheme.colorScheme.onPrimaryContainer else Color(0xFF4A3000)

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .testTag("ai_firebase_notice_banner"),
    shape = RoundedCornerShape(14.dp),
    color = bg,
    tonalElevation = 1.dp,
  ) {
    Row(
      modifier = Modifier
        .border(1.dp, border, RoundedCornerShape(14.dp))
        .padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Icon(
        imageVector = Icons.Default.Info,
        contentDescription = "AI status notice",
        tint = tint,
        modifier = Modifier.size(20.dp),
      )
      Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = textColor,
        fontWeight = FontWeight.Medium,
      )
    }
  }
}

@Composable
fun StatusPill(
  text: String,
  containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
  contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(50),
    color = containerColor,
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall,
      color = contentColor,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    )
  }
}

@Composable
fun LanguageSelectorRow(
  selected: AppLanguage,
  onSelect: (AppLanguage) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState()),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Icon(
      imageVector = Icons.Default.Language,
      contentDescription = "Language selection",
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(18.dp),
    )
    AppLanguage.entries.forEach { lang ->
      FilterChip(
        selected = selected == lang,
        onClick = { onSelect(lang) },
        label = { Text(lang.nativeLabel) },
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .testTag("lang_chip_${lang.code}"),
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = MaterialTheme.colorScheme.primary,
          selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        ),
      )
    }
  }
}

@Composable
fun SectionHeader(
  title: String,
  subtitle: String? = null,
  actionText: String? = null,
  onActionClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
      )
      if (!subtitle.isNullOrBlank()) {
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    if (!actionText.isNullOrBlank() && onActionClick != null) {
      Text(
        text = actionText,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .clickable(onClick = onActionClick)
          .padding(horizontal = 8.dp, vertical = 6.dp),
      )
    }
  }
}

@Composable
fun SimpleDropdownSelector(
  label: String,
  selectedValue: String,
  options: List<String>,
  onSelect: (String) -> Unit,
  testTag: String,
  modifier: Modifier = Modifier,
) {
  var expanded by remember { mutableStateOf(false) }
  Box(modifier = modifier) {
    OutlinedButton(
      onClick = { expanded = true },
      modifier = Modifier
        .fillMaxWidth()
        .minimumInteractiveComponentSize()
        .testTag(testTag),
      shape = RoundedCornerShape(14.dp),
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = selectedValue,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Icon(
        imageVector = Icons.Default.ArrowDropDown,
        contentDescription = "Open $label menu",
      )
    }
    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
    ) {
      options.forEach { option ->
        DropdownMenuItem(
          text = { Text(option) },
          onClick = {
            expanded = false
            onSelect(option)
          },
        )
      }
    }
  }
}

@Composable
fun ForecastBarChart(
  forecast: List<DailyForecastUi>,
  modifier: Modifier = Modifier,
) {
  if (forecast.isEmpty()) return
  val maxTemp = (forecast.maxOfOrNull { it.tempMaxC } ?: 40.0).coerceAtLeast(10.0)
  val barPrimary = ForestGreen
  val barAccent = SkyBlue
  val trackColor = MaterialTheme.colorScheme.surfaceContainer

  Card(
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    shape = RoundedCornerShape(18.dp),
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "7-Day Max Temp & Rain Chance",
          style = MaterialTheme.typography.titleSmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
          LegendDot(color = barPrimary, label = "Temp °C")
          LegendDot(color = barAccent, label = "Rain %")
        }
      }
      Spacer(Modifier.height(12.dp))
      Canvas(
        modifier = Modifier
          .fillMaxWidth()
          .height(110.dp),
      ) {
        val count = forecast.size
        val slotWidth = size.width / count
        val barWidth = (slotWidth * 0.28f).coerceAtLeast(10f)
        forecast.forEachIndexed { idx, day ->
          val centerX = idx * slotWidth + slotWidth / 2f
          val tempRatio = (day.tempMaxC / maxTemp).toFloat().coerceIn(0.15f, 1f)
          val rainRatio = (day.rainProbPct / 100f).coerceIn(0.05f, 1f)

          // Track background
          drawRoundRect(
            color = trackColor,
            topLeft = Offset(centerX - barWidth - 3f, 0f),
            size = Size(barWidth, size.height),
            cornerRadius = CornerRadius(8f, 8f),
          )
          // Temp bar
          val tempH = size.height * tempRatio
          drawRoundRect(
            brush = Brush.verticalGradient(listOf(FreshEmerald, barPrimary)),
            topLeft = Offset(centerX - barWidth - 3f, size.height - tempH),
            size = Size(barWidth, tempH),
            cornerRadius = CornerRadius(8f, 8f),
          )
          // Rain probability bar
          val rainH = size.height * rainRatio
          drawRoundRect(
            color = barAccent.copy(alpha = 0.85f),
            topLeft = Offset(centerX + 3f, size.height - rainH),
            size = Size(barWidth, rainH),
            cornerRadius = CornerRadius(8f, 8f),
          )
        }
      }
      Spacer(Modifier.height(6.dp))
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        forecast.forEach { day ->
          Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Text(
              text = day.date.takeLast(5),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = "${day.tempMaxC.roundToInt()}°",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun LegendDot(color: Color, label: String) {
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
    Box(
      modifier = Modifier
        .size(8.dp)
        .clip(CircleShape)
        .background(color),
    )
    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@Composable
fun UsageAnalyticsBarChart(
  items: List<Pair<String, Int>>,
  modifier: Modifier = Modifier,
) {
  val maxVal = (items.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
  val barColors = listOf(ForestGreen, FreshEmerald, HarvestGold, SkyBlue)

  Card(
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    shape = RoundedCornerShape(18.dp),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Text("Module Usage Breakdown", style = MaterialTheme.typography.titleMedium)
      items.forEachIndexed { idx, (label, count) ->
        val ratio = count.toFloat() / maxVal.toFloat()
        val color = barColors[idx % barColors.size]
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            Text("$count", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = color)
          }
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(10.dp)
              .clip(RoundedCornerShape(50))
              .background(MaterialTheme.colorScheme.surfaceContainer),
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth(ratio.coerceAtLeast(0.03f))
                .height(10.dp)
                .clip(RoundedCornerShape(50))
                .background(color),
            )
          }
        }
      }
    }
  }
}
