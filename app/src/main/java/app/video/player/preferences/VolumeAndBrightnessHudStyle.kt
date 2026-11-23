/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package app.video.player.preferences

import androidx.annotation.StringRes
import app.video.player.R

enum class VolumeAndBrightnessHudStyle(@StringRes val titleRes: Int) {
  DynamicIsland(R.string.hud_style_dynamic_island),
  HyperNotchMatrix(R.string.hud_style_hyper_notch_matrix),
  AeroDualTone(R.string.hud_style_aero_dual_tone),
  SegmentedBlocks(R.string.hud_style_segmented_blocks),
  SoundStageBento(R.string.hud_style_soundstage_bento),
  ClassicCapsule(R.string.hud_style_classic_capsule);

  val isTopHorizontal: Boolean
    get() =
      this == DynamicIsland ||
        this == HyperNotchMatrix ||
        this == AeroDualTone ||
        this == SoundStageBento
}
