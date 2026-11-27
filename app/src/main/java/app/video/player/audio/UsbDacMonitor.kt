package app.video.player.audio

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import app.video.player.preferences.AudioPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Monitors USB DAC connections via [AudioManager.registerAudioDeviceCallback]
 * and automatically enables [AudioPreferences.bitPerfectAudio] when a USB DAC
 * sink is connected.
 */
class UsbDacMonitor(
  context: Context,
  private val audioPreferences: AudioPreferences,
) {
  private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
  private val _isUsbDacConnected = MutableStateFlow(false)
  val isUsbDacConnected: StateFlow<Boolean> = _isUsbDacConnected.asStateFlow()

  private val callback = object : AudioDeviceCallback() {
    override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
      if (addedDevices.any { it.isUsbDacSink() }) {
        _isUsbDacConnected.value = true
        audioPreferences.bitPerfectAudio.set(true)
      }
    }

    override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
      val stillConnected = hasConnectedUsbDac()
      _isUsbDacConnected.value = stillConnected
      if (!stillConnected) {
        audioPreferences.bitPerfectAudio.set(false)
      }
    }
  }

  fun start() {
    val manager = audioManager ?: return
    val connected = hasConnectedUsbDac()
    _isUsbDacConnected.value = connected
    if (connected) {
      audioPreferences.bitPerfectAudio.set(true)
    }
    manager.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
  }

  fun stop() {
    audioManager?.unregisterAudioDeviceCallback(callback)
  }

  private fun hasConnectedUsbDac(): Boolean {
    val devices = audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS) ?: return false
    return devices.any { it.isUsbDacSink() }
  }

  private fun AudioDeviceInfo.isUsbDacSink(): Boolean =
    isSink && (type == AudioDeviceInfo.TYPE_USB_DEVICE || type == AudioDeviceInfo.TYPE_USB_HEADSET)
}
