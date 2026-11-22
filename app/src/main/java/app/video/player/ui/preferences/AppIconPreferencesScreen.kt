/* SPDX-License-Identifier: AGPL-3.0-or-later */
package app.video.player.ui.preferences

import app.video.player.ui.theme.wallpaperAwareBackgroundColor
import android.app.Activity
import android.widget.ImageView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.video.player.R
import app.video.player.presentation.Screen
import app.video.player.ui.components.AppToast as Toast
import app.video.player.ui.icons.Icon
import app.video.player.ui.icons.Icons
import app.video.player.ui.theme.AppConnectedShapeTokens
import app.video.player.ui.theme.AppMotion
import app.video.player.ui.theme.ConnectedLayout
import app.video.player.ui.theme.appPressFeedback
import app.video.player.ui.theme.connectedShape
import app.video.player.ui.utils.LocalBackStack
import app.video.player.ui.utils.popSafely
import kotlinx.serialization.Serializable

/**
 * App icon switcher: live preview and a curated grid of the 10 flagship and futuristic
 * icons designed on nocturnal Stealth Carbon. Nothing changes on the launcher until confirmed.
 */
@Serializable
object AppIconPreferencesScreen : Screen {
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backstack = LocalBackStack.current
    var current by remember {
      mutableStateOf(runCatching { LauncherIconManager.current(context) }.getOrDefault(LauncherIcon.Default))
    }
    var pending by remember { mutableStateOf(current) }
    var confirming by remember { mutableStateOf<LauncherIcon?>(null) }
    val canApply = pending != current
    val applyInteractionSource = remember { MutableInteractionSource() }
    val reduceMotion = AppMotion.shouldReduceMotion()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
      current = runCatching { LauncherIconManager.current(context) }.getOrDefault(current)
    }

    Scaffold(
      
      containerColor = wallpaperAwareBackgroundColor(),
      topBar = {
        SettingsTopAppBar(
          title = stringResource(R.string.pref_app_icon_title),
          onBack = { backstack.popSafely() },
        )
      },
    ) { padding ->
      Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
        Column(
          modifier = Modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(
              start = 16.dp,
              end = 16.dp,
              top = 12.dp,
              bottom = 24.dp,
            ),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
          IconPreviewCard {
            IconPreview(
              background = pending.background,
              mark = pending.mark,
              size = 132.dp,
            )

            AnimatedContent(
              targetState = iconName(pending),
              transitionSpec = {
                val transition = if (reduceMotion) {
                  fadeIn(snap()) togetherWith fadeOut(snap())
                } else {
                  fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                }
                transition.using(null)
              },
              label = "iconName",
            ) { name ->
              Text(
                text = name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                  .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f), RoundedCornerShape(20.dp))
                  .padding(horizontal = 16.dp, vertical = 8.dp),
              )
            }
            Text(
              text = stringResource(if (pending == current) R.string.app_icon_current else R.string.app_icon_pending),
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f), RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            )
          }

          Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionLabel(
              text = stringResource(R.string.app_icon_section_logo),
              supportingText = stringResource(R.string.app_icon_logo_hint),
            )

            val icons = LauncherIcon.entries
            val fontScale = LocalDensity.current.fontScale
            BoxWithConstraints(Modifier.fillMaxWidth()) {
              val columns = (maxWidth / (140.dp * fontScale)).toInt().coerceIn(2, 4)
              Column(
                modifier = Modifier.selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(AppConnectedShapeTokens.spacing),
              ) {
                icons.chunked(columns).forEachIndexed { row, rowIcons ->
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppConnectedShapeTokens.spacing),
                  ) {
                    rowIcons.forEachIndexed { column, icon ->
                      val shape = connectedShape(row * columns + column, icons.size, ConnectedLayout.Grid, columns)
                      MarkTile(
                        background = icon.background,
                        mark = icon.mark,
                        selected = pending == icon,
                        onClick = { pending = icon },
                        shape = shape,
                        modifier = Modifier.weight(1f),
                      )
                    }
                    repeat(columns - rowIcons.size) { Box(Modifier.weight(1f)) }
                  }
                }
              }
            }
          }

          Button(
            onClick = {
              if (canApply) confirming = pending
            },
            enabled = canApply,
            interactionSource = applyInteractionSource,
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = 56.dp)
              .appPressFeedback(applyInteractionSource, enabled = canApply),
          ) {
            Text(stringResource(R.string.app_icon_apply), style = MaterialTheme.typography.titleMedium)
          }
        }
      }
    }

    confirming?.let { target ->
      AlertDialog(
        onDismissRequest = { confirming = null },
        title = { Text(stringResource(R.string.app_icon_confirm_title)) },
        text = { Text(stringResource(R.string.app_icon_confirm_message, iconName(target))) },
        confirmButton = {
          TextButton(
            onClick = {
              confirming = null
              try {
                LauncherIconManager.select(context, target)
                (context as? Activity)?.let(LauncherIconManager::refreshTaskIcon)
                current = target
                pending = target
              } catch (_: Exception) {
                Toast.makeText(context, R.string.app_icon_change_failed, Toast.LENGTH_LONG).show()
                current = runCatching { LauncherIconManager.current(context) }.getOrDefault(current)
              }
            },
          ) { Text(stringResource(R.string.app_icon_confirm_action)) }
        },
        dismissButton = {
          TextButton(onClick = { confirming = null }) { Text(stringResource(android.R.string.cancel)) }
        },
      )
    }
  }
}

/** Returns the display name of an icon. */
@Composable
private fun iconName(icon: LauncherIcon): String = stringResource(icon.mark.label)

@Composable
private fun SectionLabel(text: String, supportingText: String) {
  Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(
      text = text,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.semantics { heading() },
    )
    Text(text = supportingText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

/** An icon choice tile. */
@Composable
private fun MarkTile(
  background: IconBackground,
  mark: IconMark,
  selected: Boolean,
  onClick: () -> Unit,
  shape: Shape,
  modifier: Modifier = Modifier,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val reduceMotion = AppMotion.shouldReduceMotion()
  val selectionTransition = AppMotion.iconChange(reduceMotion)
  val borderColor by animateColorAsState(
    targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
    animationSpec = if (reduceMotion) snap() else AppMotion.Effect.Color,
    label = "iconLogoSelectionBorder",
  )
  val containerColor by animateColorAsState(
    targetValue = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
    animationSpec = if (reduceMotion) snap() else AppMotion.Effect.Color,
    label = "iconLogoSelectionSurface",
  )
  val border = if (selected) {
    BorderStroke(2.dp, borderColor)
  } else {
    BorderStroke(1.dp, borderColor)
  }
  Column(
    modifier = modifier
      .appPressFeedback(interactionSource, enabled = true, pressedScale = 0.96f)
      .clip(shape)
      .background(containerColor)
      .border(border, shape)
      .selectable(
        selected = selected,
        enabled = true,
        role = Role.RadioButton,
        interactionSource = interactionSource,
        indication = androidx.compose.material3.ripple(),
        onClick = onClick,
      )
      .padding(horizontal = 8.dp, vertical = 12.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
      IconPreview(background, mark, 52.dp)
      androidx.compose.animation.AnimatedVisibility(
        visible = selected,
        modifier = Modifier.align(Alignment.TopEnd).size(18.dp),
        enter = selectionTransition.targetContentEnter,
        exit = selectionTransition.initialContentExit,
      ) {
        Icon(
          Icons.RoundedFilled.CheckCircle,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp),
        )
      }
    }
    Text(
      text = stringResource(mark.label),
      style = MaterialTheme.typography.bodySmall,
      fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
      color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
      textAlign = TextAlign.Center,
      minLines = 2,
    )
  }
}

/**
 * An icon drawn with the background fill and the adaptive foreground layer over it.
 */
@Composable
private fun IconPreview(
  background: IconBackground,
  mark: IconMark,
  size: Dp,
  modifier: Modifier = Modifier,
) {
  val reduceMotion = AppMotion.shouldReduceMotion()
  val fill = colorResource(background.color)
  Box(
    modifier = modifier
      .size(size)
      .clip(RoundedCornerShape(size * 0.3f))
      .background(fill),
    contentAlignment = Alignment.Center,
  ) {
    AnimatedContent(
      targetState = mark,
      transitionSpec = {
        val transition = if (reduceMotion) {
          fadeIn(snap()) togetherWith fadeOut(snap())
        } else {
          (scaleIn(initialScale = 0.6f, animationSpec = spring(dampingRatio = 0.55f, stiffness = 400f)) +
            fadeIn(tween(200))) togetherWith (scaleOut(targetScale = 1.2f) + fadeOut(tween(150)))
        }
        transition.using(null)
      },
      label = "iconMark",
    ) { shown ->
      MarkLayer(shown, size)
    }
  }
}

/**
 * One adaptive foreground layer rendered via ImageView.
 */
@Composable
private fun MarkLayer(mark: IconMark, size: Dp) {
  AndroidView(
    modifier = Modifier.requiredSize(size * 1.5f),
    factory = { context ->
      ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_XY
        setImageResource(mark.layer)
      }
    },
    update = { it.setImageResource(mark.layer) },
  )
}
