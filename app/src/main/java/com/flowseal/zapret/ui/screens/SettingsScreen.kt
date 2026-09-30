package com.flowseal.zapret.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowseal.zapret.data.SettingsRepository
import com.flowseal.zapret.ui.theme.*

@Composable
fun SettingsScreen(settingsRepo: SettingsRepository) {
    var selectedDns by remember { mutableStateOf(settingsRepo.selectedDns) }
    var customSplit by remember { mutableStateOf(settingsRepo.customSplitPos) }
    var customDisorder by remember { mutableStateOf(settingsRepo.customDisorder) }
    var customFake by remember { mutableStateOf(settingsRepo.customFake) }
    var customTtl by remember { mutableStateOf(settingsRepo.customTtl) }

    val dnsOptions = listOf(
        "1.1.1.1" to "Cloudflare (Быстрый)",
        "8.8.8.8" to "Google Public DNS",
        "9.9.9.9" to "Quad9 (Безопасный)",
        "94.140.14.14" to "AdGuard (Блокировка рекламы)"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Настройки",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        }

        // DNS Selection Section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Dns, contentDescription = null, tint = NeonCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Безопасный DNS", fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    dnsOptions.forEach { (ip, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedDns = ip
                                    settingsRepo.selectedDns = ip
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedDns == ip),
                                onClick = {
                                    selectedDns = ip
                                    settingsRepo.selectedDns = ip
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = NeonCyan)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(label, color = TextPrimary, fontSize = 15.sp)
                                Text(ip, color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Custom Parameters Section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = NeonPurple)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Тонкая настройка десинхронизации", fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    // Split pos slider
                    Text("Позиция разделения (Split pos): $customSplit", color = TextPrimary)
                    Slider(
                        value = customSplit.toFloat(),
                        onValueChange = {
                            customSplit = it.toInt()
                            settingsRepo.customSplitPos = customSplit
                        },
                        valueRange = 1f..10f,
                        steps = 8,
                        colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Fake packet toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Отправлять Fake-пакет", color = TextPrimary)
                        Switch(
                            checked = customFake,
                            onCheckedChange = {
                                customFake = it
                                settingsRepo.customFake = it
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                        )
                    }

                    if (customFake) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Fake Packet TTL: $customTtl", color = TextPrimary)
                        Slider(
                            value = customTtl.toFloat(),
                            onValueChange = {
                                customTtl = it.toInt()
                                settingsRepo.customTtl = customTtl
                            },
                            valueRange = 1f..10f,
                            steps = 8,
                            colors = SliderDefaults.colors(thumbColor = NeonPurple, activeTrackColor = NeonPurple)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Disorder toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Режим Disorder (перемешивание)", color = TextPrimary)
                        Switch(
                            checked = customDisorder,
                            onCheckedChange = {
                                customDisorder = it
                                settingsRepo.customDisorder = it
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                        )
                    }
                }
            }
        }

        // About Section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = TextSecondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("О программе", fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Flowseal Zapret Mobile v1.0.0\nМобильный клиент на базе стратегий Flowseal и bol-van zapret для свободного интернета без root-прав.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}
