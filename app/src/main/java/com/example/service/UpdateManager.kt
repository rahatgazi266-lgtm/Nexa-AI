package com.example.service

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.model.AppUpdateInfo
import com.example.model.UpdateState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class UpdateManager(private val context: Context) {
    companion object {
        const val PREFS_NAME = "nexa_update_prefs"
        const val KEY_REPO = "github_repo"
        const val DEFAULT_REPO = "rahatgazi266-lgtm/Nexa-AI"
        const val GITHUB_OWNER = "rahatgazi266-lgtm"
        const val REPOSITORY_NAME = "Nexa-AI"
        const val RELEASES_API_URL = "https://api.github.com/repos/rahatgazi266-lgtm/Nexa-AI/releases/latest"
        const val DEFAULT_APK_DOWNLOAD_URL = "https://github.com/rahatgazi266-lgtm/Nexa-AI/releases/latest/download/Nexa-AI.apk"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val verifier = ApkSecurityVerifier(context)

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    fun getGitHubRepo(): String {
        return prefs.getString(KEY_REPO, DEFAULT_REPO) ?: DEFAULT_REPO
    }

    fun setGitHubRepo(repo: String) {
        prefs.edit().putString(KEY_REPO, repo.trim()).apply()
    }

    fun resetState() {
        _updateState.value = UpdateState.Idle
    }

    suspend fun checkForUpdates(manual: Boolean = false) = withContext(Dispatchers.IO) {
        _updateState.value = UpdateState.Checking
        val repo = getGitHubRepo()
        val url = "https://api.github.com/repos/$repo/releases/latest"

        try {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "NexaAI-Android/${BuildConfig.VERSION_NAME}")
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                // Correctly handle HTTP 404 when no release exists - NEVER falsely report up-to-date!
                if (response.code == 404) {
                    _updateState.value = UpdateState.NoReleaseFound(repo)
                } else if (response.code == 403) {
                    _updateState.value = UpdateState.Error("GitHub API rate limit reached. Please try again later.")
                } else {
                    val err = "GitHub Releases API error (HTTP ${response.code})."
                    _updateState.value = UpdateState.Error(err)
                }
                return@withContext
            }

            val body = response.body?.string() ?: ""
            val json = JSONObject(body)
            val tagName = json.optString("tag_name", "").trim()
            val releaseName = json.optString("name", tagName)
            val changelog = json.optString("body", "Bug fixes and performance improvements.")

            // Look for APK in assets
            val assetsArray = json.optJSONArray("assets") ?: JSONArray()
            var downloadUrl = ""
            var apkSize = 0L

            for (i in 0 until assetsArray.length()) {
                val asset = assetsArray.getJSONObject(i)
                val name = asset.optString("name", "")
                if (name.endsWith(".apk", ignoreCase = true)) {
                    downloadUrl = asset.optString("browser_download_url", "")
                    apkSize = asset.optLong("size", 0L)
                    break
                }
            }

            // Fallback to configured exact direct APK URL if asset list doesn't contain a direct link
            if (downloadUrl.isBlank()) {
                downloadUrl = if (repo.equals(DEFAULT_REPO, ignoreCase = true)) {
                    DEFAULT_APK_DOWNLOAD_URL
                } else {
                    "https://github.com/$repo/releases/latest/download/Nexa-AI.apk"
                }
            }

            // Parse optional SHA-256 hash in changelog if provided
            val sha256Pattern = Pattern.compile("(?:sha-?256|hash):?\\s*([a-fA-F0-9]{64})", Pattern.CASE_INSENSITIVE)
            val matcher = sha256Pattern.matcher(changelog)
            val expectedSha256 = if (matcher.find()) matcher.group(1) else null

            val remoteVersion = tagName.removePrefix("v").removePrefix("V").trim()
            val localVersion = BuildConfig.VERSION_NAME.removePrefix("v").removePrefix("V").trim()

            val isNewer = isVersionNewer(remoteVersion, localVersion)

            if (isNewer && downloadUrl.isNotBlank()) {
                _updateState.value = UpdateState.Available(
                    AppUpdateInfo(
                        versionName = tagName.ifBlank { releaseName },
                        versionCode = parseVersionToCode(remoteVersion),
                        changelog = changelog,
                        downloadUrl = downloadUrl,
                        apkSize = apkSize,
                        expectedSha256 = expectedSha256
                    )
                )
            } else {
                // Only report up-to-date when a release was actually found on GitHub and is <= current version
                _updateState.value = UpdateState.UpToDate(BuildConfig.VERSION_NAME)
            }
        } catch (e: Exception) {
            _updateState.value = UpdateState.Error(
                "Network error while checking updates: ${e.localizedMessage ?: "Connection failed"}"
            )
        }
    }

    suspend fun downloadAndPrepareInstall(updateInfo: AppUpdateInfo) = withContext(Dispatchers.IO) {
        try {
            _updateState.value = UpdateState.Downloading(0, 0L, updateInfo.apkSize)

            val request = Request.Builder()
                .url(updateInfo.downloadUrl)
                .header("User-Agent", "NexaAI-Android/${BuildConfig.VERSION_NAME}")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                _updateState.value = UpdateState.Error("Download failed with HTTP ${response.code}")
                return@withContext
            }

            val body = response.body ?: throw Exception("Empty download response body")
            val totalBytes = if (updateInfo.apkSize > 0) updateInfo.apkSize else body.contentLength()

            val apkFile = File(context.cacheDir, "nexa_update_${System.currentTimeMillis()}.apk")
            if (apkFile.exists()) apkFile.delete()

            body.byteStream().use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    var downloaded = 0L
                    var lastPercent = 0

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloaded += bytesRead

                        if (totalBytes > 0) {
                            val percent = ((downloaded * 100) / totalBytes).toInt().coerceIn(0, 100)
                            if (percent != lastPercent) {
                                lastPercent = percent
                                _updateState.value = UpdateState.Downloading(percent, downloaded, totalBytes)
                            }
                        }
                    }
                    output.flush()
                }
            }

            // Perform strict pre-installation security verification
            val verificationResult = verifier.verifyDownloadedApk(apkFile, updateInfo.expectedSha256)
            if (!verificationResult.isValid) {
                if (apkFile.exists()) apkFile.delete()
                _updateState.value = UpdateState.Error(
                    "Security Verification Failed:\n${verificationResult.errorMessage}"
                )
                return@withContext
            }

            _updateState.value = UpdateState.ReadyToInstall(apkFile, verificationResult)
        } catch (e: Exception) {
            _updateState.value = UpdateState.Error("Error downloading update: ${e.localizedMessage ?: e.message}")
        }
    }

    fun launchPackageInstaller(apkFile: File) {
        try {
            if (!apkFile.exists()) {
                _updateState.value = UpdateState.Error("Update package not found on disk.")
                return
            }

            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, apkFile)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            _updateState.value = UpdateState.Error("Could not launch Package Installer: ${e.localizedMessage}")
        }
    }

    private fun isVersionNewer(remote: String, local: String): Boolean {
        if (remote.isBlank() || local.isBlank()) return false
        val remoteParts = remote.split(".").mapNotNull { it.filter { c -> c.isDigit() }.toIntOrNull() }
        val localParts = local.split(".").mapNotNull { it.filter { c -> c.isDigit() }.toIntOrNull() }

        val length = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until length) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }

    private fun parseVersionToCode(version: String): Long {
        val parts = version.split(".").mapNotNull { it.filter { c -> c.isDigit() }.toLongOrNull() }
        var code = 0L
        for (p in parts) {
            code = code * 100 + p
        }
        return if (code > 0) code else BuildConfig.VERSION_CODE.toLong() + 1
    }
}
