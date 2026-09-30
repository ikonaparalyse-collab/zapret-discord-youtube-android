package com.flowseal.zapret

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.flowseal.zapret.data.Preset
import com.flowseal.zapret.data.PresetRepository
import com.flowseal.zapret.data.SettingsRepository
import com.flowseal.zapret.service.ZapretVpnService
import com.flowseal.zapret.ui.screens.LogsScreen
import com.flowseal.zapret.ui.screens.MainScreen
import com.flowseal.zapret.ui.screens.SettingsScreen
import com.flowseal.zapret.ui.theme.*

class MainActivity : ComponentActivity() {

    private lateinit var settingsRepo: SettingsRepository

    private val vpnLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startVpnService()
        } else {
            Toast.makeText(this, "Разрешение на VPN отклонено", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsRepo = SettingsRepository(this)

        setContent {
            FlowsealZapretTheme {
                var selectedTab by remember { mutableIntStateOf(0) }
                val isRunning by ZapretVpnService.isRunning.collectAsState()
                var currentPreset by remember {
                    mutableStateOf(PresetRepository.getById(settingsRepo.selectedPresetId))
                }

                Scaffold(
                    bottomBar = {
                        NavigationBar(
                            containerColor = SurfaceDark,
                            tonalElevation = 8.dp
                        ) {
                            val items = listOf(
                                TabItem("Главная", Icons.Default.Home),
                                TabItem("Логи", Icons.Default.ListAlt),
                                TabItem("Настройки", Icons.Default.Settings)
                            )
                            items.forEachIndexed { index, item ->
                                NavigationBarItem(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    icon = { Icon(item.icon, contentDescription = item.title) },
                                    label = { Text(item.title) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = NeonCyan,
                                        selectedTextColor = NeonCyan,
                                        indicatorColor = NeonCyan.copy(alpha = 0.15f),
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(BgDark)
                    ) {
                        when (selectedTab) {
                            0 -> MainScreen(
                                isRunning = isRunning,
                                currentPreset = currentPreset,
                                onToggleVpn = { toggleVpn() },
                                onSelectPreset = { preset ->
                                    currentPreset = preset
                                    settingsRepo.selectedPresetId = preset.id
                                    if (isRunning) {
                                        // Restart VPN with new preset
                                        stopVpnService()
                                        startVpnService()
                                    }
                                }
                            )
                            1 -> LogsScreen()
                            2 -> SettingsScreen(settingsRepo)
                        }
                    }
                }
            }
        }
    }

    private fun toggleVpn() {
        if (ZapretVpnService.isRunning.value) {
            stopVpnService()
        } else {
            val vpnIntent = VpnService.prepare(this)
            if (vpnIntent != null) {
                vpnLauncher.launch(vpnIntent)
            } else {
                startVpnService()
            }
        }
    }

    private fun startVpnService() {
        val intent = Intent(this, ZapretVpnService::class.java).apply {
            action = ZapretVpnService.ACTION_START
        }
        startService(intent)
    }

    private fun stopVpnService() {
        val intent = Intent(this, ZapretVpnService::class.java).apply {
            action = ZapretVpnService.ACTION_STOP
        }
        startService(intent)
    }
}

data class TabItem(val title: String, val icon: ImageVector)
