package com.example.service

import com.example.model.SecurityScanResult
import com.example.model.SecurityThreat

object SecurityScannerService {
    private val blockedExtensions = listOf(
        ".exe", ".bat", ".cmd", ".sh", ".bash", ".vbs", ".ps1", ".scr", ".msi", ".com"
    )

    private val suspiciousSchemes = listOf(
        "javascript:", "vbscript:", "data:", "file:"
    )

    private val shortenerDomains = listOf(
        "bit.ly", "tinyurl.com", "t.co", "is.gd", "buff.ly", "ow.ly"
    )

    private val promptInjectionPatterns = listOf(
        Regex("ignore (all )?previous instructions", RegexOption.IGNORE_CASE),
        Regex("system override", RegexOption.IGNORE_CASE),
        Regex("you are now DAN", RegexOption.IGNORE_CASE),
        Regex("reveal (your )?system prompt", RegexOption.IGNORE_CASE),
        Regex("bypass security", RegexOption.IGNORE_CASE)
    )

    private val sensitivePatterns = listOf(
        Pair("Credit Card", Regex("\\b(?:\\d{4}[ -]?){3}\\d{4}\\b")),
        Pair("Private Key", Regex("-----BEGIN [A-Z ]*PRIVATE KEY-----")),
        Pair("Plaintext Password", Regex("\\b(?:password|passwd)\\s*[:=]\\s*\\S+", RegexOption.IGNORE_CASE))
    )

    var totalScanned: Int = 0
        private set
    var threatsBlocked: Int = 0
        private set
    val logs = mutableListOf<String>()

    fun scanText(text: String): SecurityScanResult {
        totalScanned++
        val threats = mutableListOf<SecurityThreat>()
        var score = 0

        for (pattern in promptInjectionPatterns) {
            if (pattern.containsMatchIn(text)) {
                threats.add(
                    SecurityThreat(
                        type = "prompt_injection",
                        severity = "high",
                        message = "Instruction override or jailbreak pattern detected."
                    )
                )
                score += 40
            }
        }

        for ((name, regex) in sensitivePatterns) {
            if (regex.containsMatchIn(text)) {
                threats.add(
                    SecurityThreat(
                        type = "sensitive_data",
                        severity = "medium",
                        message = "Sensitive credential pattern ($name) detected."
                    )
                )
                score += 30
            }
        }

        val safe = threats.isEmpty()
        if (!safe) {
            threatsBlocked++
            logs.add(0, "[Blocked] Text scan threat detected at ${System.currentTimeMillis()}")
        } else {
            logs.add(0, "[Clean] Text scanned safe at ${System.currentTimeMillis()}")
        }
        if (logs.size > 30) logs.removeAt(logs.size - 1)

        return SecurityScanResult(
            safe = safe,
            threats = threats,
            riskScore = score.coerceAtMost(100)
        )
    }

    fun scanUrl(url: String): SecurityScanResult {
        totalScanned++
        val threats = mutableListOf<SecurityThreat>()
        val trimmed = url.trim().lowercase()

        for (scheme in suspiciousSchemes) {
            if (trimmed.startsWith(scheme)) {
                threats.add(
                    SecurityThreat(
                        type = "suspicious_scheme",
                        severity = "high",
                        message = "Disallowed URL scheme detected: $scheme"
                    )
                )
            }
        }

        if (Regex("https?://\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}").containsMatchIn(trimmed)) {
            threats.add(
                SecurityThreat(
                    type = "raw_ip_url",
                    severity = "medium",
                    message = "Direct IP address URL detected (commonly linked to phishing)."
                )
            )
        }

        for (domain in shortenerDomains) {
            if (trimmed.contains(domain)) {
                threats.add(
                    SecurityThreat(
                        type = "shortened_url",
                        severity = "low",
                        message = "Shortened URL detected ($domain). Origin destination hidden."
                    )
                )
            }
        }

        val safe = threats.none { it.severity == "high" || it.severity == "critical" }
        if (!safe) {
            threatsBlocked++
            logs.add(0, "[Blocked] Unsafe URL: $url")
        } else {
            logs.add(0, "[Clean] URL scanned: $url")
        }
        if (logs.size > 30) logs.removeAt(logs.size - 1)

        return SecurityScanResult(safe = safe, threats = threats)
    }

    fun validateAttachment(fileName: String, sizeBytes: Long): SecurityScanResult {
        totalScanned++
        val threats = mutableListOf<SecurityThreat>()
        val lower = fileName.lowercase()

        if (blockedExtensions.any { lower.endsWith(it) }) {
            threats.add(
                SecurityThreat(
                    type = "executable_blocked",
                    severity = "critical",
                    message = "Executable or script files are strictly blocked for security."
                )
            )
        }

        if (sizeBytes > 15 * 1024 * 1024) {
            threats.add(
                SecurityThreat(
                    type = "size_limit",
                    severity = "high",
                    message = "File exceeds maximum upload size (15 MB)."
                )
            )
        }

        val safe = threats.isEmpty()
        if (!safe) threatsBlocked++
        return SecurityScanResult(safe = safe, threats = threats)
    }

    fun getDefenseRating(): Int {
        if (totalScanned == 0) return 100
        val ratio = threatsBlocked.toFloat() / totalScanned.toFloat()
        return (100 - (ratio * 30).toInt()).coerceIn(70, 100)
    }
}
