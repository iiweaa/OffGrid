package com.offgrid.app

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.annotation.SuppressLint
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.offgrid.app.link.ConcurrencyStatus
import com.offgrid.app.link.WifiDirectCapability
import com.offgrid.app.link.WifiDirectCapabilityChecker
import com.offgrid.app.ui.theme.OffGridTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    private var p2pEnabled by mutableStateOf(false)

    private var isCreatingGroup by mutableStateOf(false)
    private var isRemovingGroup by mutableStateOf(false)
    private var isDiscovering by mutableStateOf(false)
    private var connectingAddress by mutableStateOf<String?>(null)

    private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            log(R.string.wifi_direct_perm_granted)
        } else {
            log(getString(R.string.wifi_direct_perm_denied, permissions.filter { !it.value }.keys.joinToString()))
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    p2pEnabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED
                    log(
                        if (p2pEnabled) R.string.wifi_direct_p2p_enabled
                        else R.string.wifi_direct_p2p_disabled
                    )
                }
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    log(R.string.wifi_direct_peers_changed)
                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    log(R.string.wifi_direct_connection_changed)
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
                    log(getString(R.string.wifi_direct_this_device, device?.deviceName ?: "?", device?.deviceAddress ?: "?"))
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        wifiP2pManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = wifiP2pManager.initialize(this, Looper.getMainLooper()) {
            log(R.string.wifi_direct_channel_lost)
        }
        capability = WifiDirectCapabilityChecker.check(this)

        setContent {
            OffGridTheme {
                WifiDirectTestApp(
                    p2pEnabled = p2pEnabled,
                    groupFormed = groupFormed,
                    isGroupOwner = isGroupOwner,
                    capability = capability,
                    groupInfo = groupInfo,
                    devices = discoveredDevices,
                    logs = logs,
                    isCreatingGroup = isCreatingGroup,
                    isRemovingGroup = isRemovingGroup,
                    isDiscovering = isDiscovering,
                    connectingAddress = connectingAddress,
                    onBack = { finish() },
                    onCreateGroup = ::createGroup,
                    onRemoveGroup = ::removeGroup,
                    onDiscover = ::discoverPeers,
                    onListPeers = ::requestPeers,
                    onConnect = ::connectTo,
                    onProbe = ::probeConcurrency
                )
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
        val timestamp = timeFormatter.format(Date())
        logs.add("$timestamp  $message")
        if (logs.size > 200) {
            logs.removeAt(0)
        }
    }

    private fun log(@StringRes resId: Int) {
        log(getString(resId))
    }

    private fun createGroup() {
        isCreatingGroup = true
        wifiP2pManager.createGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                isCreatingGroup = false
                log(R.string.wifi_direct_create_group_requested)
            }
            override fun onFailure(reason: Int) {
                isCreatingGroup = false
                log(getString(R.string.wifi_direct_create_group_failed, reason))
            }
        })
    }

    private fun removeGroup() {
        isRemovingGroup = true
        wifiP2pManager.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                isRemovingGroup = false
                groupFormed = false
                groupInfo = null
                log(R.string.wifi_direct_remove_group_success)
            }
            override fun onFailure(reason: Int) {
                isRemovingGroup = false
                log(getString(R.string.wifi_direct_remove_group_failed, reason))
            }
        })
    }

    private fun discoverPeers() {
        isDiscovering = true
        wifiP2pManager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                log(R.string.wifi_direct_discover_requested)
                lifecycleScope.launch {
                    delay(60_000)
                    isDiscovering = false
                }
            }
            override fun onFailure(reason: Int) {
                isDiscovering = false
                log(getString(R.string.wifi_direct_discover_failed, reason))
            }
        })
    }

    private fun requestPeers() {
        wifiP2pManager.requestPeers(channel) { peers: WifiP2pDeviceList ->
            discoveredDevices.clear()
            discoveredDevices.addAll(peers.deviceList)
            log(getString(R.string.wifi_direct_peers_found, peers.deviceList.size))
        }
    }

    private fun connectTo(device: WifiP2pDevice) {
        connectingAddress = device.deviceAddress
        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
        }
        wifiP2pManager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                connectingAddress = null
                log(getString(R.string.wifi_direct_connect_requested, device.deviceName))
            }
            override fun onFailure(reason: Int) {
                connectingAddress = null
                log(getString(R.string.wifi_direct_connect_failed, reason))
            }
        })
    }

    private fun requestConnectionInfo() {
        wifiP2pManager.requestConnectionInfo(channel) { info: WifiP2pInfo ->
            groupFormed = info.groupFormed
            isGroupOwner = info.isGroupOwner
            log(getString(R.string.wifi_direct_connection_info, info.groupFormed, info.isGroupOwner))
        }
    }

    private fun requestGroupInfo() {
        wifiP2pManager.requestGroupInfo(channel) { group: WifiP2pGroup? ->
            groupInfo = group
            group?.let {
                log(getString(R.string.wifi_direct_group_info, it.networkName, it.passphrase, it.isGroupOwner))
            }
        }
    }

    private fun probeConcurrency() {
        log(R.string.wifi_direct_probe_started)
        WifiDirectCapabilityChecker.probe(
            wifiP2pManager,
            channel,
            groupFormed,
            isGroupOwner
        ) { result ->
            capability = result
            log(getString(R.string.wifi_direct_probe_result, result.concurrency, result.source))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WifiDirectTestApp(
    p2pEnabled: Boolean,
    groupFormed: Boolean,
    isGroupOwner: Boolean,
    capability: WifiDirectCapability,
    groupInfo: WifiP2pGroup?,
    devices: List<WifiP2pDevice>,
    logs: List<String>,
    isCreatingGroup: Boolean,
    isRemovingGroup: Boolean,
    isDiscovering: Boolean,
    connectingAddress: String?,
    onBack: () -> Unit,
    onCreateGroup: () -> Unit,
    onRemoveGroup: () -> Unit,
    onDiscover: () -> Unit,
    onListPeers: () -> Unit,
    onConnect: (WifiP2pDevice) -> Unit,
    onProbe: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_wifi_direct_test)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatusCard(
                p2pEnabled = p2pEnabled,
                groupFormed = groupFormed,
                isGroupOwner = isGroupOwner,
                capability = capability
            )

            ActionsCard(
                groupFormed = groupFormed,
                isGroupOwner = isGroupOwner,
                isCreatingGroup = isCreatingGroup,
                isRemovingGroup = isRemovingGroup,
                isDiscovering = isDiscovering,
                onCreateGroup = onCreateGroup,
                onRemoveGroup = onRemoveGroup,
                onDiscover = onDiscover,
                onListPeers = onListPeers,
                onProbe = onProbe
            )

            if (groupFormed && groupInfo != null) {
                GroupInfoCard(
                    group = groupInfo,
                    onCopyPassphrase = { passphrase ->
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                passphrase?.let {
                                    context.copyToClipboard(it)
                                    context.getString(R.string.passphrase_copied)
                                } ?: context.getString(R.string.passphrase_unavailable)
                            )
                        }
                    }
                )
            }

            DevicesCard(
                devices = devices,
                connectingAddress = connectingAddress,
                onConnect = onConnect
            )

            LogsCard(logs = logs)
        }
    }
}

private fun Context.copyToClipboard(text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("passphrase", text))
}

@Composable
private fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            content()
        }
    }
}

@Composable
private fun StatusCard(
    p2pEnabled: Boolean,
    groupFormed: Boolean,
    isGroupOwner: Boolean,
    capability: WifiDirectCapability
) {
    val groupLabel = when {
        !groupFormed -> stringResource(R.string.wifi_direct_group_idle)
        isGroupOwner -> stringResource(R.string.wifi_direct_group_owner)
        else -> stringResource(R.string.wifi_direct_group_client)
    }

    SectionCard(title = stringResource(R.string.wifi_direct_status)) {
        StatusRow(
            label = stringResource(R.string.wifi_direct_p2p),
            value = stringResource(if (p2pEnabled) R.string.enabled else R.string.disabled),
            valueColor = if (p2pEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
        StatusRow(
            label = stringResource(R.string.wifi_direct_group),
            value = groupLabel,
            valueColor = when {
                !groupFormed -> MaterialTheme.colorScheme.onSurfaceVariant
                isGroupOwner -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.secondary
            }
        )
        StatusRow(
            label = stringResource(R.string.wifi_direct_multihop),
            value = capability.statusLabel(),
            valueColor = when (capability.concurrency) {
                ConcurrencyStatus.SUPPORTED -> MaterialTheme.colorScheme.primary
                ConcurrencyStatus.UNSUPPORTED -> MaterialTheme.colorScheme.error
                ConcurrencyStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun StatusRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = valueColor
        )
    }
}

@Composable
private fun ActionsCard(
    groupFormed: Boolean,
    isGroupOwner: Boolean,
    isCreatingGroup: Boolean,
    isRemovingGroup: Boolean,
    isDiscovering: Boolean,
    onCreateGroup: () -> Unit,
    onRemoveGroup: () -> Unit,
    onDiscover: () -> Unit,
    onListPeers: () -> Unit,
    onProbe: () -> Unit
) {
    SectionCard(title = stringResource(R.string.wifi_direct_actions)) {
        Button(
            onClick = onCreateGroup,
            modifier = Modifier.fillMaxWidth(),
            enabled = !groupFormed && !isCreatingGroup
        ) {
            Text(
                stringResource(
                    if (isCreatingGroup) R.string.wifi_direct_creating_group
                    else R.string.wifi_direct_create_group
                )
            )
        }

        OutlinedButton(
            onClick = onRemoveGroup,
            modifier = Modifier.fillMaxWidth(),
            enabled = groupFormed && !isRemovingGroup
        ) {
            Text(
                stringResource(
                    if (isRemovingGroup) R.string.wifi_direct_removing_group
                    else R.string.wifi_direct_remove_group
                )
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onDiscover,
                modifier = Modifier.weight(1f),
                enabled = !isDiscovering
            ) {
                Text(
                    stringResource(
                        if (isDiscovering) R.string.wifi_direct_scanning
                        else R.string.wifi_direct_discover
                    )
                )
            }
            OutlinedButton(
                onClick = onListPeers,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.wifi_direct_list_peers))
            }
        }

        val canProbe = groupFormed && !isGroupOwner
        OutlinedButton(
            onClick = onProbe,
            modifier = Modifier.fillMaxWidth(),
            enabled = canProbe
        ) {
            Text(stringResource(R.string.wifi_direct_probe_concurrency))
        }
        if (!canProbe) {
            Text(
                text = stringResource(R.string.wifi_direct_probe_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GroupInfoCard(
    group: WifiP2pGroup,
    onCopyPassphrase: (String?) -> Unit
) {
    SectionCard(title = stringResource(R.string.wifi_direct_group_info_title)) {
        InfoRow(label = stringResource(R.string.wifi_direct_ssid), value = group.networkName ?: "-")
        InfoRow(
            label = stringResource(R.string.wifi_direct_passphrase),
            value = group.passphrase ?: "-",
            trailing = {
                IconButton(
                    onClick = { onCopyPassphrase(group.passphrase) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.copy_passphrase),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        )
        InfoRow(
            label = stringResource(R.string.wifi_direct_role),
            value = if (group.isGroupOwner) stringResource(R.string.wifi_direct_group_owner) else stringResource(
                R.string.wifi_direct_group_client
            )
        )
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        trailing?.invoke()
    }
}

@Composable
private fun DevicesCard(
    devices: List<WifiP2pDevice>,
    connectingAddress: String?,
    onConnect: (WifiP2pDevice) -> Unit
) {
    SectionCard(title = stringResource(R.string.wifi_direct_devices, devices.size)) {
        if (devices.isEmpty()) {
            EmptyDeviceState()
        } else {
            devices.forEach { device ->
                DeviceRow(
                    device = device,
                    isConnecting = connectingAddress == device.deviceAddress,
                    onConnect = { onConnect(device) }
                )
            }
        }
    }
}

@Composable
private fun EmptyDeviceState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Devices,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.wifi_direct_no_devices),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.wifi_direct_tap_discover),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DeviceRow(
    device: WifiP2pDevice,
    isConnecting: Boolean,
    onConnect: () -> Unit
) {
    val statusText = when (device.status) {
        WifiP2pDevice.CONNECTED -> stringResource(R.string.device_connected)
        WifiP2pDevice.INVITED -> stringResource(R.string.device_invited)
        WifiP2pDevice.FAILED -> stringResource(R.string.device_failed)
        WifiP2pDevice.AVAILABLE -> stringResource(R.string.device_available)
        WifiP2pDevice.UNAVAILABLE -> stringResource(R.string.device_unavailable)
        else -> stringResource(R.string.device_unknown)
    }
    val isConnectedOrInvited = device.status == WifiP2pDevice.CONNECTED || device.status == WifiP2pDevice.INVITED

    ListItem(
        leadingContent = {
            Icon(
                imageVector = Icons.Default.Smartphone,
                contentDescription = null
            )
        },
        headlineContent = {
            Text(
                text = device.deviceName ?: device.deviceAddress ?: "",
                style = MaterialTheme.typography.bodyLarge
            )
        },
        supportingContent = {
            Text(
                text = "${device.deviceAddress} · $statusText",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            if (isConnecting) {
                LinearProgressIndicator(
                    modifier = Modifier.size(width = 48.dp, height = 4.dp)
                )
            } else {
                OutlinedButton(
                    onClick = onConnect,
                    enabled = !isConnectedOrInvited
                ) {
                    Text(
                        stringResource(
                            if (isConnectedOrInvited) R.string.device_connected
                            else R.string.connect
                        )
                    )
                }
            }
        }
    )
}

@Composable
private fun LogsCard(logs: List<String>) {
    var expanded by remember { mutableStateOf(false) }

    SectionCard(title = stringResource(R.string.wifi_direct_logs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.wifi_direct_logs_count, logs.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = stringResource(
                        if (expanded) R.string.wifi_direct_collapse_logs
                        else R.string.wifi_direct_expand_logs
                    )
                )
            }
        }

        if (!expanded && logs.isNotEmpty()) {
            logs.takeLast(2).forEach { entry ->
                Text(
                    text = entry,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                if (logs.isEmpty()) {
                    Text(
                        text = stringResource(R.string.wifi_direct_no_logs),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    logs.forEach { entry ->
                        Text(
                            text = entry,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
