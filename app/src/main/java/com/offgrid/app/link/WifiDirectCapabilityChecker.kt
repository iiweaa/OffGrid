package com.offgrid.app.link

import android.content.Context
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import java.lang.reflect.Method

/**
 * Detects whether the device supports Wi-Fi Direct AP-STA concurrency.
 *
 * There is no public Android API for this capability, so the checker first tries
 * a few known hidden reflection methods and falls back to an empirical probe
 * (create a group while already connected as a client) when requested.
 */
object WifiDirectCapabilityChecker {

    private const val TAG = "WifiDirectCapChecker"

    // Candidate hidden method names on WifiP2pManager / WifiManager.
    private val REFLECTION_METHODS = listOf(
        "isConcurrentSupported",
        "isP2pConcurrencySupported",
        "isStaApConcurrencySupported",
        "isApStaConcurrencySupported"
    )

    /**
     * Passive check using reflection. Does not modify any P2P state.
     */
    fun check(context: Context): WifiDirectCapability {
        val manager = context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
        if (manager == null) {
            Log.w(TAG, "No WIFI_P2P_SERVICE")
            return WifiDirectCapability(ConcurrencyStatus.UNKNOWN, "no_service")
        }

        for (name in REFLECTION_METHODS) {
            try {
                val method: Method = manager.javaClass.getDeclaredMethod(name)
                method.isAccessible = true
                val result = method.invoke(manager)
                val status = if (result == true) {
                    ConcurrencyStatus.SUPPORTED
                } else {
                    ConcurrencyStatus.UNSUPPORTED
                }
                Log.i(TAG, "Reflection method $name returned $result")
                return WifiDirectCapability(status, "reflection:$name")
            } catch (e: NoSuchMethodException) {
                Log.d(TAG, "Reflection method $name not found")
            } catch (e: Exception) {
                Log.w(TAG, "Reflection method $name failed: ${e.message}")
            }
        }

        return WifiDirectCapability(ConcurrencyStatus.UNKNOWN, "default")
    }

    /**
     * Empirical probe: while already connected as a client, try to create a new group.
     * If the system allows a second group, AP-STA concurrency is likely supported.
     */
    @android.annotation.SuppressLint("MissingPermission")
    fun probe(
        wifiP2pManager: WifiP2pManager,
        channel: WifiP2pManager.Channel,
        groupFormed: Boolean,
        isGroupOwner: Boolean,
        callback: (WifiDirectCapability) -> Unit
    ) {
        if (!groupFormed) {
            callback(WifiDirectCapability(ConcurrencyStatus.UNKNOWN, "probe:no_group"))
            return
        }
        if (isGroupOwner) {
            // Already acting as GO: cannot easily test client concurrency from here.
            callback(WifiDirectCapability(ConcurrencyStatus.UNKNOWN, "probe:is_go"))
            return
        }

        wifiP2pManager.createGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.i(TAG, "Probe createGroup succeeded while client -> concurrency supported")
                callback(WifiDirectCapability(ConcurrencyStatus.SUPPORTED, "probe:create_group_success"))
            }

            override fun onFailure(reason: Int) {
                Log.i(TAG, "Probe createGroup failed while client, reason=$reason")
                val status = when (reason) {
                    WifiP2pManager.P2P_UNSUPPORTED,
                    WifiP2pManager.BUSY -> ConcurrencyStatus.UNSUPPORTED
                    else -> ConcurrencyStatus.UNKNOWN
                }
                callback(WifiDirectCapability(status, "probe:create_group_failure:$reason"))
            }
        })
    }
}
