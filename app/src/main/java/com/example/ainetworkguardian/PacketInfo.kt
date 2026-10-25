package com.example.ainetworkguardian

import java.io.Serializable

data class PacketInfo(
    val id: String,
    val timestamp: String,
    val sourceIp: String,
    val destIp: String,
    val protocol: String,
    val sourcePort: Int,
    val destPort: Int,
    val totalLength: Int,
    val payloadLength: Int,
    val tcpFlags: TcpFlags?,
    val entropy: Double,
    val riskLevel: RiskLevel = RiskLevel.SAFE,
    val payloadPreview: String = ""
) : Serializable

data class TcpFlags(
    val isFin: Boolean,
    val isSyn: Boolean,
    val isRst: Boolean,
    val isPsh: Boolean,
    val isAck: Boolean,
    val isUrg: Boolean
) : Serializable
