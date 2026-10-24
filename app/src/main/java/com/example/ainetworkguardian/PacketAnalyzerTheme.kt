package com.example.ainetworkguardian

import android.R.attr.repeatMode
import android.graphics.drawable.Icon
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle.Companion.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

import java.util.Collections.rotate

// CyberPunk THEME

val DarkBackground = Color(0xFF0D1117)
val CardBackground = Color(0xFF161B22)
val BorderColor = Color(0xFF30363D)
val PrimaryNeon = Color(0xFF00E676)   // Cyber Green
val WarningYellow = Color(0xFFFFD600) // Caution
val DangerRed = Color(0xFFFF1744)     // High Risk
val TechBlue = Color(0xFF00E5FF)      // Info / Accent
val TextMuted = Color(0xFF8B949E)

enum class RiskLevel(val color:Color,val label:String) {
    SAFE(PrimaryNeon,"SAFE"),
    WARNING(WarningYellow,"MEDIUM"),
    CRITICAL(DangerRed,"CRITICAL")
}

data class PacketInfo(
    val id:String,
    val timestamp:String,
    val sourceIp:String,
    val destIp:String,
    val protocol: String,
    val lengthBytes:Int,
    val RiskLevel: RiskLevel,
    val PayloadPreview:String
)

//sampledata
val samplePackets = listOf(
    PacketInfo("PKT-1092", "12:04:15.102", "192.168.1.45", "104.21.55.2", "HTTPS", 1420, RiskLevel.SAFE, "45 00 05 dc 1c 3d 40 00 40 06 ..."),
    PacketInfo("PKT-1093", "12:04:15.340", "192.168.1.100", "185.220.101.5", "DNS", 68, RiskLevel.CRITICAL, "Tor exit node query detected. DNS TXT record request."),
    PacketInfo("PKT-1094", "12:04:16.001", "192.168.1.45", "172.217.16.206", "QUIC", 1250, RiskLevel.SAFE, "45 00 04 e8 ad 12 40 00 34 11 ..."),
    PacketInfo("PKT-1095", "12:04:16.892", "10.0.0.12", "192.168.1.1", "SSH", 512, RiskLevel.WARNING, "Port scan activity: repeated failed auth attempts."),
    PacketInfo("PKT-1096", "12:04:17.210", "192.168.1.88", "192.168.1.255", "UDP", 128, RiskLevel.SAFE, "Broadcast discovery packet."),
    PacketInfo("PKT-1097", "12:04:18.055", "192.168.1.105", "45.33.32.156", "HTTP", 2048, RiskLevel.CRITICAL, "Unencrypted sensitive payload POST transmission.")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PacketAnalyzerScreen(networkViewModel: NetworkViewModel) {
    var selectedProtocolFilter by remember { mutableStateOf("ALL") }
    var activePacketDetail by remember { mutableStateOf<PacketInfo?>(null) }
    var isLiveScanning by remember { mutableStateOf(true) }
    val networkViewModel: NetworkViewModel = viewModel()


    val filteredPackets = remember(selectedProtocolFilter) {
        if (selectedProtocolFilter == "ALL") samplePackets
        else samplePackets.filter { it.protocol.equals(selectedProtocolFilter, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = TechBlue)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "NETWORK GUARDIAN",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        val infiniteTransition = rememberInfiniteTransition(label = "borderLoop")
        val rotationAngle by infiniteTransition.animateFloat(
            initialValue  = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotation"
        )
            Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(if (isLiveScanning) PrimaryNeon.copy(alpha = .2f) else WarningYellow.copy(alpha = .2f))
                        .border(
                            width = 2.dp,
                            color = if (isLiveScanning) PrimaryNeon else WarningYellow,
                            shape = CircleShape
                        )
                        .drawWithContent {
                            drawContent()

                            val strokeWidthPx = 4.dp.toPx()

                            val padding = strokeWidthPx / 2

                            if(isLiveScanning) {


                                rotate(degrees = rotationAngle) {
                                    drawArc(
                                        color = if (isLiveScanning) PrimaryNeon else WarningYellow, // Dynamic color fix
                                        startAngle = 0f,
                                        sweepAngle = 90f,
                                        useCenter = false,
                                        // 3. Size configuration using the safe, padded bounds
                                        topLeft = androidx.compose.ui.geometry.Offset(
                                            padding,
                                            padding
                                        ),
                                        size = androidx.compose.ui.geometry.Size(
                                            width = size.width - (padding * 2),
                                            height = size.height - (padding * 2)
                                        ),
                                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                                    )
                                }
                            }
                        }
                        .clickable {
                            if(isLiveScanning){
                                networkViewModel.startMonitoring()
                            }else
                                networkViewModel.stopScanning()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isLiveScanning) "STOP" else "START",
                        color = if (isLiveScanning) PrimaryNeon else WarningYellow,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }


                Spacer(modifier = Modifier.height(16.dp)
                .paddingFromBaseline(10.dp))
            // 1. Status Dashboard Header
            RiskOverviewCard(isScanning = isLiveScanning)

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard("Packets/s", "1,248", Icons.Default.Speed, TechBlue, Modifier.weight(1f))
                MetricCard("Threats", "2 Active", Icons.Default.Warning, DangerRed, Modifier.weight(1f))
                MetricCard("Bandwidth", "14.2 MB/s", Icons.Default.SwapVert, PrimaryNeon, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Protocol Filter Chips
            ProtocolFilterBar(
                selectedFilter = selectedProtocolFilter,
                onFilterSelected = { selectedProtocolFilter = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Packet Stream List Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LIVE PACKET FEED",
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${filteredPackets.size} capture(s)",
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 5. Packets List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filteredPackets) { packet ->
                    PacketItemCard(packet = packet, onClick = { activePacketDetail = packet })
                }
            }
        }

        // Detailed Packet Dialog
        activePacketDetail?.let { packet ->
            PacketDetailDialog(packet = packet, onDismiss = { activePacketDetail = null })
        }
    }
}

@Composable
fun RiskOverviewCard(isScanning: Boolean) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("SYSTEM THREAT LEVEL", color = TextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text("ELEVATED RISK", color = DangerRed, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isScanning) PrimaryNeon else TextMuted)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isScanning) "Interface wlan0 live capture..." else "Capture Paused",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = DangerRed,
                modifier = Modifier.size(44.dp)
            )
        }
    }
}

// --- Component: Quick Metric Card ---
@Composable
fun MetricCard(title: String, value: String, icon: ImageVector, accentColor: Color, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        modifier = modifier.border(1.dp, BorderColor, RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = FontFamily.Monospace)
            Text(text = title, color = TextMuted, fontSize = 10.sp)
        }
    }
}

// --- Component: Filter Bar ---
@Composable
fun ProtocolFilterBar(selectedFilter: String, onFilterSelected: (String) -> Unit) {
    val filters = listOf("ALL", "HTTPS", "DNS", "SSH", "HTTP", "QUIC")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(filters) { filter ->
            val isSelected = filter == selectedFilter
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) PrimaryNeon.copy(alpha = 0.2f) else CardBackground)
                    .border(1.dp, if (isSelected) PrimaryNeon else BorderColor, RoundedCornerShape(20.dp))
                    .clickable { onFilterSelected(filter) }
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = filter,
                    color = if (isSelected) PrimaryNeon else TextMuted,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// --- Component: Packet ListItem ---
@Composable
fun PacketItemCard(packet: PacketInfo, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Risk Level Pillar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(packet.RiskLevel.color)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = packet.protocol,
                        color = TechBlue,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = packet.id, color = TextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${packet.sourceIp} → ${packet.destIp}",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = packet.RiskLevel.label,
                    color = packet.RiskLevel.color,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "${packet.lengthBytes} B", color = TextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

// --- Component: Inspector Modal ---
@Composable
fun PacketDetailDialog(packet: PacketInfo, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.BugReport, contentDescription = null, tint = packet.RiskLevel.color)
                Spacer(modifier = Modifier.width(8.dp))
                Text("INSPECT: ${packet.id}", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 16.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow("Timestamp", packet.timestamp)
                DetailRow("Source IP", packet.sourceIp)
                DetailRow("Destination IP", packet.destIp)
                DetailRow("Protocol", packet.protocol)
                DetailRow("Packet Size", "${packet.lengthBytes} Bytes")
                DetailRow("Risk Level", packet.RiskLevel.label, valueColor = packet.RiskLevel.color)

                Spacer(modifier = Modifier.height(8.dp))
                Text("RAW PAYLOAD PREVIEW", color = TextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkBackground)
                        .border(1.dp, BorderColor, RoundedCornerShape(6.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = packet.PayloadPreview,
                        color = PrimaryNeon,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = TechBlue, fontFamily = FontFamily.Monospace)
            }
        }
    )
}

@Composable
fun DetailRow(label: String, value: String, valueColor: Color = Color.White) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        Text(text = value, color = valueColor, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
    }
}