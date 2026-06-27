package com.offgrid.app.audio.router

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Monitors audio device changes and keeps voice routed to the loud speaker
 * when no wired or Bluetooth headset is connected.
 *
 * This improves routing stability across headset plug/unplug events and
 * OEM-specific audio policy changes.
 */
class AudioRouter(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val callback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            route()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            route()
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    fun start() {
        audioManager.registerAudioDeviceCallback(callback, mainHandler)
        route()
        Log.d(TAG, "Audio router started")
    }

    fun stop() {
        try {
            audioManager.unregisterAudioDeviceCallback(callback)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unregister audio device callback", e)
        }
    }

    private fun route() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val current = audioManager.communicationDevice
            if (current != null && isHeadset(current)) {
                Log.d(TAG, "Keeping current headset communication device")
                return
            }
            val speaker = audioManager.availableCommunicationDevices
                .find { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
            if (speaker != null) {
                val ok = audioManager.setCommunicationDevice(speaker)
                Log.d(TAG, "Routed voice to speaker: $ok")
            } else {
                Log.w(TAG, "No built-in speaker communication device available")
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = true
            Log.d(TAG, "Routed voice to speakerphone (legacy)")
        }
    }

    private fun isHeadset(device: AudioDeviceInfo): Boolean {
        return device.type in setOf(
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_USB_HEADSET
        )
    }

    companion object {
        private const val TAG = "AudioRouter"
    }
}
