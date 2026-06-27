package com.offgrid.app.link.wifidirect

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
import com.offgrid.app.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val TAG = "WifiDirectConnector"
private const val DISCOVER_TIMEOUT_MS = 5_000L
private const val GROUP_FORM_TIMEOUT_MS = 10_000L
private const val CLIENT_WAIT_TIMEOUT_MS = 60_000L
private const val POLL_INTERVAL_MS = 500L

@SuppressLint("MissingPermission")
class WifiDirectConnector(
    context: Context,
    private val scope: CoroutineScope
) {
    private val appContext = context.applicationContext
    private val wifiP2pManager = appContext.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    private val channel = wifiP2pManager.initialize(appContext, Looper.getMainLooper()) {
        Log.w(TAG, "Wi-Fi Direct channel lost")
    }

    private val _state = MutableStateFlow<NetworkConnectionState>(NetworkConnectionState.Idle)
    val state: StateFlow<NetworkConnectionState> = _state.asStateFlow()

    private var p2pEnabled = false
    private var groupFormed = false
    private var isGroupOwner = false
    private var groupInfo: WifiP2pGroup? = null
    private val discoveredDevices = mutableListOf<WifiP2pDevice>()

    private var activeJob: Job? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    p2pEnabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED
                    Log.d(TAG, "P2P state changed: enabled=$p2pEnabled")
                }
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    requestPeers()
                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    requestConnectionInfo()
                    requestGroupInfo()
                }
                WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                    // no-op
                }
            }
        }
    }

    fun start() {
        val filter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }
        try {
            ContextCompat.registerReceiver(appContext, receiver, filter, RECEIVER_NOT_EXPORTED)
        } catch (e: Exception) {
            Log.w(TAG, "Register receiver failed", e)
        }
    }

    fun stop() {
        activeJob?.cancel()
        activeJob = null
        try {
            appContext.unregisterReceiver(receiver)
        } catch (_: IllegalArgumentException) {
            // ignored
        }
    }

    fun establish(role: NetworkRole) {
        if (activeJob?.isActive == true) {
            Log.d(TAG, "Establish already in progress")
            return
        }
        activeJob = scope.launch {
            try {
                _state.value = NetworkConnectionState.Connecting(appContext.getString(R.string.network_checking_permissions))
                if (!hasPermissions()) {
                    fail(
                        appContext.getString(R.string.network_error_permission),
                        appContext.getString(R.string.network_hint_permission)
                    )
                    return@launch
                }

                when (role) {
                    NetworkRole.GROUP_OWNER -> createGroupAsOwner()
                    NetworkRole.CLIENT -> connectAsClient()
                }
            } catch (e: CancellationException) {
                Log.d(TAG, "Establish cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "Establish failed", e)
                fail(
                    appContext.getString(R.string.network_error_unexpected, e.message),
                    appContext.getString(R.string.network_hint_unexpected)
                )
            }
        }
    }

    fun disconnect() {
        activeJob?.cancel()
        activeJob = null
        wifiP2pManager.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "Group removed")
            }
            override fun onFailure(reason: Int) {
                Log.w(TAG, "Remove group failed: $reason")
            }
        })
        resetState()
        _state.value = NetworkConnectionState.Idle
    }

    private fun hasPermissions(): Boolean {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        return permissions.all {
            ContextCompat.checkSelfPermission(appContext, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private suspend fun createGroupAsOwner() {
        _state.value = NetworkConnectionState.Connecting(appContext.getString(R.string.network_creating_group))

        // Reuse an existing group if this device is already the GO. This avoids
        // createGroup BUSY when a previous P2P group was left behind.
        val existingGroup = requestGroupInfoSync()
        if (existingGroup != null && existingGroup.isGroupOwner) {
            Log.d(TAG, "Reusing existing GO group: ${existingGroup.networkName}")
            groupInfo = existingGroup
            groupFormed = true
            isGroupOwner = true
            publishGroupAndWaitForClient(existingGroup.toGroupInfo())
            return
        }

        val ok = suspendCreateGroup()
        if (!ok) {
            // The call may have failed because a group already exists. If we are
            // already the GO, treat it as success rather than failing the UI.
            val group = requestGroupInfoSync()
            if (group == null || !group.isGroupOwner) {
                fail(
                    appContext.getString(R.string.network_error_create_group),
                    appContext.getString(R.string.network_hint_create_group)
                )
                return
            }
            Log.d(TAG, "createGroup returned busy/error but GO group exists, continuing")
            groupInfo = group
            groupFormed = true
            isGroupOwner = true
        }

        _state.value = NetworkConnectionState.Connecting(appContext.getString(R.string.network_waiting_group_info))
        val info = waitForGroupFormed()
        if (info == null) {
            fail(
                appContext.getString(R.string.network_error_group_timeout),
                appContext.getString(R.string.network_hint_group_timeout)
            )
            return
        }

        publishGroupAndWaitForClient(info)
    }

    private suspend fun publishGroupAndWaitForClient(info: GroupInfo) {
        _state.value = NetworkConnectionState.GroupCreated(info)

        _state.value = NetworkConnectionState.Connecting(appContext.getString(R.string.network_waiting_client))
        val connected = waitForClient()
        if (connected) {
            val updated = currentGroupInfo()
            _state.value = NetworkConnectionState.Connected(updated ?: info)
        } else {
            _state.value = NetworkConnectionState.GroupCreated(info)
        }
    }

    private fun WifiP2pGroup.toGroupInfo(): GroupInfo {
        return GroupInfo(
            networkName = networkName ?: "-",
            passphrase = passphrase,
            isGroupOwner = isGroupOwner,
            clientCount = clientList.size
        )
    }

    private suspend fun connectAsClient() {
        _state.value = NetworkConnectionState.Connecting(appContext.getString(R.string.network_scanning))
        val goDevice = discoverGroupOwner(DISCOVER_TIMEOUT_MS)
        if (goDevice == null) {
            fail(
                appContext.getString(R.string.network_error_no_go),
                appContext.getString(R.string.network_hint_no_go)
            )
            return
        }

        _state.value = NetworkConnectionState.Connecting(appContext.getString(R.string.network_connecting_to, goDevice.deviceName ?: goDevice.deviceAddress))
        val ok = suspendConnect(goDevice)
        if (!ok) {
            fail(
                appContext.getString(R.string.network_error_connect),
                appContext.getString(R.string.network_hint_connect)
            )
            return
        }

        _state.value = NetworkConnectionState.Connecting(appContext.getString(R.string.network_waiting_connection))
        val info = waitForGroupFormed()
        if (info == null) {
            fail(
                appContext.getString(R.string.network_error_connection_timeout),
                appContext.getString(R.string.network_hint_connection_timeout)
            )
            return
        }
        _state.value = NetworkConnectionState.Connected(info)
    }

    private suspend fun connectAsClient(device: WifiP2pDevice) {
        _state.value = NetworkConnectionState.Connecting(appContext.getString(R.string.network_connecting_to, device.deviceName ?: device.deviceAddress))
        val ok = suspendConnect(device)
        if (!ok) {
            fail(
                appContext.getString(R.string.network_error_connect),
                appContext.getString(R.string.network_hint_connect)
            )
            return
        }
        _state.value = NetworkConnectionState.Connecting(appContext.getString(R.string.network_waiting_connection))
        val info = waitForGroupFormed()
        if (info == null) {
            fail(
                appContext.getString(R.string.network_error_connection_timeout),
                appContext.getString(R.string.network_hint_connection_timeout)
            )
            return
        }
        _state.value = NetworkConnectionState.Connected(info)
    }

    private suspend fun discoverGroupOwner(timeoutMs: Long): WifiP2pDevice? {
        discoveredDevices.clear()
        val discoverOk = suspendDiscoverPeers()
        if (!discoverOk) {
            Log.w(TAG, "discoverPeers failed")
        }
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val go = discoveredDevices.firstOrNull { it.isGroupOwner }
            if (go != null) return go
            delay(POLL_INTERVAL_MS)
            requestPeers()
        }
        return discoveredDevices.firstOrNull { it.isGroupOwner }
    }

    private suspend fun waitForGroupFormed(): GroupInfo? {
        val deadline = System.currentTimeMillis() + GROUP_FORM_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            val info = currentGroupInfo()
            if (info != null) return info
            requestConnectionInfo()
            requestGroupInfo()
            delay(POLL_INTERVAL_MS)
        }
        return currentGroupInfo()
    }

    private suspend fun waitForClient(): Boolean {
        val deadline = System.currentTimeMillis() + CLIENT_WAIT_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            val group = requestGroupInfoSync()
            if (group != null && group.clientList.isNotEmpty()) {
                return true
            }
            delay(POLL_INTERVAL_MS)
        }
        return false
    }

    private fun currentGroupInfo(): GroupInfo? {
        val group = groupInfo ?: return null
        if (!groupFormed) return null
        return GroupInfo(
            networkName = group.networkName ?: "-",
            passphrase = group.passphrase,
            isGroupOwner = group.isGroupOwner,
            clientCount = group.clientList.size
        )
    }

    private fun resetState() {
        groupFormed = false
        isGroupOwner = false
        groupInfo = null
        discoveredDevices.clear()
    }

    private fun fail(error: String, hint: String) {
        _state.value = NetworkConnectionState.Failed(error, hint)
    }

    // --- Suspended Wi-Fi Direct operations ---

    private suspend fun suspendCreateGroup(): Boolean = suspendCancellableCoroutine { cont ->
        wifiP2pManager.createGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                cont.resume(true)
            }
            override fun onFailure(reason: Int) {
                Log.w(TAG, "createGroup failed: $reason")
                cont.resume(false)
            }
        })
    }

    private suspend fun suspendDiscoverPeers(): Boolean = suspendCancellableCoroutine { cont ->
        wifiP2pManager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                cont.resume(true)
            }
            override fun onFailure(reason: Int) {
                Log.w(TAG, "discoverPeers failed: $reason")
                cont.resume(false)
            }
        })
    }

    private suspend fun suspendConnect(device: WifiP2pDevice): Boolean = suspendCancellableCoroutine { cont ->
        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
        }
        wifiP2pManager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                cont.resume(true)
            }
            override fun onFailure(reason: Int) {
                Log.w(TAG, "connect failed: $reason")
                cont.resume(false)
            }
        })
    }

    private fun requestPeers() {
        wifiP2pManager.requestPeers(channel) { peers: WifiP2pDeviceList ->
            discoveredDevices.clear()
            discoveredDevices.addAll(peers.deviceList)
            Log.d(TAG, "Peers found: ${peers.deviceList.size}")
        }
    }

    private fun requestConnectionInfo() {
        wifiP2pManager.requestConnectionInfo(channel) { info: WifiP2pInfo ->
            groupFormed = info.groupFormed
            isGroupOwner = info.isGroupOwner
            Log.d(TAG, "Connection info: formed=$groupFormed, isGO=$isGroupOwner")
        }
    }

    private fun requestGroupInfo() {
        wifiP2pManager.requestGroupInfo(channel) { group: WifiP2pGroup? ->
            groupInfo = group
            Log.d(TAG, "Group info: ${group?.networkName}, clients=${group?.clientList?.size}")
        }
    }

    private suspend fun requestGroupInfoSync(): WifiP2pGroup? = suspendCancellableCoroutine { cont ->
        wifiP2pManager.requestGroupInfo(channel) { group: WifiP2pGroup? ->
            cont.resume(group)
        }
    }
}
