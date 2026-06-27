package com.offgrid.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.offgrid.app.MainActivity
import com.offgrid.app.OffGridApplication
import com.offgrid.app.R
import com.offgrid.app.audio.AudioEngine
import com.offgrid.app.audio.media.MediaButtonHandler
import com.offgrid.app.audio.router.AudioRouter
import com.offgrid.app.link.LinkManager
import com.offgrid.app.service.keepalive.KeepAliveHelper
import com.offgrid.app.link.location.Location
import com.offgrid.app.link.location.LocationEngine
import com.offgrid.app.link.location.LocationPayload
import com.offgrid.app.link.packet.PacketType

class VoiceService : Service() {

    companion object {
        private const val TAG = "VoiceService"
        private const val CHANNEL_ID = "offgrid_voice"
        private const val NOTIFICATION_ID = 1

        const val ACTION_START = "com.offgrid.app.action.START_VOICE"
        const val ACTION_STOP = "com.offgrid.app.action.STOP_VOICE"
        const val ACTION_TOGGLE_MUTE = "com.offgrid.app.action.TOGGLE_MUTE"

        fun start(context: Context) {
            val intent = Intent(context, VoiceService::class.java).apply { action = ACTION_START }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.startService(Intent(context, VoiceService::class.java).apply { action = ACTION_STOP })
        }

        fun toggleMute(context: Context) {
            context.startService(Intent(context, VoiceService::class.java).apply { action = ACTION_TOGGLE_MUTE })
        }
    }

    private lateinit var audioEngine: AudioEngine
    private lateinit var linkManager: LinkManager
    private lateinit var locationEngine: LocationEngine
    private lateinit var keepAliveHelper: KeepAliveHelper
    private lateinit var audioRouter: AudioRouter
    private lateinit var mediaButtonHandler: MediaButtonHandler

    override fun onCreate() {
        super.onCreate()
        audioEngine = AudioEngine(this)
        linkManager = LinkManager((application as OffGridApplication).localNodeId)
        locationEngine = LocationEngine(this)
        keepAliveHelper = KeepAliveHelper(this)
        audioRouter = AudioRouter(this)
        mediaButtonHandler = MediaButtonHandler(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startVoice()
            ACTION_STOP -> stopVoice()
            ACTION_TOGGLE_MUTE -> toggleMute()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startVoice() {
        if (VoiceStateHolder.state.value.isRunning) return

        try {
            startForeground(NOTIFICATION_ID, buildNotification())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground", e)
            VoiceStateHolder.update { it.copy(lastError = "Foreground start failed: ${e.message}") }
            stopSelf()
            return
        }

        audioEngine.setOnEncodedFrame { data, length ->
            linkManager.send(data, length)
        }
        linkManager.setCallbacks(
            onNeighborsChanged = { neighbors ->
                VoiceStateHolder.update {
                    it.copy(
                        peer = neighbors.firstOrNull()?.address,
                        neighbors = neighbors
                    )
                }
            },
            onPacket = { data, length, _ ->
                audioEngine.playPacket(data, length)
            },
            onLocationReceived = { nodeId, location ->
                VoiceStateHolder.update {
                    it.copy(peerLocations = it.peerLocations + (nodeId to location))
                }
            }
        )

        locationEngine.setCallback { androidLocation ->
            val location = Location(
                latitude = androidLocation.latitude,
                longitude = androidLocation.longitude,
                altitude = androidLocation.altitude,
                accuracy = androidLocation.accuracy,
                timestampMs = androidLocation.time
            )
            VoiceStateHolder.update { it.copy(myLocation = location) }
            val payload = LocationPayload.serialize(location)
            linkManager.send(PacketType.LOCATION, payload, payload.size)
        }
        locationEngine.start()

        mediaButtonHandler.setCallbacks(
            onToggleMute = {
                val newMuted = !audioEngine.isMuted()
                audioEngine.setMuted(newMuted)
                VoiceStateHolder.update { it.copy(isMuted = newMuted) }
            },
            onEndCall = ::stopVoice
        )
        mediaButtonHandler.start()

        val ok = audioEngine.start()
        if (!ok) {
            VoiceStateHolder.update { it.copy(lastError = "Audio engine start failed") }
            stopVoice()
            return
        }
        audioRouter.start()
        keepAliveHelper.acquire()
        linkManager.start()
        VoiceStateHolder.update { it.copy(isRunning = true, lastError = null) }
    }

    private fun stopVoice() {
        mediaButtonHandler.stop()
        locationEngine.stop()
        linkManager.stop()
        audioRouter.stop()
        audioEngine.stop()
        keepAliveHelper.release()
        VoiceStateHolder.update { VoiceState() }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun toggleMute() {
        if (!VoiceStateHolder.state.value.isRunning) return
        val newMuted = !audioEngine.isMuted()
        audioEngine.setMuted(newMuted)
        VoiceStateHolder.update { it.copy(isMuted = newMuted) }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, VoiceService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_content))
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentIntent(contentIntent)
            .addAction(0, getString(R.string.notification_stop), stopIntent)
            .setOngoing(true)
            .build()
    }
}
