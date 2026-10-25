package com.example.ainetworkguardian

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.IntentCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ainetworkguardian.ui.theme.AINetworkGuardianTheme


class MainActivity : ComponentActivity() {

    private val PacketState = mutableStateOf<List<PacketInfo>>(emptyList())
    private var receiverRegistered = false

    private val vpnDataReciver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            if(intent?.action != "VPN_DATA_BROADCAST")
                return

            val raw = intent.extras?.get("log_message")
            Log.d("NetworkGuardian", "Extras keys: ${intent.extras?.keySet()}")
            Log.d("NetworkGuardian", "Raw object: $raw")

            Log.d(
                "NetworkGuardian",
                "Raw class: ${raw?.javaClass?.name}"
            )
            Log.d(
                "NetworkGuardian",
                "Expected class: ${PacketInfo::class.java.name}"
            )

            val packet: PacketInfo? =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getSerializableExtra(
                        "log_message",
                        PacketInfo::class.java
                    )
                } else {
                    @Suppress("DEPRECATION")
                    intent.getSerializableExtra("log_message") as? PacketInfo
                }


                if (packet != null) {
                    PacketState.value = PacketState.value + packet
                    android.util.Log.d(
                        "NetworkGuardian",
                        "Received packet: ${packet.protocol}, ${packet.sourceIp} -> ${packet.destIp}"
                    )
                } else {
                    android.util.Log.e(
                        "NetworkGuardian",
                        "Broadcast received, but PacketInfo is null or has the wrong type"
                    )
                }

            Log.d("Packet Detail","Pakcet : ${packet}")
        }
    }

    //vpn permission dialogbox
    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){
        result ->
            if(result.resultCode == RESULT_OK)
                StartVpnTunnelService()
            else
                Toast.makeText(this,"Permission Denied", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent{

            var isLiveScanning by remember { mutableStateOf(false) }

            MaterialTheme{
                PacketAnalyzerScreen(
                    packets = PacketState.value,
                    toogle = {disabled ->
                        if(disabled)
                            vpntunnel()
                        else
                            StopVpnTUnnelService()
                    }
                )

            }
        }

    }

    private fun StartVpnTunnelService(){
        val intent = Intent(this, MyVpnService::class.java).apply {
            action = "START_VPN"
        }
        startService(intent);
        Toast.makeText(this, "VPN Engine Started!", Toast.LENGTH_SHORT).show()
    }

    private fun StopVpnTUnnelService() {
        val intent = Intent(this, MyVpnService::class.java).apply {
            action = "STOP_VPN"
        }
        startService(intent)
        Toast.makeText(this,"VPN Engine STOPPED!", Toast.LENGTH_SHORT).show()
    }

    override fun onStart() {
        super.onStart()
        val filter = android.content.IntentFilter("VPN_DATA_BROADCAST")

        if (!receiverRegistered) {
            val filter = android.content.IntentFilter(
                "VPN_DATA_BROADCAST"
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(
                    vpnDataReciver,
                    filter,
                    Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                registerReceiver(vpnDataReciver, filter)
            }

            receiverRegistered = true
        }
    }

    override fun onStop() {
        if (receiverRegistered) {
            unregisterReceiver(vpnDataReciver)
            receiverRegistered = false
        }
        super.onStop()
    }

    private fun vpntunnel () {

        val intent = VpnService.prepare(this);
        if(intent != null)
            vpnPermissionLauncher.launch(intent);
        else
            StartVpnTunnelService()


    }
}

