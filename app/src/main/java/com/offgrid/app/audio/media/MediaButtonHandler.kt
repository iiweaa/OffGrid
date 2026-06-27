package com.offgrid.app.audio.media

import android.content.Context
import android.content.Intent
import android.media.session.MediaSession
import android.os.Build
import android.util.Log
import android.view.KeyEvent

/**
 * Handles Bluetooth / wired headset media buttons during a call.
 *
 * - Short press of the headset hook toggles microphone mute.
 * - Long press (>= 500ms) ends the call.
 *
 * Uses a [MediaSession] so the app receives media button events while the
 * foreground service is running.
 */
class MediaButtonHandler(context: Context) {

    private val mediaSession: MediaSession = MediaSession(context, TAG).apply {
        setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS)
        setCallback(object : MediaSession.Callback() {
            override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
                handleMediaButton(mediaButtonIntent)
                return true
            }
        })
    }

    private var onToggleMute: (() -> Unit)? = null
    private var onEndCall: (() -> Unit)? = null

    private var keyDownTime: Long = 0

    fun setCallbacks(
        onToggleMute: (() -> Unit)? = null,
        onEndCall: (() -> Unit)? = null
    ) {
        this.onToggleMute = onToggleMute
        this.onEndCall = onEndCall
    }

    fun start() {
        mediaSession.isActive = true
        Log.d(TAG, "Media session activated")
    }

    fun stop() {
        mediaSession.isActive = false
        mediaSession.release()
        Log.d(TAG, "Media session released")
    }

    private fun handleMediaButton(intent: Intent) {
        val event = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
        } ?: return

        when (event.keyCode) {
            KeyEvent.KEYCODE_HEADSETHOOK,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                when (event.action) {
                    KeyEvent.ACTION_DOWN -> {
                        if (keyDownTime == 0L) {
                            keyDownTime = event.eventTime
                        }
                    }
                    KeyEvent.ACTION_UP -> {
                        val duration = if (keyDownTime != 0L) event.eventTime - keyDownTime else 0
                        keyDownTime = 0
                        if (duration >= LONG_PRESS_THRESHOLD_MS) {
                            Log.d(TAG, "Long press detected -> end call")
                            onEndCall?.invoke()
                        } else {
                            Log.d(TAG, "Short press detected -> toggle mute")
                            onToggleMute?.invoke()
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "MediaButtonHandler"
        private const val LONG_PRESS_THRESHOLD_MS = 500L
    }
}
