package com.offgrid.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.offgrid.app.link.CapabilityStateHolder
import com.offgrid.app.link.WifiDirectCapabilityChecker
import com.offgrid.app.ui.screens.CallScreen
import com.offgrid.app.ui.screens.CallViewModel
import com.offgrid.app.ui.screens.HomeScreen
import com.offgrid.app.ui.screens.OnboardingScreen
import com.offgrid.app.ui.screens.PeerScreen
import com.offgrid.app.ui.screens.PeerViewModel
import com.offgrid.app.ui.screens.SettingsScreen
import com.offgrid.app.ui.screens.isOnboardingCompleted
import com.offgrid.app.ui.screens.markOnboardingCompleted
import com.offgrid.app.ui.theme.OffGridTheme
import com.offgrid.app.ui.theme.ThemeMode
import com.offgrid.app.ui.theme.ThemePreference
import com.offgrid.app.util.BatteryOptimizationHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CapabilityStateHolder.update(WifiDirectCapabilityChecker.check(this))
        val themeMode = ThemePreference.get(this)
        setContent {
            OffGridTheme(
                darkTheme = when (themeMode) {
                    ThemeMode.DARK -> true
                    ThemeMode.LIGHT -> false
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                }
            ) {
                OffGridApp()
            }
        }
    }
}

@Composable
fun OffGridApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var onboardingCompleted by remember {
        mutableStateOf(isOnboardingCompleted(context))
    }

    if (!onboardingCompleted) {
        OnboardingScreen(onFinished = {
            markOnboardingCompleted(context)
            onboardingCompleted = true
        })
    } else {
        MainAppContent()
    }
}

@Composable
private fun MainAppContent() {
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                BottomNavItem.entries.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = selectedIndex == index,
                        onClick = { selectedIndex = index }
                    )
                }
            }
        }
    ) { innerPadding ->
        ContentContainer(
            selectedIndex = selectedIndex,
            Modifier.padding(innerPadding)
        )
    }
}

@Composable
private fun ContentContainer(
    selectedIndex: Int,
    modifier: Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val capability by CapabilityStateHolder.capability.collectAsStateWithLifecycle()

    when (BottomNavItem.entries[selectedIndex]) {
        BottomNavItem.HOME -> HomeScreen(
            modifier = modifier,
            capability = capability,
            onRequestBatteryWhitelist = {
                BatteryOptimizationHelper.requestDisable(context)
            }
        )
        BottomNavItem.CALL -> {
            val viewModel: CallViewModel = viewModel()
            CallScreen(viewModel, modifier)
        }
        BottomNavItem.PEERS -> {
            val viewModel: PeerViewModel = viewModel()
            PeerScreen(viewModel, modifier)
        }
        BottomNavItem.SETTINGS -> SettingsScreen(modifier)
    }
}

private enum class BottomNavItem(
    val label: String,
    val icon: ImageVector
) {
    HOME("Home", Icons.Default.Home),
    CALL("Call", Icons.Default.Phone),
    PEERS("Peers", Icons.Default.Explore),
    SETTINGS("Settings", Icons.Default.Settings)
}
