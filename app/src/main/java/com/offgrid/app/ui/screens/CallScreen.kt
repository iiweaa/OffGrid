package com.offgrid.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.offgrid.app.R
import com.offgrid.app.link.wifidirect.GroupInfo
import com.offgrid.app.link.wifidirect.NetworkConnectionState
import com.offgrid.app.link.location.Location
import com.offgrid.app.link.neighbor.Neighbor

@Composable
fun CallScreen(
    viewModel: CallViewModel,
    modifier: Modifier = Modifier
) {
    val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()
    val networkState by viewModel.networkState.collectAsStateWithLifecycle()

    val permissions = mutableListOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.NEARBY_WIFI_DEVICES
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val recordAudioGranted = results[Manifest.permission.RECORD_AUDIO] == true
        if (recordAudioGranted) {
            viewModel.startCall()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            Text(
                text = stringResource(R.string.screen_call),
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = if (voiceState.isRunning) stringResource(R.string.call_voice_running) else stringResource(R.string.call_ready),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = stringResource(R.string.call_peer, voiceState.peer?.hostAddress ?: stringResource(R.string.call_discovering)),
                style = MaterialTheme.typography.bodyMedium
            )

            NetworkStatusCard(networkState)
            NeighborListCard(voiceState.neighbors)
            LocationStatusCard(voiceState.myLocation, voiceState.peerLocations.size)

            voiceState.lastError?.let {
                Text(
                    text = stringResource(R.string.call_error, it),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Button(
                onClick = {
                    if (voiceState.isRunning) {
                        viewModel.stopCall()
                    } else {
                        permissionLauncher.launch(permissions.toTypedArray())
                    }
                }
            ) {
                Text(if (voiceState.isRunning) stringResource(R.string.call_end) else stringResource(R.string.call_start))
            }

            if (voiceState.isRunning) {
                Button(
                    onClick = { viewModel.toggleMute() }
                ) {
                    Icon(
                        imageVector = if (voiceState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = if (voiceState.isMuted) stringResource(R.string.call_unmute) else stringResource(R.string.call_mute)
                    )
                    Text(
                        text = if (voiceState.isMuted) stringResource(R.string.call_unmute) else stringResource(R.string.call_mute),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NetworkStatusCard(state: NetworkConnectionState) {
    val (title, color) = when (state) {
        is NetworkConnectionState.Idle -> stringResource(R.string.network_status_idle) to MaterialTheme.colorScheme.onSurfaceVariant
        is NetworkConnectionState.Connecting -> stringResource(R.string.network_status_connecting) to MaterialTheme.colorScheme.secondary
        is NetworkConnectionState.GroupCreated -> stringResource(R.string.network_status_group_created) to MaterialTheme.colorScheme.secondary
        is NetworkConnectionState.Connected -> stringResource(R.string.network_status_connected) to MaterialTheme.colorScheme.primary
        is NetworkConnectionState.Failed -> stringResource(R.string.network_status_failed) to MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.network_status),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = color
            )
            when (state) {
                is NetworkConnectionState.Connecting -> {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is NetworkConnectionState.GroupCreated -> {
                    GroupInfoRows(state.info)
                }
                is NetworkConnectionState.Connected -> {
                    GroupInfoRows(state.info)
                }
                is NetworkConnectionState.Failed -> {
                    Text(
                        text = state.error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = state.hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> { /* no-op */ }
            }
        }
    }
}

@Composable
private fun GroupInfoRows(info: GroupInfo) {
    Text(
        text = stringResource(R.string.network_ssid_label, info.networkName),
        style = MaterialTheme.typography.bodyMedium
    )
    info.passphrase?.let {
        Text(
            text = stringResource(R.string.network_passphrase_label, it),
            style = MaterialTheme.typography.bodyMedium
        )
    }
    if (info.isGroupOwner) {
        Text(
            text = stringResource(R.string.network_clients_label, info.clientCount),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun LocationStatusCard(myLocation: Location?, peerCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.call_location),
                style = MaterialTheme.typography.titleMedium
            )
            if (myLocation != null) {
                Text(
                    text = stringResource(
                        R.string.call_my_location,
                        myLocation.latitude,
                        myLocation.longitude,
                        myLocation.accuracy
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    text = stringResource(R.string.call_waiting_gps),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = stringResource(R.string.call_peer_locations, peerCount),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun NeighborListCard(neighbors: List<Neighbor>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.call_neighbors, neighbors.size),
                style = MaterialTheme.typography.titleMedium
            )
            if (neighbors.isEmpty()) {
                Text(
                    text = stringResource(R.string.call_no_neighbors),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                neighbors.forEach { neighbor ->
                    val ageSeconds = (System.currentTimeMillis() - neighbor.lastSeenMs) / 1000
                    Text(
                        text = stringResource(
                            R.string.call_neighbor_item,
                            neighbor.displayName(),
                            neighbor.address.hostAddress ?: "-",
                            ageSeconds
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
