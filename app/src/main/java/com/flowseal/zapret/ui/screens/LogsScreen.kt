package com.flowseal.zapret.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowseal.zapret.service.ZapretVpnService
import com.flowseal.zapret.ui.theme.*

@Composable
fun LogsScreen() {
    val logs by ZapretVpnService.logMessages.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Логи десинхронизации",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            Row {
                IconButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Zapret Logs", logs.joinToString("\n"))
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Логи скопированы", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Копировать", tint = NeonCyan)
                }
                IconButton(onClick = { ZapretVpnService.clearLogs() }) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Очистить", tint = DisconnectedRed)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceDark)
                .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            if (logs.isEmpty()) {
                Text(
                    text = "Логов пока нет. Включите защиту и откройте YouTube / Discord...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(logs) { log ->
                        Text(
                            text = log,
                            color = when {
                                log.contains("❌") || log.contains("⚠️") -> DisconnectedRed
                                log.contains("✅") || log.contains("🟢") -> ActiveGreen
                                log.contains("🎯") || log.contains("✂️") -> NeonCyan
                                else -> TextPrimary
                            },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}
