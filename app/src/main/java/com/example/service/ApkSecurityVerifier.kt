package com.example.service

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.security.MessageDigest

data class ApkVerificationResult(
    val isValid: Boolean,
    val packageName: String,
    val fileSha256: String,
    val signingCertSha256: String,
    val errorMessage: String? = null
)

class ApkSecurityVerifier(private val context: Context) {

    /**
     * Performs strict verification of a downloaded APK package:
     * 1. SHA-256 file checksum validation (if expected hash provided)
     * 2. Package archive validity check
     * 3. Package ID (applicationId) match against installed app
     * 4. Signing certificate SHA-256 match against installed app
     *
     * IF ANY STEP FAILS, the APK is rejected (isValid = false).
     */
    fun verifyDownloadedApk(apkFile: File, expectedFileSha256: String? = null): ApkVerificationResult {
        if (!apkFile.exists() || apkFile.length() == 0L) {
            return ApkVerificationResult(
                isValid = false,
                packageName = "",
                fileSha256 = "",
                signingCertSha256 = "",
                errorMessage = "Downloaded APK file does not exist or is empty."
            )
        }

        // 1. Calculate file SHA-256
        val fileSha256 = calculateFileSha256(apkFile)

        // Step 1: Verify checksum if expected hash was provided
        if (!expectedFileSha256.isNullOrBlank()) {
            val cleanExpected = expectedFileSha256.trim().lowercase()
            if (!fileSha256.equals(cleanExpected, ignoreCase = true)) {
                return ApkVerificationResult(
                    isValid = false,
                    packageName = "",
                    fileSha256 = fileSha256,
                    signingCertSha256 = "",
                    errorMessage = "SHA-256 checksum mismatch. Expected: $cleanExpected, Calculated: $fileSha256. APK rejected."
                )
            }
        }

        // Step 2: Inspect APK Package Archive
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }

        val archiveInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, flags)
            ?: return ApkVerificationResult(
                isValid = false,
                packageName = "",
                fileSha256 = fileSha256,
                signingCertSha256 = "",
                errorMessage = "Android Package Manager could not parse APK archive. File is corrupted or incomplete. APK rejected."
            )

        // Step 3: Verify Package ID (applicationId)
        val targetPackageName = archiveInfo.packageName ?: ""
        if (targetPackageName != context.packageName) {
            return ApkVerificationResult(
                isValid = false,
                packageName = targetPackageName,
                fileSha256 = fileSha256,
                signingCertSha256 = "",
                errorMessage = "Package ID mismatch. Expected '${context.packageName}', but APK has '$targetPackageName'. Update rejected."
            )
        }

        // Step 4: Verify Signing Certificate
        val archiveCertSha256 = extractSigningCertSha256(archiveInfo)
        val installedCertSha256 = getInstalledSigningCertSha256()

        if (archiveCertSha256.isBlank() || installedCertSha256.isBlank() ||
            !archiveCertSha256.equals(installedCertSha256, ignoreCase = true)
        ) {
            return ApkVerificationResult(
                isValid = false,
                packageName = targetPackageName,
                fileSha256 = fileSha256,
                signingCertSha256 = archiveCertSha256,
                errorMessage = "Signing certificate verification failed! The downloaded APK was signed with an untrusted or mismatched certificate. Update rejected."
            )
        }

        // All 4 checks passed
        return ApkVerificationResult(
            isValid = true,
            packageName = targetPackageName,
            fileSha256 = fileSha256,
            signingCertSha256 = archiveCertSha256
        )
    }

    fun calculateFileSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun extractSigningCertSha256(packageInfo: PackageInfo): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val signingInfo = packageInfo.signingInfo
                val signatures = if (signingInfo != null) {
                    if (signingInfo.hasMultipleSigners()) {
                        signingInfo.apkContentsSigners
                    } else {
                        signingInfo.signingCertificateHistory
                    }
                } else null
                val certBytes = signatures?.firstOrNull()?.toByteArray() ?: return ""
                computeSha256Bytes(certBytes)
            } else {
                @Suppress("DEPRECATION")
                val certBytes = packageInfo.signatures?.firstOrNull()?.toByteArray() ?: return ""
                computeSha256Bytes(certBytes)
            }
        } catch (_: Exception) {
            ""
        }
    }

    fun getInstalledSigningCertSha256(): String {
        return try {
            val pm = context.packageManager
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES
            }
            val pkgInfo = pm.getPackageInfo(context.packageName, flags)
            extractSigningCertSha256(pkgInfo)
        } catch (_: Exception) {
            ""
        }
    }

    private fun computeSha256Bytes(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(bytes)
        return hash.joinToString(":") { "%02X".format(it) }
    }
}
