/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package app.video.player.ui.player.visualizer

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private data class RibbonTones(
  val bass: Color,
  val mid: Color,
)

private fun ribbonTones(palette: VisualizerPalette): RibbonTones =
  RibbonTones(
    bass = Color(palette.secondary),
    mid = Color(palette.tertiary),
  )

/** Loud audio can accelerate the ribbon motion by at most thirty percent. */
internal fun ribbonSpeedMultiplier(loudness: Float): Float =
  1f + loudness.coerceIn(0f, 1f) * 0.30f

/** Keeps the default lift, then adds explicit system-volume and per-band audio response. */
internal fun ribbonLiftScale(
  outputVolume: Float,
  loudness: Float,
  bandLevel: Float,
): Float {
  val audioLevel =
    (loudness.coerceIn(0f, 1f) * 0.52f + bandLevel.coerceIn(0f, 1f) * 0.48f)
      .coerceIn(0f, 1f)
  return (
    1f + outputVolume.coerceIn(0f, 1f) * (0.08f + audioLevel * 0.47f)
  ).coerceIn(1f, 1.55f)
}

/**
 * One UI 6 media-notification style visualizer: a solid ribbon grows above the elapsed
 * track, breathes with the music, and tapers back into the seekbar at the playhead.
 */
@Composable
internal fun WaveVisualizerOverlay(
  palette: VisualizerPalette,
  isSheetOpen: Boolean,
  features: AudioFeatures,
  isPlaying: Boolean,
  progressProvider: () -> Float,
  trackHeight: Dp,
  trackColor: Color,
  modifier: Modifier = Modifier,
) {
  // Each ribbon owns a frequency-band envelope; system volume remains a separate live gain.
  var frameNanos by remember { mutableLongStateOf(0L) }
  var lowPhase by remember { mutableFloatStateOf(0f) }
  var midPhase by remember { mutableFloatStateOf(1.8f) }
  var highPhase by remember { mutableFloatStateOf(3.6f) }
  var lowLevel by remember { mutableFloatStateOf(0f) }
  var midLevel by remember { mutableFloatStateOf(0f) }
  var highLevel by remember { mutableFloatStateOf(0f) }
  var loudness by remember { mutableFloatStateOf(0f) }
  var outputVolume by remember { mutableFloatStateOf(features.volumeScale.coerceIn(0f, 1f)) }
  var beatPulse by remember { mutableFloatStateOf(0f) }
  LaunchedEffect(isPlaying, isSheetOpen) {
    if (!isPlaying || isSheetOpen) {
      beatPulse = 0f
      return@LaunchedEffect
    }
    var previous = 0L
    var beatWasActive = false
    fun responsiveLevel(value: Float): Float = sqrt(value.coerceIn(0f, 1f))

    while (true) {
      withFrameNanos { now ->
        val dt = if (previous == 0L) 0f else (now - previous) / 1_000_000_000f
        previous = now
        val lowTarget =
          responsiveLevel(
            features.subBass * 0.45f +
              features.bass * 0.35f +
              features.beat * 0.12f +
              features.energy * 0.08f,
          )
        val midTarget =
          responsiveLevel(
            features.lowMid * 0.25f +
              features.mid * 0.45f +
              features.energy * 0.20f +
              features.spectralFlux.coerceIn(0f, 1f) * 0.10f,
          )
        val highTarget =
          responsiveLevel(
            features.highMid * 0.25f +
              features.treble * 0.45f +
              features.spectralFlux.coerceIn(0f, 1f) * 0.22f +
              features.beat * 0.08f,
          )
        val loudnessTarget =
          responsiveLevel(
            features.energy * 0.50f +
              features.subBass * 0.10f +
              features.bass * 0.15f +
              features.mid * 0.15f +
              features.treble * 0.10f,
          )

        lowLevel += (lowTarget - lowLevel) * min(1f, dt * (if (lowTarget > lowLevel) 10f else 3f))
        midLevel += (midTarget - midLevel) * min(1f, dt * (if (midTarget > midLevel) 12f else 4f))
        highLevel += (highTarget - highLevel) * min(1f, dt * (if (highTarget > highLevel) 15f else 6f))
        loudness +=
          (loudnessTarget - loudness) *
            min(1f, dt * (if (loudnessTarget > loudness) 10f else 3.5f))
        val volumeTarget = features.volumeScale.coerceIn(0f, 1f)
        outputVolume +=
          (volumeTarget - outputVolume) * min(1f, dt * 8f)
        beatPulse *= exp(-6f * dt)
        val beatActive = features.beat >= 0.5f
        if (beatActive && !beatWasActive) {
          beatPulse = 1f
          lowPhase += 0.10f * outputVolume
          midPhase += 0.16f * outputVolume
          highPhase += 0.22f * outputVolume
        }
        beatWasActive = beatActive
        lowPhase +=
          dt * 1.15f * ribbonSpeedMultiplier(outputVolume * (loudness * 0.55f + lowLevel * 0.45f))
        midPhase +=
          dt * 2f * ribbonSpeedMultiplier(outputVolume * (loudness * 0.55f + midLevel * 0.45f))
        highPhase +=
          dt * 3.4f * ribbonSpeedMultiplier(outputVolume * (loudness * 0.55f + highLevel * 0.45f))
        frameNanos = now
      }
    }
  }

  // Wave collapses to a flat line while paused, exactly like the One UI notification.
  val amplitudeFraction by animateFloatAsState(
    targetValue = if (isPlaying) 1f else 0f,
    animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow),
    label = "wave_amplitude",
  )

  val lowFilledPath = remember { Path() }
  val lowContourPath = remember { Path() }
  val midFilledPath = remember { Path() }
  val midContourPath = remember { Path() }
  val highFilledPath = remember { Path() }
  val highContourPath = remember { Path() }
  val tones = remember(palette) { ribbonTones(palette) }

  Canvas(modifier = modifier) {
    @Suppress("UNUSED_EXPRESSION")
    frameNanos // Reading the frame clock keeps the canvas invalidating per frame.

    val width = size.width
    val centerY = size.height / 2f
    if (width <= 0f) return@Canvas

    val trackHalf = trackHeight.toPx() / 2f
    val trackTop = centerY - trackHalf
    val progress = progressProvider().coerceIn(0f, 1f)
    val waveEnd = (width * progress).coerceIn(0f, width)
    val twoPi = (2.0 * PI).toFloat()

    fun smoothstep(value: Float): Float {
      val clamped = value.coerceIn(0f, 1f)
      return clamped * clamped * (3f - 2f * clamped)
    }

    fun drawRibbon(
      filledPath: Path,
      contourPath: Path,
      color: Color,
      wavelength: Float,
      baseLift: Float,
      phase: Float,
      liftScale: Float,
      beatLift: Float,
      contourWidth: Float,
      glowWidth: Float,
      glowAlpha: Float,
      bodyTopAlpha: Float,
      bodyBottomAlpha: Float,
      hasWhiteHighlight: Boolean = false,
    ) {
      val availableLift = (trackTop - 1.dp.toPx()).coerceAtLeast(0f)
      val lift = min((baseLift * liftScale + beatLift) * amplitudeFraction, availableLift)
      val joinInset = trackHalf.coerceAtLeast(1.dp.toPx())
      val ribbonStart = joinInset.coerceAtMost(waveEnd / 2f)
      val ribbonEnd = (waveEnd - joinInset).coerceAtLeast(ribbonStart)
      if (ribbonEnd - ribbonStart <= 1f || lift <= 0.25f) return

      fun ribbonTop(x: Float): Float {
        val edgeIn = smoothstep((x - ribbonStart) / (wavelength * 0.5f))
        val edgeOut = smoothstep((ribbonEnd - x) / (wavelength * 0.55f))
        val envelope = edgeIn * edgeOut
        val undulation = 0.60f + 0.40f * sin((x / wavelength) * twoPi - phase)
        return trackTop - lift * undulation * envelope
      }

      val step = 1.5.dp.toPx().coerceAtLeast(1f)
      val baseline = trackTop + min(trackHalf, 1.5.dp.toPx())
      filledPath.reset()
      contourPath.reset()

      val startY = ribbonTop(ribbonStart)
      filledPath.moveTo(ribbonStart, baseline)
      filledPath.lineTo(ribbonStart, startY)
      contourPath.moveTo(ribbonStart, startY)

      var x = ribbonStart
      while (x < ribbonEnd) {
        x = min(x + step, ribbonEnd)
        val y = ribbonTop(x)
        filledPath.lineTo(x, y)
        contourPath.lineTo(x, y)
      }
      filledPath.lineTo(ribbonEnd, baseline)
      filledPath.close()

      // 1. One UI 9.5 Frosted Glass Gradient Body Fill (curtain down to baseline)
      val topY = trackTop - lift
      drawPath(
        path = filledPath,
        brush = Brush.verticalGradient(
          colors = listOf(
            color.copy(alpha = (bodyTopAlpha * amplitudeFraction).coerceIn(0f, 1f)),
            color.copy(alpha = (bodyBottomAlpha * amplitudeFraction).coerceIn(0f, 1f)),
          ),
          startY = topY,
          endY = baseline,
        ),
      )

      // 2. Diffused Luminous Gaussian Glow Halo along wave crests
      if (glowWidth > 0f && lift > 1.dp.toPx()) {
        drawPath(
          path = contourPath,
          color = color.copy(alpha = (glowAlpha * amplitudeFraction).coerceIn(0f, 1f)),
          style = Stroke(
            width = glowWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
          ),
        )
      }

      // 3. Crisp Luminous Crest Contour Stroke
      drawPath(
        path = contourPath,
        color = color.copy(alpha = (0.95f * amplitudeFraction).coerceIn(0f, 1f)),
        style = Stroke(
          width = contourWidth,
          cap = StrokeCap.Round,
          join = StrokeJoin.Round,
        ),
      )

      // 4. Foreground White Specular Highlight Core for crisp liquid glass reflection
      if (hasWhiteHighlight && lift > 2.dp.toPx()) {
        drawPath(
          path = contourPath,
          color = Color.White.copy(alpha = (0.65f * amplitudeFraction).coerceIn(0f, 1f)),
          style = Stroke(
            width = 1.0.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
          ),
        )
      }
    }

    val rearLiftScale = ribbonLiftScale(outputVolume, loudness, lowLevel)
    val middleLiftScale =
      min(
        ribbonLiftScale(outputVolume, loudness, midLevel),
        rearLiftScale * 1.20f,
      )
    val frontLiftScale =
      min(
        ribbonLiftScale(outputVolume, loudness, highLevel),
        middleLiftScale * 1.18f,
      )
    val beatImpact = beatPulse * outputVolume

    // One UI 9.5 Aurora Frosted Glass Veil: 3-tier multi-depth wave curtains
    // Layer 1: Ambient Deep Bass Veil
    drawRibbon(
      filledPath = lowFilledPath,
      contourPath = lowContourPath,
      color = tones.bass,
      wavelength = 112.dp.toPx(),
      baseLift = 17.5.dp.toPx(),
      phase = lowPhase,
      liftScale = rearLiftScale,
      beatLift = 4.dp.toPx() * beatImpact,
      contourWidth = 1.2.dp.toPx(),
      glowWidth = 5.0.dp.toPx(),
      glowAlpha = 0.28f,
      bodyTopAlpha = 0.28f,
      bodyBottomAlpha = 0.04f,
    )

    // Layer 2: Harmonic Mid Veil
    drawRibbon(
      filledPath = midFilledPath,
      contourPath = midContourPath,
      color = tones.mid,
      wavelength = 70.dp.toPx(),
      baseLift = 13.5.dp.toPx(),
      phase = midPhase,
      liftScale = middleLiftScale,
      beatLift = 3.dp.toPx() * beatImpact,
      contourWidth = 1.6.dp.toPx(),
      glowWidth = 6.0.dp.toPx(),
      glowAlpha = 0.38f,
      bodyTopAlpha = 0.42f,
      bodyBottomAlpha = 0.08f,
    )

    // Layer 3: Vibrant Lead Luminous Wave with Specular Core
    drawRibbon(
      filledPath = highFilledPath,
      contourPath = highContourPath,
      color = trackColor,
      wavelength = 44.dp.toPx(),
      baseLift = 10.dp.toPx(),
      phase = highPhase,
      liftScale = frontLiftScale,
      beatLift = 2.dp.toPx() * beatImpact,
      contourWidth = 2.0.dp.toPx(),
      glowWidth = 7.5.dp.toPx(),
      glowAlpha = 0.48f,
      bodyTopAlpha = 0.62f,
      bodyBottomAlpha = 0.14f,
      hasWhiteHighlight = true,
    )
  }
}
