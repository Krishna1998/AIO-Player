package app.video.player.ui.mediainfo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MediaFormatBadgesRow(
  badges: List<MediaBadge>,
  colorize: Boolean = true,
  modifier: Modifier = Modifier,
) {
  FlowRow(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    badges.forEach { badge ->
      FormatBadgeChip(badge = badge, colorize = colorize)
    }
  }
}

@Composable
fun FormatBadgeChip(
  badge: MediaBadge,
  colorize: Boolean,
) {
  val (containerColor, contentColor, borderColor) = if (colorize) {
    when (badge.category) {
      BadgeCategory.HDR -> Triple(
        Color(0xFF4A148C).copy(alpha = 0.22f),
        Color(0xFFE1BEE7),
        Color(0xFFAB47BC).copy(alpha = 0.5f),
      )
      BadgeCategory.AUDIO -> Triple(
        Color(0xFF0D47A1).copy(alpha = 0.22f),
        Color(0xFFBBDEFB),
        Color(0xFF42A5F5).copy(alpha = 0.5f),
      )
      BadgeCategory.RESOLUTION -> Triple(
        Color(0xFF1B5E20).copy(alpha = 0.22f),
        Color(0xFFC8E6C9),
        Color(0xFF66BB6A).copy(alpha = 0.5f),
      )
      BadgeCategory.SUBTITLE -> Triple(
        Color(0xFFE65100).copy(alpha = 0.20f),
        Color(0xFFFFE0B2),
        Color(0xFFFFA726).copy(alpha = 0.45f),
      )
      else -> Triple(
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.onSecondaryContainer,
        MaterialTheme.colorScheme.outlineVariant,
      )
    }
  } else {
    Triple(
      MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
      MaterialTheme.colorScheme.onSurfaceVariant,
      MaterialTheme.colorScheme.outlineVariant,
    )
  }

  Surface(
    shape = RoundedCornerShape(6.dp),
    color = containerColor,
    border = BorderStroke(1.dp, borderColor),
  ) {
    Text(
      text = badge.label.uppercase(),
      color = contentColor,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = 0.5.sp,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
    )
  }
}

