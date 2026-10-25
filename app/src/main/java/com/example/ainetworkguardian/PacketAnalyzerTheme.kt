
package com.example.ainetworkguardian

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Cyberpunk theme

val DarkBackground = Color(0xFF0D1117)
val CardBackground = Color(0xFF161B22)
val BorderColor = Color(0xFF30363D)
val PrimaryNeon = Color(0xFF00E676)
val WarningYellow = Color(0xFFFFD600)
val DangerRed = Color(0xFFFF1744)
val TechBlue = Color(0xFF00E5FF)
val TextMuted = Color(0xFF8B949E)

enum class RiskLevel(val color: Color, val label: String) {
    SAFE(PrimaryNeon, "SAFE"),
    WARNING(WarningYellow, "MEDIUM"),
    CRITICAL(DangerRed, "CRITICAL")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PacketAnalyzerScreen(
    packets: List<PacketInfo>,
    toogle: (Boolean) -> Unit
) {
    var selectedProtocolFilter by remember {
        mutableStateOf("ALL")
    }

    var activePacketDetail by remember {
        mutableStateOf<PacketInfo?>(null)
    }

    var isLiveScanning by remember {
        mutableStateOf(false)
    }

    val filteredPackets = remember(
        packets,
        selectedProtocolFilter
    ) {
        if (selectedProtocolFilter == "ALL") {
            packets
        } else {
            packets.filter {
                it.protocol.equals(
                    selectedProtocolFilter,
                    ignoreCase = true
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = TechBlue
                        )

                        Spacer(Modifier.width(10.dp))

                        Text(
                            text = "NETWORK GUARDIAN",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->

        val infiniteTransition =
            rememberInfiniteTransition(label = "borderLoop")

        val rotationAngle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 2000,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotation"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.height(16.dp))

            // START / STOP scanning button

            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        if (isLiveScanning)
                            PrimaryNeon.copy(alpha = 0.2f)
                        else
                            WarningYellow.copy(alpha = 0.2f)
                    )
                    .border(
                        width = 2.dp,
                        color = if (isLiveScanning)
                            PrimaryNeon
                        else
                            WarningYellow,
                        shape = CircleShape
                    )
                    .drawWithContent {
                        drawContent()

                        if (isLiveScanning) {
                            val strokeWidthPx = 4.dp.toPx()
                            val inset = strokeWidthPx / 2

                            rotate(rotationAngle) {
                                drawArc(
                                    color = PrimaryNeon,
                                    startAngle = 0f,
                                    sweepAngle = 90f,
                                    useCenter = false,
                                    topLeft = Offset(inset, inset),
                                    size = Size(
                                        width = size.width - inset * 2,
                                        height = size.height - inset * 2
                                    ),
                                    style = Stroke(
                                        width = strokeWidthPx,
                                        cap = StrokeCap.Round
                                    )
                                )
                            }
                        }
                    }
                    .clickable {
                        val newState = !isLiveScanning
                        isLiveScanning = newState
                        toogle(newState)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isLiveScanning) "STOP" else "START",
                    color = if (isLiveScanning)
                        PrimaryNeon
                    else
                        WarningYellow,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(16.dp))

            RiskOverviewCard(isScanning = isLiveScanning)

            Spacer(Modifier.height(16.dp))

            // Dynamic packet metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    title = "Packets",
                    value = packets.size.toString(),
                    icon = Icons.Default.Speed,
                    accentColor = TechBlue,
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "Threats",
                    value = packets.count {
                        it.riskLevel == RiskLevel.CRITICAL
                    }.toString(),
                    icon = Icons.Default.Warning,
                    accentColor = DangerRed,
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "Total Bytes",
                    value = packets.sumOf {
                        it.totalLength.toLong()
                    }.toString(),
                    icon = Icons.Default.SwapVert,
                    accentColor = PrimaryNeon,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))

            ProtocolFilterBar(
                selectedFilter = selectedProtocolFilter,
                onFilterSelected = {
                    selectedProtocolFilter = it
                }
            )

            Spacer(Modifier.height(12.dp))


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
                    text = "${filteredPackets.size} packet(s)",
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(Modifier.height(8.dp))

            if (filteredPackets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (packets.isEmpty()) {
                            if (isLiveScanning)
                                "Waiting for real packet data..."
                            else
                                "Start scanning to capture packets"
                        } else {
                            "No packets match this protocol"
                        },
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = filteredPackets,
                        key = { it.id }
                    ) { packet ->
                        PacketItemCard(
                            packet = packet,
                            onClick = {
                                activePacketDetail = packet
                            }
                        )
                    }
                }
            }
        }

        activePacketDetail?.let { packet ->
            PacketDetailDialog(
                packet = packet,
                onDismiss = {
                    activePacketDetail = null
                }
            )
        }
    }
}

@Composable
fun RiskOverviewCard(isScanning: Boolean) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                BorderColor,
                RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "SYSTEM THREAT LEVEL",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = if (isScanning) "MONITORING" else "STANDBY",
                    color = if (isScanning) TechBlue else TextMuted,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isScanning) PrimaryNeon
                                else TextMuted
                            )
                    )

                    Spacer(Modifier.width(6.dp))

                    Text(
                        text = if (isScanning)
                            "Live capture active"
                        else
                            "Capture paused",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = if (isScanning) PrimaryNeon else TextMuted,
                modifier = Modifier.size(44.dp)
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground
        ),
        modifier = modifier.border(
            1.dp,
            BorderColor,
            RoundedCornerShape(10.dp)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = value,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace
            )

            Text(
                text = title,
                color = TextMuted,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun ProtocolFilterBar(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {
    val filters = listOf(
        "ALL", "HTTPS", "DNS", "SSH", "HTTP", "QUIC", "UDP", "TCP"
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters) { filter ->
            val isSelected = filter == selectedFilter

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isSelected)
                            PrimaryNeon.copy(alpha = 0.2f)
                        else
                            CardBackground
                    )
                    .border(
                        1.dp,
                        if (isSelected) PrimaryNeon else BorderColor,
                        RoundedCornerShape(20.dp)
                    )
                    .clickable {
                        onFilterSelected(filter)
                    }
                    .padding(
                        horizontal = 16.dp,
                        vertical = 6.dp
                    )
            ) {
                Text(
                    text = filter,
                    color = if (isSelected)
                        PrimaryNeon
                    else
                        TextMuted,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun PacketItemCard(
    packet: PacketInfo,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                BorderColor,
                RoundedCornerShape(8.dp)
            )
            .clickable {
                onClick()
            }
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(packet.riskLevel.color)
            )

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = packet.protocol,
                        color = TechBlue,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = packet.id,
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(Modifier.height(2.dp))

                Text(
                    text = "${packet.sourceIp} → ${packet.destIp}",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = packet.timestamp,
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }

            Spacer(Modifier.width(8.dp))

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = packet.riskLevel.label,
                    color = packet.riskLevel.color,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text = "${packet.totalLength} B",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun PacketDetailDialog(
    packet: PacketInfo,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = null,
                    tint = packet.riskLevel.color
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = "INSPECT: ${packet.id}",
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailRow("Timestamp", packet.timestamp)
                DetailRow("Source IP", packet.sourceIp)
                DetailRow("Destination IP", packet.destIp)
                DetailRow("Protocol", packet.protocol)
                DetailRow(
                    "Packet Size",
                    "${packet.totalLength} Bytes"
                )
                DetailRow(
                    "Risk Level",
                    packet.riskLevel.label,
                    packet.riskLevel.color
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "RAW PAYLOAD PREVIEW",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkBackground)
                        .border(
                            1.dp,
                            BorderColor,
                            RoundedCornerShape(6.dp)
                        )
                        .padding(10.dp)
                ) {
                    Text(
                        text = packet.payloadPreview,
                        color = PrimaryNeon,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "CLOSE",
                    color = TechBlue,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    )
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = Color.White
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = TextMuted,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )

        Spacer(Modifier.width(12.dp))

        Text(
            text = value,
            color = valueColor,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )
    }
}
