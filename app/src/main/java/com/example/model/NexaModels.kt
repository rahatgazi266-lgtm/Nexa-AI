package com.example.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isTyping: Boolean = false,
    val attachmentName: String? = null,
    val liked: Boolean? = null
) {
    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
}

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val date: String = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(Date()),
    val messages: List<ChatMessage>
)

data class MemoryItem(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val type: String, // "permanent" or "temporary"
    val timestamp: Long = System.currentTimeMillis()
)

data class SecurityThreat(
    val type: String,
    val severity: String,
    val message: String
)

data class SecurityScanResult(
    val safe: Boolean,
    val threats: List<SecurityThreat>,
    val riskScore: Int = 0
)

data class SystemStatusReport(
    val coreName: String = "Nexa Ocean Core",
    val version: String = "2.0.0",
    val healthPercent: Int = 100,
    val uptimeSeconds: Long,
    val isOnline: Boolean = true,
    val services: List<ServiceHealth>
)

data class ServiceHealth(
    val name: String,
    val isHealthy: Boolean = true
)
