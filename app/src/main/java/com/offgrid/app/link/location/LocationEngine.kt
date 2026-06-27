package com.offgrid.app.link.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Acquires device location and notifies the caller on each update.
 *
 * Uses both GPS and network providers. The caller is responsible for forwarding
 * the location into the mesh as a [PacketType.LOCATION] payload.
 */
class LocationEngine(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var callback: ((android.location.Location) -> Unit)? = null

    private val listener = object : LocationListener {
        override fun onLocationChanged(location: android.location.Location) {
            Log.d(TAG, "Location update: ${location.latitude}, ${location.longitude} (acc=${location.accuracy})")
            callback?.invoke(location)
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    fun setCallback(callback: (android.location.Location) -> Unit) {
        this.callback = callback
    }

    @SuppressLint("MissingPermission")
    fun start(intervalMs: Long = DEFAULT_INTERVAL_MS) {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Location permission not granted")
            return
        }

        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                intervalMs,
                0f,
                listener,
                Looper.getMainLooper()
            )
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                intervalMs,
                0f,
                listener,
                Looper.getMainLooper()
            )

            // Emit last known location immediately if available.
            val last = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            last?.let { callback?.invoke(it) }
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to request location updates", e)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Location provider not available", e)
        }
    }

    fun stop() {
        try {
            locationManager.removeUpdates(listener)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to remove location updates", e)
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val TAG = "LocationEngine"
        const val DEFAULT_INTERVAL_MS = 2000L
    }
}
