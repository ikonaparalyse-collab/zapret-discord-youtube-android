package com.flowseal.zapret.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.flowseal.zapret.MainActivity
import com.flowseal.zapret.R
import com.flowseal.zapret.ZapretApp
import com.flowseal.zapret.data.Preset
import com.flowseal.zapret.data.PresetRepository
import com.flowseal.zapret.data.SettingsRepository
import com.flowseal.zapret.engine.DpiProxyServer
import com.flowseal.zapret.engine.TunRouter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

class ZapretVpnService : VpnService() {

    companion object {
        const val ACTION_START = "com.flowseal.zapret.START"
        const val ACTION_STOP = "com.flowseal.zapret.STOP"
        private const val NOTIFICATION_ID = 1001
        private const val TAG = "ZapretVpnService"

        private val _isRunning = MutableStateFlow(false)
        val isRunning = _isRunning.asStateFlow()

        private val _currentPreset = MutableStateFlow<Preset?>(null)
        val currentPreset = _currentPreset.asStateFlow()

        private val _logMessages = MutableStateFlow<List<String>>(emptyList())
        val logMessages = _logMessages.asStateFlow()

        fun addLog(msg: String) {
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val formatted = "[$time] $msg"
            val current = _logMessages.value.toMutableList()
            if (current.size > 200) current.removeAt(0)
            current.add(formatted)
            _logMessages.value = current
            Log.d(TAG, formatted)
        }

        fun clearLogs() {
            _logMessages.value = emptyList()
        }
    }

    private var tunInterface: ParcelFileDescriptor? = null
    private var proxyServer: DpiProxyServer? = null
    private var tunRouter: TunRouter? = null
    private lateinit var settingsRepo: SettingsRepository

    override fun onCreate() {
        super.onCreate()
        settingsRepo = SettingsRepository(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startVpn()
            ACTION_STOP -> stopVpn()
        }
        return START_NOT_STICKY
    }

    private fun startVpn() {
        if (_isRunning.value) return

        val presetId = settingsRepo.selectedPresetId
        val preset = PresetRepository.getById(presetId)
        _currentPreset.value = preset

        addLog("🚀 Запуск Flowseal Zapret [${preset.title}]")

        // 1. Start local SOCKS5 DPI Desync proxy
        proxyServer = DpiProxyServer(
            vpnService = this,
            port = 10808,
            presetProvider = { _currentPreset.value ?: preset },
            logCallback = { addLog(it) }
        ).apply {
            start()
        }

        // 2. Establish TUN interface
        try {
            val builder = Builder()
                .setSession("Flowseal Zapret")
                .addAddress("10.0.0.2", 24)
                .addRoute("0.0.0.0", 0)
                .addDnsServer(settingsRepo.selectedDns)
                .setMtu(1500)

            // Per-app routing if enabled
            if (settingsRepo.isPerAppRouting) {
                val allowedApps = settingsRepo.selectedApps
                var addedCount = 0
                for (pkg in allowedApps) {
                    try {
                        builder.addAllowedApplication(pkg)
                        addedCount++
                    } catch (_: PackageManager.NameNotFoundException) {}
                }
                addLog("📦 Маршрутизация включена для $addedCount приложений")
            }

            tunInterface = builder.establish()
            if (tunInterface == null) {
                addLog("❌ Не удалось создать TUN интерфейс")
                stopVpn()
                return
            }

            // 3. Start TUN Router for packet processing and DNS
            tunRouter = TunRouter(
                vpnService = this,
                tunInterface = tunInterface!!,
                dnsServerIp = settingsRepo.selectedDns,
                logCallback = { addLog(it) }
            ).apply {
                start()
            }

            _isRunning.value = true
            startForeground(NOTIFICATION_ID, buildNotification(preset.title))
            addLog("🟢 Защита успешно активирована! Трафик десинхронизируется.")
        } catch (e: Exception) {
            addLog("❌ Ошибка запуска VPN: ${e.localizedMessage}")
            stopVpn()
        }
    }

    private fun stopVpn() {
        addLog("🛑 Отключение Flowseal Zapret...")
        tunRouter?.stop()
        tunRouter = null

        proxyServer?.stop()
        proxyServer = null

        try {
            tunInterface?.close()
        } catch (_: Exception) {}
        tunInterface = null

        _isRunning.value = false
        _currentPreset.value = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
        addLog("⚪ Защита отключена.")
    }

    private fun buildNotification(presetTitle: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, ZapretVpnService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, ZapretApp.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Flowseal Zapret: Активен")
            .setContentText("Пресет: $presetTitle")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Отключить", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }
}
