package com.dumbify.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.dumbify.R
import com.dumbify.repository.AppConfigRepository
import com.dumbify.ui.MainActivity
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

class DnsFilterService : VpnService() {
    
    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var repository: AppConfigRepository
    @Volatile private var isRunning = false
    
    companion object {
        private const val VPN_ADDRESS = "10.0.0.2"
        private const val VPN_ROUTE = "0.0.0.0"
        private const val VPN_DNS = "1.1.1.1" // Cloudflare DNS as upstream
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "dns_filter_channel"
    }
    
    override fun onCreate() {
        super.onCreate()
        repository = AppConfigRepository(this)
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            stopVpn()
            return START_NOT_STICKY
        }
        
        if (!isRunning) {
            startVpn()
        }
        
        return START_STICKY
    }
    
    private fun startVpn() {
        val builder = Builder()
            .addAddress(VPN_ADDRESS, 32)
            .addRoute(VPN_ROUTE, 0)
            .addDnsServer(VPN_DNS)
            .setSession("Dumbify DNS Filter")
            .setConfigureIntent(createConfigIntent())
        
        try {
            vpnInterface = builder.establish()
            isRunning = true
            
            startForeground(NOTIFICATION_ID, createNotification())
            
            // Start packet filtering
            serviceScope.launch {
                handlePackets()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }
    
    private fun stopVpn() {
        isRunning = false
        vpnInterface?.close()
        vpnInterface = null
        serviceScope.cancel()
        stopForeground(true)
        stopSelf()
    }
    
    private suspend fun handlePackets() {
        val vpn = vpnInterface ?: return
        
        val buffer = ByteBuffer.allocate(32767)
        val blockedDomains = repository.getBlockedDomains()
            .filter { it.enabled }
            .map { it.domain.lowercase() }
            .toSet()
        
        withContext(Dispatchers.IO) {
            try {
                FileInputStream(vpn.fileDescriptor).use { inputStream ->
                    FileOutputStream(vpn.fileDescriptor).use { outputStream ->
                        while (isActive && isRunning) {
                            buffer.clear()
                            val length = inputStream.read(buffer.array())
                            
                            if (length > 0) {
                                buffer.limit(length)
                                
                                // Parse DNS query
                                val packet = buffer.array().copyOf(length)
                                val isDnsQuery = isDnsPacket(packet)
                                
                                if (isDnsQuery) {
                                    val domain = extractDomain(packet)
                                    
                                    if (domain != null && isBlocked(domain, blockedDomains)) {
                                        // Send blocked response
                                        val blockedResponse = createBlockedDnsResponse(packet)
                                        outputStream.write(blockedResponse)
                                        continue
                                    }
                                }
                                
                                // Forward legitimate packets
                                forwardPacket(packet, outputStream)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun isDnsPacket(packet: ByteArray): Boolean {
        if (packet.size < 28) return false
        
        // Check if it's UDP packet
        val protocol = packet[9].toInt() and 0xFF
        if (protocol != 17) return false // 17 = UDP
        
        // Check destination port (DNS uses port 53)
        val destPort = ((packet[22].toInt() and 0xFF) shl 8) or (packet[23].toInt() and 0xFF)
        return destPort == 53
    }
    
    private fun extractDomain(packet: ByteArray): String? {
        try {
            if (packet.size < 40) return null
            
            // DNS query starts at byte 28 for UDP/IP header
            var pos = 28
            val domain = StringBuilder()
            
            while (pos < packet.size) {
                val labelLength = packet[pos].toInt() and 0xFF
                if (labelLength == 0) break
                
                pos++
                if (pos + labelLength > packet.size) break
                
                if (domain.isNotEmpty()) domain.append(".")
                domain.append(String(packet, pos, labelLength, Charsets.US_ASCII))
                pos += labelLength
            }
            
            return domain.toString().lowercase()
        } catch (e: Exception) {
            return null
        }
    }
    
    private fun isBlocked(domain: String, blockedDomains: Set<String>): Boolean {
        return blockedDomains.any { blocked ->
            domain == blocked || domain.endsWith(".$blocked")
        }
    }
    
    private fun createBlockedDnsResponse(queryPacket: ByteArray): ByteArray {
        // Create a DNS response with NXDOMAIN (non-existent domain)
        val response = queryPacket.copyOf()
        
        // Set response flags (QR=1, RCODE=3 for NXDOMAIN)
        if (response.size > 30) {
            response[30] = (response[30].toInt() or 0x80).toByte() // QR flag
            response[31] = (response[31].toInt() or 0x03).toByte() // RCODE = 3 (NXDOMAIN)
        }
        
        return response
    }
    
    private fun forwardPacket(packet: ByteArray, outputStream: FileOutputStream) {
        try {
            // Simple pass-through for now
            // In a production app, you'd forward to actual DNS server
            outputStream.write(packet)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun createNotification(): Notification {
        val stopIntent = Intent(this, DnsFilterService::class.java).apply {
            action = "STOP"
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.dns_filter_active))
            .setContentText(getString(R.string.dns_filter_description))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_notification, "Stop", stopPendingIntent)
            .setOngoing(true)
            .build()
    }
    
    private fun createConfigIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "DNS Filter",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Blocks access to unproductive websites"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopVpn()
    }
}
