package com.offgrid.app

import android.Manifest
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
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import android.annotation.SuppressLint
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.offgrid.app.link.ConcurrencyStatus
import com.offgrid.app.link.WifiDirectCapability
import com.offgrid.app.link.WifiDirectCapabilityChecker
import com.offgrid.app.ui.theme.OffGridTheme

private const val TAG = "WifiDirectTest"

@SuppressLint("MissingPermission")
class WifiDirectTestActivity : ComponentActivity() {

    private lateinit var wifiP2pManager: WifiP2pManager
    private lateinit var channel: WifiP2pManager.Channel

    private val discoveredDevices = mutableStateListOf<WifiP2pDevice>()
    private val logs = mutableStateListOf<String>()
    private var isGroupOwner by mutableStateOf(false)
    private var groupFormed by mutableStateOf(false)
    private var groupInfo by mutableStateOf<WifiP2pGroup?>(null)
    private var capability by mutableStateOf(WifiDirectCapability(ConcurrencyStatus.UNKNOWN, "default"))

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            log("Permissions granted")
        } else {
            log("Some permissions denied: ${permissions.filter { !it.value }.keys}")
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    log("P2P state: ${if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) "ENABLED" else "DISABLED"}")
                }
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    log("Peers changed")
                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    log("Connection changed")
                    requestConnectionInfo()
                    requestGroupInfo()
                }
                WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(
                            WifiP2pManager.EXTRA_WIFI_P2P_DEVICE,
                            WifiP2pDevice::class.java
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)
                    }
                    log("This device: ${device?.deviceName} / ${device?.deviceAddress}")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        wifiP2pManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = wifiP2pManager.initialize(this, Looper.getMainLooper()) {
            log("Channel lost")
        }
        capability = WifiDirectCapabilityChecker.check(this)

        setContent {
            OffGridTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WifiDirectTestScreen()
                }
            }
        }

        requestPermissions()
        registerReceiver()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(receiver)
        } catch (_: IllegalArgumentException) {
            // ignored
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Not strictly needed for API 31+ if NEARBY_WIFI_DEVICES is granted,
            // but kept for broader compatibility with some OEM implementations.
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun registerReceiver() {
        val intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }
        registerReceiver(receiver, intentFilter)
    }

    private fun log(message: String) {
        Log.d(TAG, message)
        logs.add("${System.currentTimeMillis() % 100000}: $message")
    }

    private fun createGroup() {
        wifiP2pManager.createGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                log("createGroup requested")
            }
            override fun onFailure(reason: Int) {
                log("createGroup failed: $reason")
            }
        })
    }

    private fun removeGroup() {
        wifiP2pManager.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                log("removeGroup success")
                groupFormed = false
                groupInfo = null
            }
            override fun onFailure(reason: Int) {
                log("removeGroup failed: $reason")
            }
        })
    }

    private fun discoverPeers() {
        wifiP2pManager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                log("discoverPeers requested")
            }
            override fun onFailure(reason: Int) {
                log("discoverPeers failed: $reason")
            }
        })
    }

    private fun requestPeers() {
        wifiP2pManager.requestPeers(channel) { peers: WifiP2pDeviceList ->
            discoveredDevices.clear()
            discoveredDevices.addAll(peers.deviceList)
            log("Found ${peers.deviceList.size} peers")
        }
    }

    private fun connectTo(device: WifiP2pDevice) {
        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
        }
        wifiP2pManager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                log("connect requested to ${device.deviceName}")
            }
            override fun onFailure(reason: Int) {
                log("connect failed: $reason")
            }
        })
    }

    private fun requestConnectionInfo() {
        wifiP2pManager.requestConnectionInfo(channel) { info: WifiP2pInfo ->
            groupFormed = info.groupFormed
            isGroupOwner = info.isGroupOwner
            log("Connection info: groupFormed=${info.groupFormed}, isGO=${info.isGroupOwner}")
        }
    }

    private fun requestGroupInfo() {
        wifiP2pManager.requestGroupInfo(channel) { group: WifiP2pGroup? ->
            groupInfo = group
            group?.let {
                log("Group info: SSID=${it.networkName}, pass=${it.passphrase}, isGO=${it.isGroupOwner}")
            }
        }
    }

    private fun probeConcurrency() {
        log("Probing AP-STA concurrency...")
        WifiDirectCapabilityChecker.probe(
            wifiP2pManager,
            channel,
            groupFormed,
            isGroupOwner
        ) { result ->
            capability = result
            log("Concurrency probe result: ${result.concurrency} (source=${result.source})")
        }
    }

    @Composable
    fun WifiDirectTestScreen() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Wi-Fi Direct Concurrency Test",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "Status: groupFormed=$groupFormed, isGO=$isGroupOwner",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Capability: ${capability.statusLabel()} (${capability.source})",
                style = MaterialTheme.typography.bodyMedium
            )

            Button(
                onClick = ::probeConcurrency,
                enabled = groupFormed && !isGroupOwner,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Probe AP-STA concurrency")
            }

            groupInfo?.let { group ->
                Text(text = "SSID: ${group.networkName}")
                Text(text = "Passphrase: ${group.passphrase}")
                Text(text = "Group Owner: ${group.isGroupOwner}")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = ::createGroup, modifier = Modifier.weight(1f)) {
                    Text("Create Group")
                }
                Button(onClick = ::removeGroup, modifier = Modifier.weight(1f)) {
                    Text("Remove Group")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = ::discoverPeers, modifier = Modifier.weight(1f)) {
                    Text("Discover")
                }
                Button(onClick = ::requestPeers, modifier = Modifier.weight(1f)) {
                    Text("List Peers")
                }
            }

            Text(
                text = "Discovered Devices (${discoveredDevices.size}):",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )

            discoveredDevices.forEach { device ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "${device.deviceName} (${device.deviceAddress})")
                    Button(onClick = { connectTo(device) }) {
                        Text("Connect")
                    }
                }
            }

            Text(
                text = "Logs:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )

            logs.forEach { log ->
                Text(
                    text = log,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}
