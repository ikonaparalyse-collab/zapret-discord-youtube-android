package com.flowseal.zapret.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowseal.zapret.data.Preset
import com.flowseal.zapret.data.PresetRepository
import com.flowseal.zapret.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    isRunning: Boolean,
    currentPreset: Preset,
    onToggleVpn: () -> Unit,
    onSelectPreset: (Preset) -> Unit
) {
    var showPresetSheet by remember { mutableStateOf(false) }

    // Pulsing animation when VPN is active
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRunning) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val buttonColor by animateColorAsState(
        targetValue = if (isRunning) ActiveGreen else DisconnectedRed,
        label = "buttonColor"
    )

    Scaffold(
        containerColor = BgDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "FLOWSEAL ZAPRET",
                    style = MaterialTheme.typography.headlineLarge,
                    color = NeonCyan,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "DPI Circumvention Engine",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            // Central Power Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(200.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    buttonColor.copy(alpha = 0.25f),
                                    Color.Transparent
                                )
                            )
                        )
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                            .background(SurfaceDark)
                            .border(3.dp, buttonColor, CircleShape)
                            .clickable { onToggleVpn() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Включить/Выключить",
                            tint = buttonColor,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Status Text
                Text(
                    text = if (isRunning) "ЗАЩИТА АКТИВНА" else "ОТКЛЮЧЕНО",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (isRunning) ActiveGreen else DisconnectedRed,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = if (isRunning) "Трафик десинхронизируется" else "Нажмите кнопку для подключения",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            // Preset Selector Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                    .clickable { showPresetSheet = true },
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertangement,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Выбранный пресет",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentPreset.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = currentPreset.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = NeonCyan
                        )
                    }
                    Button(
                        onClick = { showPresetSheet = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple.copy(alpha = 0.2f))
                    ) {
                        Text("Сменить", color = NeonPurple, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet for Presets
    if (showPresetSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPresetSheet = false },
            containerColor = SurfaceDark,
            scrimColor = Color.Black.copy(alpha = 0.6f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Выберите стратегию Flowseal",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(PresetRepository.PRESETS) { preset ->
                        val isSelected = preset.id == currentPreset.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) NeonCyan else CardBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    onSelectPreset(preset)
                                    showPresetSheet = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) NeonCyan.copy(alpha = 0.1f) else BgDark
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = preset.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            color = NeonPurple.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = preset.tag,
                                                color = NeonPurple,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = preset.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Выбран",
                                        tint = NeonCyan
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
