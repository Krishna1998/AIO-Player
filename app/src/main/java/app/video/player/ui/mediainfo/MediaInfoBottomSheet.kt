package app.video.player.ui.mediainfo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.video.player.ui.icons.AppIcon
import app.video.player.ui.icons.Icon
import app.video.player.ui.icons.Icons
import app.video.player.utils.clipboard.SafeClipboard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaInfoBottomSheet(
  state: MediaInfoUiState,
  onDismiss: () -> Unit,
  colorizeBadges: Boolean = true,
) {
  val sheetState = rememberModalBottomSheetState(
    skipPartiallyExpanded = false,
  )
  val context = LocalContext.current

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    dragHandle = { BottomSheetDefaults.DragHandle() },
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding(),
      contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = state.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
          if (state.badges.isNotEmpty()) {
            MediaFormatBadgesRow(
              badges = state.badges,
              colorize = colorizeBadges,
            )
          }
        }
      }

      item {
        InfoSectionCard(title = "File & Stream", icon = Icons.RoundedFilled.InsertDriveFile) {
          InfoRow(label = "File Name", value = state.fileName)
          InfoRow(
            label = "Location",
            value = state.filePath,
            onCopy = { SafeClipboard.copyPlainText(context, "Location", state.filePath) },
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Box(Modifier.weight(1f)) {
              InfoRow(label = "Size", value = state.fileSize)
            }
            Box(Modifier.weight(1f)) {
              InfoRow(label = "Duration", value = state.duration)
            }
            Box(Modifier.weight(1f)) {
              InfoRow(label = "Bitrate", value = state.overallBitrate)
            }
          }
        }
      }

      if (state.videoTracks.isNotEmpty()) {
        item {
          InfoSectionCard(title = "Video", icon = Icons.RoundedFilled.Movie) {
            state.videoTracks.forEach { track ->
              TrackItemRow(track = track)
            }
          }
        }
      }

      if (state.audioTracks.isNotEmpty()) {
        item {
          InfoSectionCard(title = "Audio", icon = Icons.RoundedFilled.Audiotrack) {
            state.audioTracks.forEach { track ->
              TrackItemRow(track = track)
            }
          }
        }
      }

      if (state.subtitleTracks.isNotEmpty()) {
        item {
          InfoSectionCard(title = "Subtitles", icon = Icons.RoundedFilled.Subtitles) {
            state.subtitleTracks.forEach { track ->
              TrackItemRow(track = track)
            }
          }
        }
      }

      if (state.embeddedMetadata.isNotEmpty()) {
        item {
          InfoSectionCard(title = "Metadata", icon = Icons.RoundedFilled.Info) {
            state.embeddedMetadata.forEach { (key, value) ->
              InfoRow(label = key, value = value)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun InfoSectionCard(
  title: String,
  icon: AppIcon,
  content: @Composable ColumnScope.() -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp),
        )
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.primary,
        )
      }
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
      content()
    }
  }
}

@Composable
private fun InfoRow(
  label: String,
  value: String,
  onCopy: (() -> Unit)? = null,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = value,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
    if (onCopy != null) {
      IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
        Icon(
          imageVector = Icons.RoundedFilled.ContentCopy,
          contentDescription = "Copy",
          modifier = Modifier.size(16.dp),
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun TrackItemRow(track: TrackDetail) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .background(
        if (track.isSelected) {
          MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        } else {
          MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
        },
      )
      .padding(10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(
          text = "#${track.id} \u2022 ${track.title.ifBlank { track.language }}",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Medium,
        )
        if (track.isDefault) {
          Text(
            text = "DEFAULT",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
          )
        }
      }
      Text(
        text = track.codecInfo,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    if (track.isSelected) {
      Icon(
        imageVector = Icons.RoundedFilled.CheckCircle,
        contentDescription = "Active Track",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(18.dp),
      )
    }
  }
}

