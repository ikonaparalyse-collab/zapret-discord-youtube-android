package com.flowseal.zapret.engine

import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

class TunRouter(
    private val vpnService: VpnService,
    private val tunInterface: ParcelFileDescriptor,
    private val dnsServerIp: String,
    private val logCallback: (String) -> Unit
) {
    companion object {
        private const val TAG = "TunRouter"
        private const val BUFFER_SIZE = 32768
    }

    private val isRunning = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var dnsSocket: DatagramSocket? = null

    fun start() {
        if (isRunning.getAndSet(true)) return

        scope.launch {
            try {
                dnsSocket = DatagramSocket().apply {
                    vpnService.protect(this)
                }

                val inStream = FileInputStream(tunInterface.fileDescriptor)
                val outStream = FileOutputStream(tunInterface.fileDescriptor)
                val buffer = ByteBuffer.allocate(BUFFER_SIZE)

                logCallback("📡 TUN Маршрутизатор запущен. DNS настроен на $dnsServerIp")

                while (isRunning.get()) {
                    val length = inStream.read(buffer.array())
                    if (length > 0) {
                        buffer.limit(length)
                        processPacket(buffer, outStream)
                        buffer.clear()
                    }
                }
            } catch (e: Exception) {
                if (isRunning.get()) {
                    Log.e(TAG, "TUN Router error", e)
                }
            }
        }
    }

    fun stop() {
        isRunning.set(false)
        try { dnsSocket?.close() } catch (_: Exception) {}
        try { tunInterface.close() } catch (_: Exception) {}
        scope.cancel()
    }

    private fun processPacket(packet: ByteBuffer, outStream: FileOutputStream) {
        if (packet.remaining() < 20) return
        val versionAndIHL = packet.get(0).toInt() and 0xFF
        val version = versionAndIHL shr 4
        if (version != 4) return // IPv4 only for now

        val ihl = (versionAndIHL and 0x0F) * 4
        val protocol = packet.get(9).toInt() and 0xFF

        if (protocol == 17) { // UDP
            val srcPort = ((packet.get(ihl).toInt() and 0xFF) shl 8) or (packet.get(ihl + 1).toInt() and 0xFF)
            val destPort = ((packet.get(ihl + 2).toInt() and 0xFF) shl 8) or (packet.get(ihl + 3).toInt() and 0xFF)

            if (destPort == 53) { // DNS Query
                forwardDns(packet, ihl, outStream)
            }
        }
    }

    private fun forwardDns(packet: ByteBuffer, ipHeaderLen: Int, outStream: FileOutputStream) {
        scope.launch {
            try {
                val udpHeaderLen = 8
                val totalHeaderLen = ipHeaderLen + udpHeaderLen
                val payloadLen = packet.limit() - totalHeaderLen
                if (payloadLen <= 0) return@launch

                val dnsQuery = ByteArray(payloadLen)
                System.arraycopy(packet.array(), totalHeaderLen, dnsQuery, 0, payloadLen)

                val dnsTarget = InetAddress.getByName(dnsServerIp)
                val sendPacket = DatagramPacket(dnsQuery, dnsQuery.size, dnsTarget, 53)
                dnsSocket?.send(sendPacket)

                val responseBuffer = ByteArray(4096)
                val recvPacket = DatagramPacket(responseBuffer, responseBuffer.size)
                dnsSocket?.soTimeout = 3000
                dnsSocket?.receive(recvPacket)

                // Swap IPs and Ports for DNS response
                val srcIp = ByteArray(4)
                val dstIp = ByteArray(4)
                System.arraycopy(packet.array(), 12, srcIp, 0, 4)
                System.arraycopy(packet.array(), 16, dstIp, 0, 4)

                val srcPort = ((packet.get(ipHeaderLen).toInt() and 0xFF) shl 8) or (packet.get(ipHeaderLen + 1).toInt() and 0xFF)
                val dstPort = ((packet.get(ipHeaderLen + 2).toInt() and 0xFF) shl 8) or (packet.get(ipHeaderLen + 3).toInt() and 0xFF)

                val respIpPacket = buildUdpIpPacket(
                    srcIp = dstIp,
                    dstIp = srcIp,
                    srcPort = dstPort,
                    dstPort = srcPort,
                    payload = recvPacket.data,
                    payloadLen = recvPacket.length
                )

                synchronized(outStream) {
                    outStream.write(respIpPacket)
                    outStream.flush()
                }
            } catch (_: Exception) {}
        }
    }

    private fun buildUdpIpPacket(
        srcIp: ByteArray,
        dstIp: ByteArray,
        srcPort: Int,
        dstPort: Int,
        payload: ByteArray,
        payloadLen: Int
    ): ByteArray {
        val totalLen = 20 + 8 + payloadLen
        val packet = ByteArray(totalLen)

        // IPv4 Header
        packet[0] = 0x45.toByte() // IPv4, Header length 20
        packet[1] = 0x00.toByte() // DSCP/ECN
        packet[2] = ((totalLen shr 8) and 0xFF).toByte()
        packet[3] = (totalLen and 0xFF).toByte()
        packet[4] = 0x00.toByte(); packet[5] = 0x01.toByte() // Identification
        packet[6] = 0x40.toByte(); packet[7] = 0x00.toByte() // Flags: Don't Fragment
        packet[8] = 64.toByte()   // TTL
        packet[9] = 17.toByte()   // Protocol: UDP
        // Checksum at 10,11 (calculated below)
        System.arraycopy(srcIp, 0, packet, 12, 4)
        System.arraycopy(dstIp, 0, packet, 16, 4)

        // Calculate IP checksum
        var ipChecksum = 0
        for (i in 0 until 20 step 2) {
            if (i == 10) continue
            val word = ((packet[i].toInt() and 0xFF) shl 8) or (packet[i + 1].toInt() and 0xFF)
            ipChecksum += word
        }
        while (ipChecksum shr 16 > 0) {
            ipChecksum = (ipChecksum and 0xFFFF) + (ipChecksum shr 16)
        }
        ipChecksum = ipChecksum.inv() and 0xFFFF
        packet[10] = ((ipChecksum shr 8) and 0xFF).toByte()
        packet[11] = (ipChecksum and 0xFF).toByte()

        // UDP Header
        val udpLen = 8 + payloadLen
        packet[20] = ((srcPort shr 8) and 0xFF).toByte()
        packet[21] = (srcPort and 0xFF).toByte()
        packet[22] = ((dstPort shr 8) and 0xFF).toByte()
        packet[23] = (dstPort and 0xFF).toByte()
        packet[24] = ((udpLen shr 8) and 0xFF).toByte()
        packet[25] = (udpLen and 0xFF).toByte()
        packet[26] = 0x00.toByte(); packet[27] = 0x00.toByte() // Optional UDP checksum

        // Payload
        System.arraycopy(payload, 0, packet, 28, payloadLen)

        return packet
    }
}
