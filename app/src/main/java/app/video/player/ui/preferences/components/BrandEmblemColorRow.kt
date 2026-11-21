/* SPDX-License-Identifier: AGPL-3.0-or-later */
package app.video.player.ui.preferences.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.video.player.R
import app.video.player.ui.icons.Icon
import app.video.player.ui.icons.Icons
import app.video.player.ui.theme.BrandEmblemColor

/**
 * Brand Emblem Colour selector displaying glowing circular colour swatches with active selection indicators.
 */
@Composable
fun BrandEmblemColorRow(
  selectedColor: BrandEmblemColor,
  onColorSelected: (BrandEmblemColor) -> Unit,
  modifier: Modifier = Modifier,
) {
  val themePrimary = MaterialTheme.colorScheme.primary

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Text(
      text = stringResource(R.string.pref_emblem_color_title),
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.onSurface,
    )
    Text(
      text = stringResource(R.string.pref_emblem_color_summary),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.outline,
    )

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      BrandEmblemColor.entries.forEach { emblem ->
        val isSelected = selectedColor == emblem
        val displayColor = emblem.resolveColor(themePrimary)

        val borderColor by animateColorAsState(
          targetValue = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
          animationSpec = tween(200),
          label = "emblemBorder",
        )

        Surface(
          modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.RadioButton) { onColorSelected(emblem) },
          shape = RoundedCornerShape(16.dp),
          color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f) else Color.Transparent,
          border = BorderStroke(
            width = if (isSelected) 1.5.dp else 0.8.dp,
            color = if (isSelected) displayColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
          ),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Box(
              modifier = Modifier
                .size(24.dp)
                .shadow(if (isSelected) 6.dp else 0.dp, CircleShape, spotColor = displayColor)
                .clip(CircleShape)
                .background(
                  if (emblem == BrandEmblemColor.SyncTheme) {
                    Brush.sweepGradient(
                      listOf(
                        Color(0xFFD4FF00),
                        Color(0xFF00F0FF),
                        Color(0xFFFF1744),
                        Color(0xFFA855F7),
                        Color(0xFFD4FF00),
                      ),
                    )
                  } else {
                    Brush.radialGradient(listOf(displayColor, displayColor.copy(alpha = 0.85f)))
                  },
                ),
              contentAlignment = Alignment.Center,
            ) {
              if (isSelected) {
                Icon(
                  Icons.RoundedFilled.Check,
                  contentDescription = null,
                  tint = if (emblem == BrandEmblemColor.DiamondWhite) Color.Black else Color.White,
                  modifier = Modifier.size(14.dp),
                )
              }
            }

            Text(
              text = stringResource(emblem.labelRes),
              style = MaterialTheme.typography.labelMedium,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}

