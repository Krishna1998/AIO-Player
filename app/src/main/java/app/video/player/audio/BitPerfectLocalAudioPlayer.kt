package app.video.player.audio

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Decodes local lossless audio files (FLAC, WAV, ALAC, etc.) via [MediaExtractor] +
 * [MediaCodec] and streams raw PCM directly to [BitPerfectAudioManager] without
 * Android AudioFlinger resampling.
 */
class BitPerfectLocalAudioPlayer(
  private val bitPerfectManager: BitPerfectAudioManager = BitPerfectAudioManager(),
) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var playbackJob: Job? = null
  private val isPaused = AtomicBoolean(false)

  fun play(
    filePath: String,
    onError: (Throwable) -> Unit = {},
    onCompleted: () -> Unit = {},
  ) {
    stop()
    isPaused.set(false)
    playbackJob = scope.launch {
      var extractor: MediaExtractor? = null
      var codec: MediaCodec? = null
      try {
        extractor = MediaExtractor().apply { setDataSource(filePath) }
        val audioTrackIndex = (0 until extractor.trackCount).firstOrNull { index ->
          extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        } ?: error("No audio track found in $filePath")

        extractor.selectTrack(audioTrackIndex)
        val format = extractor.getTrackFormat(audioTrackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: error("Missing MIME type")
        val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val pcmEncoding = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
          format.containsKey(MediaFormat.KEY_PCM_ENCODING)
        ) {
          format.getInteger(MediaFormat.KEY_PCM_ENCODING)
        } else {
          AudioFormat.ENCODING_PCM_16BIT
        }

        val directEncoding = when (pcmEncoding) {
          AudioFormat.ENCODING_PCM_FLOAT -> 3 // kDirectFloat
          AudioFormat.ENCODING_PCM_32BIT -> 2 // kDirectI32
          AudioFormat.ENCODING_PCM_24BIT_PACKED -> 1 // kDirectI24Packed
          else -> 0 // kDirectI16
        }

        if (!bitPerfectManager.open(sampleRate, channelCount, directEncoding)) {
          error("Failed to open bit-perfect direct stream ($sampleRate Hz, $channelCount ch)")
        }

        codec = MediaCodec.createDecoderByType(mime).apply {
          configure(format, null, null, 0)
          start()
        }

        val bufferInfo = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false

        while (isActive && !outputDone) {
          if (isPaused.get()) {
            Thread.sleep(20)
            continue
          }

          if (!inputDone) {
            val inputIndex = codec.dequeueInputBuffer(10_000)
            if (inputIndex >= 0) {
              val inputBuffer = codec.getInputBuffer(inputIndex) ?: continue
              val sampleSize = extractor.readSampleData(inputBuffer, 0)
              if (sampleSize < 0) {
                codec.queueInputBuffer(inputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                inputDone = true
              } else {
                codec.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                extractor.advance()
              }
            }
          }

          val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000)
          if (outputIndex >= 0) {
            val outputBuffer = codec.getOutputBuffer(outputIndex)
            if (outputBuffer != null && bufferInfo.size > 0) {
              val directBuf = ByteBuffer.allocateDirect(bufferInfo.size)
              outputBuffer.position(bufferInfo.offset)
              outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
              directBuf.put(outputBuffer)
              directBuf.flip()
              while (directBuf.hasRemaining() && isActive && !isPaused.get()) {
                val written = bitPerfectManager.write(directBuf, directEncoding, channelCount)
                if (written <= 0) Thread.sleep(2)
              }
            }
            codec.releaseOutputBuffer(outputIndex, false)
            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
              outputDone = true
            }
          }
        }

        if (outputDone) {
          onCompleted()
        }
      } catch (t: Throwable) {
        onError(t)
      } finally {
        runCatching { codec?.stop() }
        runCatching { codec?.release() }
        runCatching { extractor?.release() }
        bitPerfectManager.close()
      }
    }
  }

  fun pause() {
    isPaused.set(true)
    bitPerfectManager.pause()
  }

  fun resume() {
    isPaused.set(false)
    bitPerfectManager.resume()
  }

  fun stop() {
    playbackJob?.cancel()
    playbackJob = null
    bitPerfectManager.close()
  }
}
