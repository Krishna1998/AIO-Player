/* SPDX-License-Identifier: AGPL-3.0-or-later */
package app.video.player.ui.browser.components

import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.video.player.ui.theme.AppMotion

/** Sorting and pinning move existing keyed items; scrolling never replays an entrance. */
@Composable
internal fun LazyItemScope.mediaItemMotion(): Modifier {
  val reduce = AppMotion.shouldReduceMotion()
  return Modifier.animateItem(
    fadeInSpec = if (reduce) null else spring(
      dampingRatio = AppMotion.Spatial.ExpressiveDefault.dampingRatio,
      stiffness = AppMotion.Spatial.ExpressiveDefault.stiffness,
    ),
    placementSpec = AppMotion.spatial(AppMotion.Spatial.StandardOffset, snap()),
    fadeOutSpec = if (reduce) null else spring(
      dampingRatio = AppMotion.ReducedAlpha.dampingRatio,
      stiffness = AppMotion.ReducedAlpha.stiffness,
    ),
  )
}

@Composable
internal fun LazyGridItemScope.mediaItemMotion(): Modifier {
  val reduce = AppMotion.shouldReduceMotion()
  return Modifier.animateItem(
    fadeInSpec = if (reduce) null else spring(
      dampingRatio = AppMotion.Spatial.ExpressiveDefault.dampingRatio,
      stiffness = AppMotion.Spatial.ExpressiveDefault.stiffness,
    ),
    placementSpec = AppMotion.spatial(AppMotion.Spatial.StandardOffset, snap()),
    fadeOutSpec = if (reduce) null else spring(
      dampingRatio = AppMotion.ReducedAlpha.dampingRatio,
      stiffness = AppMotion.ReducedAlpha.stiffness,
    ),
  )
}

