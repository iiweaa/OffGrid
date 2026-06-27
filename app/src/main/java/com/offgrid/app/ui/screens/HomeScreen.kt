package com.offgrid.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.offgrid.app.link.ConcurrencyStatus
import com.offgrid.app.link.WifiDirectCapability
import com.offgrid.app.util.BatteryOptimizationHelper

@Composable
fun HomeScreen(
    capability: WifiDirectCapability,
    onRequestBatteryWhitelist: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "OffGrid",
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                text = "Offline mesh voice communicator",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            CapabilityCard(capability)
            BatteryWhitelistCard(onRequestBatteryWhitelist)

            Text(
                text = "Use the bottom navigation to start a call, " +
                    "view peer compass, or change settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CapabilityCard(capability: WifiDirectCapability) {
    val containerColor = when (capability.concurrency) {
        ConcurrencyStatus.SUPPORTED -> MaterialTheme.colorScheme.primaryContainer
        ConcurrencyStatus.UNSUPPORTED -> MaterialTheme.colorScheme.errorContainer
        ConcurrencyStatus.UNKNOWN -> MaterialTheme.colorScheme.secondaryContainer
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = capability.statusLabel(),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = capability.userMessage(),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun BatteryWhitelistCard(onRequest: () -> Unit) {
    val context = LocalContext.current
    val isWhitelisted = remember {
        BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
    }

    if (isWhitelisted) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "电池优化限制",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "系统电池优化可能在锁屏后切断语音/位置。建议将 OffGrid 加入白名单。",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = onRequest) {
                Text("Request battery whitelist")
            }
        }
    }
}
