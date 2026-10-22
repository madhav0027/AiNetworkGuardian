package com.example.ainetworkguardian

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

            val packetBuffer = ByteArray(32768)

            while (isRunning && !Thread.currentThread().isInterrupted) {
                val length = inputStream.read(packetBuffer)

                if (length > 0) {
                    val hexString = packetBuffer.take(length).joinToString(" ") { byte ->
                        String.format("%02X", byte)
                    }

                    Log.d("VPN_DEBUG", "Packet Length: $length bytes")
                    Log.d("VPN_DEBUG", "Packet Data (HEX): $hexString")

                    val message = "$hexString"

                    sendBroadcast(
                        Intent("VPN_DATA_BROADCAST").apply {
                            setPackage(packageName)
                            putExtra("log_message", message)
                        }
                    )
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

    private fun parseipv4packet(buffer: ByteBuffer){

        if(buffer.remaining() < 20)
            return

        val firstbyte = buffer.get(0).toInt() and 0xff
        val version = firstbyte shr 4

        val byte2 = buffer.get(2).toInt() and 0xFF
        val byte3 = buffer.get(3).toInt() and 0xFF

        val sizeofpacket = (byte2 shl 8) or byte3


    }

}