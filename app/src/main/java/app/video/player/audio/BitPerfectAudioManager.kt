package app.video.player.audio

import java.nio.ByteBuffer

/**
 * Manages exclusive bit-perfect direct audio output via [InternalDirectStream].
 *
 * Used for offline lossless playback (FLAC, WAV, ALAC, etc.) when bit-perfect
 * mode is enabled and an external USB DAC or exclusive AAudio route is available.
 */
class BitPerfectAudioManager {
  private var activeStream: InternalDirectStream? = null

  /**
   * Probes the current audio route for exclusive (MMAP/AAudio) bit-perfect support.
   */
  fun probe(channelCount: Int = 2): InternalDirectStream.Probe? =
    InternalDirectStream.probe(channelCount)

  /**
   * Opens a direct exclusive stream for the given PCM parameters.
   * Returns `true` if the stream opened and started successfully.
   */
  fun open(
    sampleRateHz: Int,
    channelCount: Int,
    encoding: Int,
  ): Boolean {
    close()
    val stream = InternalDirectStream()
    if (!stream.isAvailable) {
      stream.close()
      return false
    }
    if (stream.open(sampleRateHz, channelCount, encoding) != InternalDirectStream.OPEN_OK) {
      stream.close()
      return false
    }
    if (!stream.start()) {
      stream.close()
      return false
    }
    activeStream = stream
    return true
  }

  /**
   * Writes raw PCM frames from [buffer] into the active direct stream.
   */
  fun write(buffer: ByteBuffer, inputEncoding: Int, inputChannels: Int): Int {
    val stream = activeStream ?: return 0
    return stream.write(buffer, inputEncoding, inputChannels)
  }

  fun flush() {
    activeStream?.flush()
  }

  fun pause() {
    activeStream?.pause()
  }

  fun resume() {
    activeStream?.start()
  }

  fun isActive(): Boolean = activeStream != null

  fun close() {
    activeStream?.close()
    activeStream = null
  }
}
