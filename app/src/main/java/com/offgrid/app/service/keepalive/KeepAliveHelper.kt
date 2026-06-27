package com.offgrid.app.service.keepalive

import android.content.Context
import android.net.wifi.WifiManager
import android.os.PowerManager
import android.util.Log

/**
 * Acquires wake lock and Wi-Fi lock while the voice service is running.
 *
 * This reduces the chance of the CPU or Wi-Fi radio being suspended while the
 * device is locked, which would otherwise interrupt voice and location packets.
 */
class KeepAliveHelper(context: Context) {

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    fun acquire() {
        if (wakeLock != null) return
        try {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).apply {
                acquire(WAKE_LOCK_TIMEOUT_MS)
            }
            wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, WIFI_LOCK_TAG).apply {
                acquire()
            }
            Log.i(TAG, "Wake lock and Wi-Fi lock acquired")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire keep-alive locks", e)
        }
    }

    fun release() {
        try {
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
            wakeLock = null
            wifiLock?.let {
                if (it.isHeld) it.release()
            }
            wifiLock = null
            Log.i(TAG, "Wake lock and Wi-Fi lock released")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release keep-alive locks", e)
        }
    }

    companion object {
        private const val TAG = "KeepAliveHelper"
        private const val WAKE_LOCK_TAG = "OffGrid::VoiceLock"
        private const val WIFI_LOCK_TAG = "OffGrid::WifiLock"
        private const val WAKE_LOCK_TIMEOUT_MS = 30 * 60 * 1000L
    }
}
