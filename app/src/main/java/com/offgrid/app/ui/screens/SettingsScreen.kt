package com.offgrid.app.ui.screens

import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.offgrid.app.OffGridApplication
import com.offgrid.app.R
import com.offgrid.app.ui.theme.ThemeMode
import com.offgrid.app.ui.theme.ThemePreference

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineLarge
            )

            UserIdentityCard(context)
            AppearanceCard(context)
            DeveloperToolsCard(context)
            AboutCard(context)
        }
    }
}

@Composable
private fun UserIdentityCard(context: Context) {
    val app = context.applicationContext as OffGridApplication
    val nodeId = remember { app.localNodeId.toHex() }

    SettingsSection(title = "Identity") {
        InfoRow(label = "User ID", value = nodeId.uppercase())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppearanceCard(context: Context) {
    val currentMode = remember { ThemePreference.get(context) }
    var expanded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(currentMode) }

    SettingsSection(title = "Appearance") {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selected.label(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Theme") },
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

@Composable
private fun DeveloperToolsCard(context: Context) {
    SettingsSection(title = "Developer tools") {
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
            Text("Opus latency test")
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

    SettingsSection(title = "About") {
        InfoRow(label = "Version", value = "$versionName ($versionCode)")
        InfoRow(label = "Licenses", value = "Opus codec by Xiph.Org; Material Design Icons")
        Text(
            text = "OffGrid is an experimental mesh voice communicator. " +
                "It does not require internet or cellular connectivity.",
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

private fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "System default"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}
