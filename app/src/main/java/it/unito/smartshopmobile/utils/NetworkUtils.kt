package it.unito.smartshopmobile.utils

import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.NetworkInterface

object NetworkUtils {
    private const val TAG = "NetworkUtils"
    private const val BROADCAST_PORT = 45678
    private const val DISCOVERY_TIMEOUT = 5000L // 5 secondi

    /**
     * Rileva automaticamente l'IP del backend tramite UDP broadcast
     * - Emulatore: usa 10.0.2.2
     * - Dispositivo fisico: ascolta i broadcast del server per 5 secondi
     */
    suspend fun detectBackendHost(): String {
        return if (isEmulator()) {
            "10.0.2.2"
        } else {
            // Prova a rilevare il server tramite broadcast UDP
            discoverServer() ?: getDeviceLocalIP() ?: "192.168.1.1"
        }
    }

    /**
     * Ascolta i broadcast UDP dal server per rilevare automaticamente il suo IP
     */
    private suspend fun discoverServer(): String? = withContext(Dispatchers.IO) {
        return@withContext withTimeoutOrNull(DISCOVERY_TIMEOUT) {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket(BROADCAST_PORT)
                socket.reuseAddress = true
                socket.broadcast = true

                val buffer = ByteArray(1024)
                val packet = DatagramPacket(buffer, buffer.size)

                Log.d(TAG, "In ascolto su porta $BROADCAST_PORT per rilevare il server...")

                // Ascolta per un broadcast
                socket.receive(packet)

                val message = String(packet.data, 0, packet.length)
                val json = JSONObject(message)

                if (json.optString("service") == "smartshop-backend") {
                    val host = json.getString("host")
                    val port = json.getInt("port")
                    Log.d(TAG, "✅ Server rilevato: $host:$port")
                    return@withTimeoutOrNull host
                }
            } catch (e: Exception) {
                Log.e(TAG, "Errore durante il discovery del server", e)
            } finally {
                socket?.close()
            }
            null
        }
    }

    /**
     * Ottiene l'IP del dispositivo sulla rete locale (fallback)
     */
    private fun getDeviceLocalIP(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()

                if (!networkInterface.isUp || networkInterface.isLoopback) continue

                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()

                    if (address is Inet4Address && !address.isLoopbackAddress) {
                        val ip = address.hostAddress ?: continue

                        if (ip.startsWith("192.168.") || ip.startsWith("10.")) {
                            return ip
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Errore durante il rilevamento dell'IP del dispositivo", e)
        }
        return null
    }

    private fun isEmulator(): Boolean {
        return (
            Build.FINGERPRINT.startsWith("google/sdk_gphone") ||
                Build.FINGERPRINT.startsWith("generic") ||
                Build.FINGERPRINT.contains("vbox") ||
                Build.FINGERPRINT.contains("test-keys") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK built for x86") ||
                Build.MANUFACTURER.contains("Genymotion") ||
                Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic") ||
                "google_sdk" == Build.PRODUCT
            )
    }
}


