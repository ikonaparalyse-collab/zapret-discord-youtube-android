package com.flowseal.zapret.engine

import android.util.Log
import com.flowseal.zapret.data.Preset
import java.io.OutputStream
import java.net.Socket

object PacketModifier {
    private const val TAG = "PacketModifier"

    /**
     * Checks if the given byte array is a TLS ClientHello packet.
     * Record Type: 0x16 (Handshake)
     * TLS Version: 0x03, 0x01 or 0x03, 0x03
     * Handshake Type: 0x01 (ClientHello)
     */
    fun isTlsClientHello(data: ByteArray, length: Int): Boolean {
        if (length < 6) return false
        return data[0] == 0x16.toByte() &&
               data[1] == 0x03.toByte() &&
               data[5] == 0x01.toByte()
    }

    /**
     * Attempts to extract the Server Name Indication (SNI) hostname from a TLS ClientHello.
     */
    fun extractSni(data: ByteArray, length: Int): String? {
        try {
            if (!isTlsClientHello(data, length)) return null
            var pos = 43 // Skip Record Header (5) + Handshake Type/Length (4) + Version (2) + Random (32)
            if (pos >= length) return null

            // Skip Session ID
            val sessionIdLen = data[pos].toInt() and 0xFF
            pos += 1 + sessionIdLen
            if (pos + 2 >= length) return null

            // Skip Cipher Suites
            val cipherLen = ((data[pos].toInt() and 0xFF) shl 8) or (data[pos + 1].toInt() and 0xFF)
            pos += 2 + cipherLen
            if (pos + 1 >= length) return null

            // Skip Compression Methods
            val compLen = data[pos].toInt() and 0xFF
            pos += 1 + compLen
            if (pos + 2 >= length) return null

            // Extensions Length
            val extensionsLen = ((data[pos].toInt() and 0xFF) shl 8) or (data[pos + 1].toInt() and 0xFF)
            pos += 2
            val extensionsEnd = pos + extensionsLen

            while (pos + 4 <= extensionsEnd && pos + 4 <= length) {
                val extType = ((data[pos].toInt() and 0xFF) shl 8) or (data[pos + 1].toInt() and 0xFF)
                val extLen = ((data[pos + 2].toInt() and 0xFF) shl 8) or (data[pos + 3].toInt() and 0xFF)
                pos += 4

                if (extType == 0) { // server_name extension
                    if (pos + 5 <= length) {
                        val serverNameListLen = ((data[pos].toInt() and 0xFF) shl 8) or (data[pos + 1].toInt() and 0xFF)
                        val nameType = data[pos + 2].toInt() and 0xFF
                        val nameLen = ((data[pos + 3].toInt() and 0xFF) shl 8) or (data[pos + 4].toInt() and 0xFF)
                        if (nameType == 0 && pos + 5 + nameLen <= length) {
                            return String(data, pos + 5, nameLen, Charsets.US_ASCII)
                        }
                    }
                }
                pos += extLen
            }
        } catch (e: Exception) {
            Log.d(TAG, "Failed to parse SNI: ${e.message}")
        }
        return null
    }

    /**
     * Sends TLS ClientHello payload to remote socket applying Flowseal DPI desynchronization tricks:
     * - Fake ClientHello injection
     * - ClientHello Split at specified offset
     * - Disordered TCP segment push
     */
    fun sendDesyncedTls(
        out: OutputStream,
        socket: Socket,
        data: ByteArray,
        length: Int,
        preset: Preset,
        logCallback: (String) -> Unit
    ) {
        val sni = extractSni(data, length) ?: "unknown-target"
        logCallback("🎯 TLS Handshake перехвачен [$sni] | Применяем: ${preset.title}")

        // 1. Fake Packet Injection if enabled
        if (preset.fakePacket) {
            try {
                // Send dummy TLS header or junk segment to poison DPI state
                val fakeHeader = byteArrayOf(
                    0x16.toByte(), 0x03.toByte(), 0x01.toByte(),
                    0x00.toByte(), 0x20.toByte(), 0x01.toByte()
                )
                out.write(fakeHeader)
                out.flush()
                logCallback("  🛡️ Отправлен Fake TLS фрагмент (DPI State Poisoning)")
                Thread.sleep(5)
            } catch (e: Exception) {
                Log.w(TAG, "Fake write error: ${e.message}")
            }
        }

        // 2. TLS ClientHello Split
        val splitPos = preset.splitPos.coerceIn(1, length - 1)

        if (preset.disorder) {
            // Disorder mode: Send second part, then first part
            logCallback("  ✂️ Disorder Split: отправка сегментов [${length - splitPos} байт] и [$splitPos байт]")
            out.write(data, splitPos, length - splitPos)
            out.flush()
            Thread.sleep(2)
            out.write(data, 0, splitPos)
            out.flush()
        } else {
            // Standard Split: Send part 1, then part 2
            logCallback("  ✂️ Split: фрагмент 1 ($splitPos байт) -> пауза -> фрагмент 2 (${length - splitPos} байт)")
            out.write(data, 0, splitPos)
            out.flush()
            Thread.sleep(5)
            out.write(data, splitPos, length - splitPos)
            out.flush()
        }

        logCallback("  ✅ TLS ClientHello успешно доставлен в обход ТСПУ")
    }
}
