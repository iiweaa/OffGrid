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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.offgrid.app.link.location.Location
import com.offgrid.app.link.neighbor.Neighbor

@Composable
fun CallScreen(
    viewModel: CallViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.voiceState.collectAsStateWithLifecycle()

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
                text = "Direct Call",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = if (state.isRunning) "Voice service running" else "Ready",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Peer: ${state.peer?.hostAddress ?: "discovering..."}",
                style = MaterialTheme.typography.bodyMedium
            )

            NeighborListCard(state.neighbors)
            LocationStatusCard(state.myLocation, state.peerLocations.size)

            state.lastError?.let {
                Text(
                    text = "Error: $it",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Button(
                onClick = {
                    if (state.isRunning) {
                        viewModel.stopCall()
                    } else {
                        permissionLauncher.launch(permissions.toTypedArray())
                    }
                }
            ) {
                Text(if (state.isRunning) "End Call" else "Start Call")
            }

            if (state.isRunning) {
                Button(
                    onClick = { viewModel.toggleMute() }
                ) {
                    Icon(
                        imageVector = if (state.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = if (state.isMuted) "Unmute" else "Mute"
                    )
                    Text(
                        text = if (state.isMuted) " Unmute" else " Mute",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
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
                text = "Location",
                style = MaterialTheme.typography.titleMedium
            )
            if (myLocation != null) {
                Text(
                    text = "My: %.5f, %.5f (±%.1fm)".format(
                        myLocation.latitude,
                        myLocation.longitude,
                        myLocation.accuracy
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    text = "Waiting for GPS fix...",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = "Peer locations received: $peerCount",
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
                text = "Neighbors (${neighbors.size})",
                style = MaterialTheme.typography.titleMedium
            )
            if (neighbors.isEmpty()) {
                Text(
                    text = "No neighbors discovered yet",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                neighbors.forEach { neighbor ->
                    val ageSeconds = (System.currentTimeMillis() - neighbor.lastSeenMs) / 1000
                    Text(
                        text = "${neighbor.displayName()} @ ${neighbor.address.hostAddress} " +
                            "(seen ${ageSeconds}s ago)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
