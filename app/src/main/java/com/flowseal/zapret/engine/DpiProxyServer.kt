package com.flowseal.zapret.engine

import android.net.VpnService
import android.util.Log
import com.flowseal.zapret.data.Preset
import kotlinx.coroutines.*
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class DpiProxyServer(
    private val vpnService: VpnService,
    private val port: Int = 10808,
    private val presetProvider: () -> Preset,
    private val logCallback: (String) -> Unit
) {
    companion object {
        private const val TAG = "DpiProxyServer"
    }

    private var serverSocket: ServerSocket? = null
    private val isRunning = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val totalBytesIn = AtomicLong(0)
    val totalBytesOut = AtomicLong(0)

    fun start() {
        if (isRunning.getAndSet(true)) return

        scope.launch {
            try {
                serverSocket = ServerSocket(port, 100, InetAddress.getByName("127.0.0.1"))
                logCallback("🚀 Локальный SOCKS5 Desync прокси запущен на 127.0.0.1:$port")

                while (isRunning.get()) {
                    val clientSocket = serverSocket?.accept() ?: break
                    scope.launch {
                        handleClient(clientSocket)
                    }
                }
            } catch (e: Exception) {
                if (isRunning.get()) {
                    Log.e(TAG, "Proxy server error", e)
                    logCallback("⚠️ Ошибка прокси-сервера: ${e.localizedMessage}")
                }
            }
        }
    }

    fun stop() {
        isRunning.set(false)
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        scope.cancel()
        logCallback("🛑 Прокси-сервер остановлен")
    }

    private suspend fun handleClient(clientSocket: Socket) = withContext(Dispatchers.IO) {
        var remoteSocket: Socket? = null
        try {
            clientSocket.tcpNoDelay = true
            val clientIn = clientSocket.getInputStream()
            val clientOut = clientSocket.getOutputStream()

            // 1. SOCKS5 Method Negotiation
            val version = clientIn.read()
            if (version != 0x05) {
                clientSocket.close()
                return@withContext
            }
            val numMethods = clientIn.read()
            val methods = ByteArray(numMethods)
            clientIn.read(methods)

            // Respond: SOCKS5, No Authentication Required (0x00)
            clientOut.write(byteArrayOf(0x05, 0x00))
            clientOut.flush()

            // 2. SOCKS5 Request
            val reqVer = clientIn.read()
            val cmd = clientIn.read()
            clientIn.read() // RSV
            val atyp = clientIn.read()

            if (reqVer != 0x05 || cmd != 0x01) { // Only CMD CONNECT (0x01)
                clientOut.write(byteArrayOf(0x05, 0x07, 0x00, 0x01, 0, 0, 0, 0, 0, 0))
                clientSocket.close()
                return@withContext
            }

            var destHost = ""
            var destPort = 0

            when (atyp) {
                0x01 -> { // IPv4
                    val ipBytes = ByteArray(4)
                    clientIn.read(ipBytes)
                    destHost = InetAddress.getByAddress(ipBytes).hostAddress ?: ""
                }
                0x03 -> { // Domain name
                    val len = clientIn.read()
                    val domainBytes = ByteArray(len)
                    clientIn.read(domainBytes)
                    destHost = String(domainBytes, Charsets.US_ASCII)
                }
                0x04 -> { // IPv6
                    val ipBytes = ByteArray(16)
                    clientIn.read(ipBytes)
                    destHost = InetAddress.getByAddress(ipBytes).hostAddress ?: ""
                }
                else -> {
                    clientSocket.close()
                    return@withContext
                }
            }

            val portBytes = ByteArray(2)
            clientIn.read(portBytes)
            destPort = ((portBytes[0].toInt() and 0xFF) shl 8) or (portBytes[1].toInt() and 0xFF)

            // Connect to remote target
            remoteSocket = Socket()
            remoteSocket.tcpNoDelay = true
            // Protect socket so its outbound packets bypass the Android VPN tunnel
            vpnService.protect(remoteSocket)
            remoteSocket.connect(java.net.InetSocketAddress(destHost, destPort), 10000)

            val remoteIn = remoteSocket.getInputStream()
            val remoteOut = remoteSocket.getOutputStream()

            // SOCKS5 Connection Established Success response
            clientOut.write(byteArrayOf(0x05, 0x00, 0x00, 0x01, 127, 0, 0, 1, 0x00, 0x00))
            clientOut.flush()

            // 3. Bi-directional forwarding with DPI Desync on ClientHello
            val clientToRemote = launch {
                val buffer = ByteArray(32768)
                var isFirstPacket = true

                while (isActive) {
                    val read = clientIn.read(buffer)
                    if (read <= 0) break

                    totalBytesOut.addAndGet(read.toLong())

                    if (isFirstPacket) {
                        isFirstPacket = false
                        if (PacketModifier.isTlsClientHello(buffer, read)) {
                            PacketModifier.sendDesyncedTls(
                                out = remoteOut,
                                socket = remoteSocket,
                                data = buffer,
                                length = read,
                                preset = presetProvider(),
                                logCallback = logCallback
                            )
                            continue
                        }
                    }

                    remoteOut.write(buffer, 0, read)
                    remoteOut.flush()
                }
            }

            val remoteToClient = launch {
                val buffer = ByteArray(32768)
                while (isActive) {
                    val read = remoteIn.read(buffer)
                    if (read <= 0) break
                    totalBytesIn.addAndGet(read.toLong())
                    clientOut.write(buffer, 0, read)
                    clientOut.flush()
                }
            }

            joinAll(clientToRemote, remoteToClient)
        } catch (_: Exception) {
        } finally {
            try { clientSocket.close() } catch (_: Exception) {}
            try { remoteSocket?.close() } catch (_: Exception) {}
        }
    }
}
