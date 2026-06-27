package com.offgrid.app.ui.screens

import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.offgrid.app.OffGridApplication
import com.offgrid.app.R
import com.offgrid.app.link.ConcurrencyStatus
import com.offgrid.app.link.WifiDirectCapabilityChecker
import com.offgrid.app.link.wifidirect.NetworkConfig
import com.offgrid.app.link.wifidirect.NetworkRole
import com.offgrid.app.power.PowerSavingConfig
import com.offgrid.app.service.VoiceStateHolder
import com.offgrid.app.ui.theme.ThemeMode
import com.offgrid.app.ui.theme.ThemePreference
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val voiceState by VoiceStateHolder.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineLarge
                )

                UserIdentityCard(context)
                AppearanceCard(context)
                ConnectionCard(context)
                PowerSavingCard(context, voiceState.isRunning) {
                    if (voiceState.isRunning) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.power_saving_changed_in_call)
                            )
                        }
                    }
                }
                DeveloperToolsCard(context)
                AboutCard(context)
            }
        }
    }
}

@Composable
private fun UserIdentityCard(context: Context) {
    val app = context.applicationContext as OffGridApplication
    val nodeId = remember { app.localNodeId.toHex() }

    SettingsSection(title = stringResource(R.string.settings_identity)) {
        InfoRow(label = stringResource(R.string.settings_user_id), value = nodeId.uppercase())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppearanceCard(context: Context) {
    val currentMode = remember { ThemePreference.get(context) }
    var expanded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(currentMode) }

    SettingsSection(title = stringResource(R.string.settings_appearance)) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selected.label(),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.settings_theme)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                ThemeMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(mode.label()) },
                        onClick = {
                            selected = mode
                            expanded = false
                            ThemePreference.set(context, mode)
                            (context as? android.app.Activity)?.recreate()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConnectionCard(context: Context) {
    val currentRole = remember { NetworkConfig.getRole(context) }
    var expanded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(currentRole) }

    var groupName by remember { mutableStateOf(NetworkConfig.getGroupName(context)) }
    var passphrase by remember { mutableStateOf(NetworkConfig.getPassphrase(context)) }

    val capability = remember { WifiDirectCapabilityChecker.check(context) }
    val showConcurrencyHint = capability.concurrency == ConcurrencyStatus.UNSUPPORTED

    SettingsSection(title = stringResource(R.string.settings_connection)) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = roleLabel(selected),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.network_role)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                NetworkRole.entries.forEach { role ->
                    DropdownMenuItem(
                        text = { Text(roleLabel(role)) },
                        onClick = {
                            selected = role
                            expanded = false
                            NetworkConfig.setRole(context, role)
                        }
                    )
                }
            }
        }

        if (selected == NetworkRole.GROUP_OWNER) {
            Text(
                text = stringResource(R.string.network_go_config),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 8.dp)
            )
            OutlinedTextField(
                value = groupName,
                onValueChange = {
                    groupName = it
                    NetworkConfig.setGroupName(context, it)
                },
                label = { Text(stringResource(R.string.network_group_name)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = passphrase,
                onValueChange = {
                    passphrase = it
                    NetworkConfig.setPassphrase(context, it)
                },
                label = { Text(stringResource(R.string.network_passphrase)) },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = stringResource(R.string.network_go_config_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (showConcurrencyHint) {
            Text(
                text = stringResource(R.string.network_concurrency_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun PowerSavingCard(
    context: Context,
    isCallRunning: Boolean,
    onToggleDuringCall: () -> Unit
) {
    val initialEnabled = remember { PowerSavingConfig.isEnabled(context) }
    var enabled by remember { mutableStateOf(initialEnabled) }

    SettingsSection(title = stringResource(R.string.settings_power)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.power_saving_mode),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.power_saving_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = {
                    enabled = it
                    PowerSavingConfig.setEnabled(context, it)
                    if (isCallRunning) {
                        onToggleDuringCall()
                    }
                }
            )
        }
    }
}

@Composable
private fun DeveloperToolsCard(context: Context) {
    SettingsSection(title = stringResource(R.string.settings_developer_tools)) {
        OutlinedButton(
            onClick = {
                context.startActivity(
                    android.content.Intent(context, com.offgrid.app.WifiDirectTestActivity::class.java)
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.title_wifi_direct_test))
        }
        OutlinedButton(
            onClick = {
                context.startActivity(
                    android.content.Intent(context, com.offgrid.app.OpusLatencyTestActivity::class.java)
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.title_opus_latency_test))
        }
    }
}

@Composable
private fun AboutCard(context: Context) {
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "-"
        } catch (_: Exception) {
            "-"
        }
    }
    val versionCode = remember {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionCode.toLong()
            }
        } catch (_: Exception) {
            -1L
        }
    }

    SettingsSection(title = stringResource(R.string.settings_about)) {
        InfoRow(label = stringResource(R.string.settings_version), value = "$versionName ($versionCode)")
        InfoRow(label = stringResource(R.string.settings_licenses), value = stringResource(R.string.about_licenses))
        Text(
            text = stringResource(R.string.about_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun roleLabel(role: NetworkRole): String = when (role) {
    NetworkRole.AUTO -> stringResource(R.string.network_role_auto)
    NetworkRole.GROUP_OWNER -> stringResource(R.string.network_role_group_owner)
    NetworkRole.CLIENT -> stringResource(R.string.network_role_client)
}

private fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "System default"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}
