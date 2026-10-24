package com.example.ainetworkguardian

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ainetworkguardian.ui.theme.AINetworkGuardianTheme


class MainActivity : ComponentActivity() {


    private lateinit var myView: TextView

    private val vpnDataReciver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val incomingmessage = intent?.getStringExtra("log_message")

            if (incomingmessage != null) {
                // Force the text append to run on the Main UI Thread
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    myView.append("$incomingmessage\n")
                }
            }
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
            val networkViewModel: NetworkViewModel = viewModel()

            MaterialTheme{
                PacketAnalyzerScreen(
                    networkViewModel = networkViewModel,
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

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(vpnDataReciver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(vpnDataReciver, filter)
        }
    }

    override fun onStop() {
        super.onStop()
        // 3. Always unregister to prevent app background memory leaks
        unregisterReceiver(vpnDataReciver)
    }
    private fun vpntunnel () {

        val intent = VpnService.prepare(this);
        if(intent != null)
            vpnPermissionLauncher.launch(intent);
        else
            StartVpnTunnelService()


    }
}

