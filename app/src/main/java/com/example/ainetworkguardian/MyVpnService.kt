package com.example.ainetworkguardian

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues.TAG
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.nio.Buffer
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.log2
import kotlin.random.Random

class MyVpnService : VpnService() , Runnable {

    private var vpnInterface : ParcelFileDescriptor? = null
    companion object {
        private const val Channel_ID = "VpnService"
    }

    private var vpnThread: Thread? = null
    @Volatile private var isRunning = false

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        when (intent?.action) {
            "STOP_VPN" -> {
                stopvpntunnel()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }

            "START_VPN" -> {
                createNotificationChannel()

                startForeground(1001, buildNotification())

                Toast.makeText(this, "VPN Thread Started!", Toast.LENGTH_SHORT).show()


                if (vpnThread == null || vpnThread?.isAlive != true) {
                    isRunning = true
                    vpnThread = Thread(
                        this,
                        "VpnWorkThread"
                    ).apply { start() }
                }

                return START_NOT_STICKY
            }

            else -> {
                Log.w(TAG, "Unknown VPN action: ${intent?.action}")
                return START_NOT_STICKY
            }
        }
    }
    private fun stopvpntunnel() {
        Log.d(TAG,"Stopping VPN tunnel..")
        isRunning = false

        try {
            vpnInterface?.close() //close file descriptor and restore os routing
            vpnInterface = null

        }catch (e: Exception){
            Log.e(TAG, "Error closing tunnel handle", e)
        }
        vpnThread?.interrupt()
        vpnThread = null
    }
    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, Channel_ID)
            .setContentTitle("Local VPN ACTIVE")
            .setContentText("Initializing VPN tunnel...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Channel_ID,
                "VPN Service",
                NotificationManager.IMPORTANCE_LOW
            )

            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopvpntunnel()
    }



    override fun run() {
        var inputStream: FileInputStream? = null

        try {
            Log.d("VPN_DEBUG", "Building VPN interface")

            val builder = Builder()
                .setSession("LocalVpnSession")
                .addAddress("10.0.0.2", 24)
                .addDnsServer("8.8.8.8")
                .addRoute("0.0.0.0", 0)

            vpnInterface = builder.establish()

            val pfd = vpnInterface
            if (pfd == null) {
                Log.e("VPN_DEBUG", "VPN establish() returned null")
                return
            }

            inputStream = FileInputStream(pfd.fileDescriptor)

            Log.d("VPN_DEBUG", "VPN interface established")
            Log.d("VPN_DEBUG", "Waiting for packets...")

            val packetBuffer = ByteBuffer.allocate(32768)

            while (isRunning && !Thread.currentThread().isInterrupted) {
                val length = inputStream.read(packetBuffer.array())

                if (length > 0) {
                    val packetinfo = parseipv4packet(packetBuffer,length)

                    Log.d("VPN_DEBUG", "Packet Length: $length bytes")

                    val message = """
                        Protocol: ${packetinfo?.protocol},
                        Source IP: ${packetinfo?.sourceIp},
                        Destination IP: ${packetinfo?.destIp},
                        Source Port: ${packetinfo?.sourcePort},
                        Destination Port: ${packetinfo?.destPort},
                        Total Length: ${packetinfo?.totalLength},
                        Payload Length: ${packetinfo?.payloadLength},
                        TCP Flags: ${packetinfo?.tcpFlags},
                        Entropy: ${packetinfo?.entropy}
                    """.trimIndent()

                    sendBroadcast(
                        Intent("VPN_DATA_BROADCAST").apply {
                            setPackage(packageName)
                            putExtra("log_message", packetinfo)
                        }
                    )
                    Log.d("Packetinfo","PACKET INFO : ${packetinfo}")
                }
            }
        } catch (e: InterruptedException) {
            Log.d("VPN_DEBUG", "VPN worker interrupted")
            Thread.currentThread().interrupt()
        } catch (e: Exception) {
            Log.e("VPN_DEBUG", "VPN worker failed", e)
        } finally {
            try {
                inputStream?.close()
            } catch (e: Exception) {
                Log.e("VPN_DEBUG", "Input stream close failed", e)
            }

            try {
                vpnInterface?.close()
            } catch (e: Exception) {
                Log.e("VPN_DEBUG", "VPN interface close failed", e)
            }

            vpnInterface = null
            isRunning = false
            vpnThread = null

            Log.d("VPN_DEBUG", "VPN worker stopped")
        }
    }

    private fun parseipv4packet(buffer: ByteBuffer,length:Int): PacketInfo? {

        if(buffer.remaining() < 40)
            return null

        val firstbyte = buffer.get(0).toInt() and 0xff
        val version = firstbyte shr 4

        val ipHeaderLength = (firstbyte and 0x0F) * 4

        Log.d("IPVersion","$version")

        val byte2 = buffer.get(2).toInt() and 0xFF
        val byte3 = buffer.get(3).toInt() and 0xFF

        val sizeofpacket = (byte2 shl 8) or byte3

        val protocol = buffer.get(9).toInt() and 0xFF

        var sourcePort = 0
        var destPort = 0
        var transportHeaderLength = 0
        var tcpFlags: TcpFlags? = null

        val totalLength = buffer.getShort(2).toInt() and 0xFFFF

        val srcIpBytes = ByteArray(4)
        val dstIpBytes = ByteArray(4)

        buffer.position(12)
        buffer.get(srcIpBytes)
        buffer.get(dstIpBytes)

        val sourceIp = srcIpBytes.joinToString(".") { (it.toInt() and 0xFF).toString() }
        val destIp = dstIpBytes.joinToString(".") { (it.toInt() and 0xFF).toString() }

        if (protocol == 6) { // TCP
            if (length < ipHeaderLength + 20) return null
            buffer.position(ipHeaderLength)

            sourcePort = buffer.short.toInt() and 0xFFFF
            destPort = buffer.short.toInt() and 0xFFFF

            // Data Offset field contains TCP Header Length (in 32-bit words)
            val dataOffsetByte = buffer.get(ipHeaderLength + 12).toInt() and 0xFF
            transportHeaderLength = ((dataOffsetByte shr 4) and 0x0F) * 4

            // Extract 6 control flag bits from offset + 13
            val flagsByte = buffer.get(ipHeaderLength + 13).toInt() and 0xFF
            tcpFlags = TcpFlags(
                isFin = (flagsByte and 0x01) != 0,
                isSyn = (flagsByte and 0x02) != 0,
                isRst = (flagsByte and 0x04) != 0,
                isPsh = (flagsByte and 0x08) != 0,
                isAck = (flagsByte and 0x10) != 0,
                isUrg = (flagsByte and 0x20) != 0
            )
        } else if (protocol == 17) { // UDP
            if (length < ipHeaderLength + 8) return null
            buffer.position(ipHeaderLength)

            sourcePort = buffer.short.toInt() and 0xFFFF
            destPort = buffer.short.toInt() and 0xFFFF
            transportHeaderLength = 8 // Fixed 8-byte UDP header
        }

        Log.d("Size of Packet","$sizeofpacket")

        val ipheaderlength = firstbyte * 4
        val udpstartoffest = ipheaderlength

        val dest_port_byte1 = buffer.get(udpstartoffest + 2).toInt() and 0xFF
        val dest_port_byte2 = buffer.get(udpstartoffest + 3).toInt() and 0xFF

        val port = (dest_port_byte1 shl 8) or dest_port_byte2

        Log.d("PORT ","$port")

        val payloadStart = ipHeaderLength + transportHeaderLength
        val payloadLength = (length - payloadStart).coerceAtLeast(0)

        val entropy = if (payloadLength > 0) {
            val payloadBytes = ByteArray(payloadLength)
            buffer.position(payloadStart)
            buffer.get(payloadBytes)
            calculateShannonEntropy(payloadBytes)
        } else {
            0.0
        }

        val randomInt: Int = Random.nextInt()

        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

        return PacketInfo(
            id = "PKT-${randomInt}",
            timestamp = sdf.format((Date())),
            sourceIp = sourceIp,
            destIp = destIp,
            protocol = protocol.toString(),
            sourcePort = sourcePort,
            destPort = destPort,
            totalLength = totalLength,
            payloadLength = payloadLength,
            tcpFlags = tcpFlags,
            entropy = entropy
        )

    }

    private fun calculateShannonEntropy(data: ByteArray): Double {
        if(data.isEmpty()) return 0.0

        val frequencymap = IntArray(256)
        for (byte in data)
            frequencymap[byte.toInt() and 0xFF]++

        var entropy: Double = 0.0
        val totalBytes = data.size.toDouble()

        for(count in frequencymap){
            if (count > 0){
                val probability = count / totalBytes
                entropy -= probability * log2(probability)

            }
        }
        return entropy
    }

}