package com.example.model

import com.example.service.ApkVerificationResult
import java.io.File

data class AppUpdateInfo(
    val versionName: String,
    val versionCode: Long,
    val changelog: String,
    val downloadUrl: String,
    val apkSize: Long,
    val expectedSha256: String? = null
)

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class Available(val updateInfo: AppUpdateInfo) : UpdateState()
    data class Downloading(
        val progressPercent: Int,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : UpdateState()
    data class ReadyToInstall(
        val apkFile: File,
        val verificationResult: ApkVerificationResult
    ) : UpdateState()
    data class NoReleaseFound(val repository: String) : UpdateState()
    data class Error(val message: String) : UpdateState()
    data class UpToDate(val currentVersion: String) : UpdateState()
}
