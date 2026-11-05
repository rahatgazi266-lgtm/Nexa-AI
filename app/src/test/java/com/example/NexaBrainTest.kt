package com.example

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import com.example.model.ChatMessage
import com.example.model.ChatSession
import com.example.service.ApkSecurityVerifier
import com.example.service.ChatSessionService
import com.example.service.MemoryVaultService
import com.example.service.NexaBrainService
import com.example.service.SecureKeyStoreManager
import com.example.service.SecurityScannerService
import com.example.service.UpdateManager
import com.example.service.UserDataBackupService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class NexaBrainTest {

    // In-memory SharedPreferences implementation for testing real Android service logic on JVM
    private class TestSharedPreferences : SharedPreferences {
        val data = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = HashMap(data)
        override fun getString(key: String, defValue: String?): String? = data[key] as? String ?: defValue
        override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? =
            @Suppress("UNCHECKED_CAST") (data[key] as? MutableSet<String> ?: defValues)
        override fun getInt(key: String, defValue: Int): Int = (data[key] as? Number)?.toInt() ?: defValue
        override fun getLong(key: String, defValue: Long): Long = (data[key] as? Number)?.toLong() ?: defValue
        override fun getFloat(key: String, defValue: Float): Float = (data[key] as? Number)?.toFloat() ?: defValue
        override fun getBoolean(key: String, defValue: Boolean): Boolean = data[key] as? Boolean ?: defValue
        override fun contains(key: String): Boolean = data.containsKey(key)
        override fun edit(): SharedPreferences.Editor = TestEditor(this)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        class TestEditor(private val parent: TestSharedPreferences) : SharedPreferences.Editor {
            private val temp = mutableMapOf<String, Any?>()
            private val toRemove = mutableSetOf<String>()
            private var clearFlag = false

            override fun putString(key: String, value: String?): SharedPreferences.Editor { temp[key] = value; return this }
            override fun putStringSet(key: String, values: MutableSet<String>?): SharedPreferences.Editor { temp[key] = values; return this }
            override fun putInt(key: String, value: Int): SharedPreferences.Editor { temp[key] = value; return this }
            override fun putLong(key: String, value: Long): SharedPreferences.Editor { temp[key] = value; return this }
            override fun putFloat(key: String, value: Float): SharedPreferences.Editor { temp[key] = value; return this }
            override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor { temp[key] = value; return this }
            override fun remove(key: String): SharedPreferences.Editor { toRemove.add(key); return this }
            override fun clear(): SharedPreferences.Editor { clearFlag = true; return this }
            override fun commit(): Boolean { apply(); return true }
            override fun apply() {
                if (clearFlag) parent.data.clear()
                toRemove.forEach { parent.data.remove(it) }
                temp.forEach { (k, v) -> if (v != null) parent.data[k] = v else parent.data.remove(k) }
            }
        }
    }

    private class TestContext(private val tempCacheDir: File) : ContextWrapper(null) {
        val prefStores = mutableMapOf<String, TestSharedPreferences>()

        override fun getApplicationContext(): Context = this
        override fun getPackageName(): String = "com.aistudio.nexaai.hqzjtr"
        override fun getCacheDir(): File = tempCacheDir
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
            return prefStores.getOrPut(name) { TestSharedPreferences() }
        }
    }

    @Test
    fun testCreatorAttribution() {
        val expected = "I was created by MD Rahat Gazi."
        assertEquals("I was created by MD Rahat Gazi.", expected)
        assertEquals("gemini-3.5-flash-lite", NexaBrainService.EXACT_MODEL_ID)
    }

    @Test
    fun testSecureKeyStoreManagerLegacyMigrationAndNoPlaintext() {
        val tempDir = File.createTempFile("nexa_test_cache", "")
        tempDir.delete()
        tempDir.mkdirs()

        try {
            val context = TestContext(tempDir)

            // 1. Setup legacy SharedPreferences containing unencrypted plaintext key
            val legacyPrefs = context.getSharedPreferences("nexa_settings_prefs", Context.MODE_PRIVATE)
            legacyPrefs.edit().putString("custom_gemini_api_key", "AIzaSy_LEGACY_TEST_SECRET_KEY_12345").commit()

            // 2. Instantiate actual SecureKeyStoreManager
            val keyManager = SecureKeyStoreManager(context)

            // 3. Verify key is successfully migrated and decrypted
            val retrievedKey = keyManager.getApiKey()
            assertEquals("AIzaSy_LEGACY_TEST_SECRET_KEY_12345", retrievedKey)

            // 4. Verify legacy copy is erased ONLY after successful migration
            val legacyAfterMigration = legacyPrefs.getString("custom_gemini_api_key", null)
            assertNull("Legacy plaintext key must be deleted after successful migration", legacyAfterMigration)

            // 5. Inspect encrypted preferences - verify AES-GCM ciphertext is stored
            val securePrefs = context.getSharedPreferences("nexa_secure_prefs", Context.MODE_PRIVATE)
            val encValue = securePrefs.getString("enc_api_key", null)
            val ivValue = securePrefs.getString("enc_api_iv", null)
            assertNotNull(encValue)
            assertNotNull(ivValue)

            // CRITICAL SECURITY RULE: Under NO circumstances is plaintext or Base64 of plaintext stored
            assertFalse("Stored ciphertext must not contain plaintext secret", encValue!!.contains("AIzaSy"))
            assertFalse("No unencrypted fallback key must exist", securePrefs.contains("fallback_key"))

            // 6. Test storing new API key
            val newStored = keyManager.storeApiKey("AIzaSy_UPDATED_SECURE_KEY_999")
            assertTrue(newStored)
            assertEquals("AIzaSy_UPDATED_SECURE_KEY_999", keyManager.getApiKey())

            // 7. Test removing API key
            keyManager.removeApiKey()
            assertTrue(keyManager.getApiKey().isEmpty())
            assertFalse(securePrefs.contains("enc_api_key"))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testApkSecurityVerifierChecksumAndMismatchRejection() {
        val tempDir = File.createTempFile("nexa_apk_test", "")
        tempDir.delete()
        tempDir.mkdirs()

        try {
            val context = TestContext(tempDir)
            val verifier = ApkSecurityVerifier(context)

            // Create test APK file with known content
            val testApk = File(tempDir, "sample_update.apk")
            testApk.writeText("Nexa-AI-Security-Verification-Test-Payload-1.4")

            // Test actual SHA-256 calculation
            val calculatedHash = verifier.calculateFileSha256(testApk)
            assertEquals(64, calculatedHash.length)

            // Independently calculate expected SHA-256
            val md = MessageDigest.getInstance("SHA-256")
            val expectedHash = md.digest("Nexa-AI-Security-Verification-Test-Payload-1.4".toByteArray())
                .joinToString("") { "%02x".format(it) }
            assertEquals(expectedHash, calculatedHash)

            // Test checksum mismatch rejection (must return isValid = false)
            val mismatchResult = verifier.verifyDownloadedApk(
                testApk,
                "0000000000000000000000000000000000000000000000000000000000000000"
            )
            assertFalse("Mismatched checksum must be rejected", mismatchResult.isValid)
            assertTrue(mismatchResult.errorMessage?.contains("SHA-256 checksum mismatch") == true)

            // Test empty file rejection
            val emptyFile = File(tempDir, "empty.apk")
            emptyFile.createNewFile()
            val emptyResult = verifier.verifyDownloadedApk(emptyFile)
            assertFalse("Empty APK must be rejected", emptyResult.isValid)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testActualBackupCreationAndRestoration() {
        val tempDir = File.createTempFile("nexa_backup_test", "")
        tempDir.delete()
        tempDir.mkdirs()

        try {
            val context = TestContext(tempDir)
            val sessionService = ChatSessionService(context)
            val memoryService = MemoryVaultService(context)
            val backupService = UserDataBackupService(context)

            // 1. Save real chat session and real memory using actual services
            sessionService.saveSession(
                ChatSession(
                    id = "session_live_1",
                    title = "Nexa AI Architecture Review",
                    messages = listOf(
                        ChatMessage(id = "m1", text = "How is data protected in Nexa AI?", isUser = true),
                        ChatMessage(id = "m2", text = "All API keys are encrypted via Android Keystore.", isUser = false)
                    )
                )
            )

            memoryService.saveMemory("User prefers Bengali and English", "permanent")

            // 2. Perform actual backup via UserDataBackupService
            val backupJson = backupService.createBackupJson()
            assertNotNull(backupJson)
            assertTrue(backupJson.contains("Nexa AI Architecture Review"))
            assertTrue(backupJson.contains("User prefers Bengali and English"))
            assertTrue(backupJson.contains("documented_data_types"))
            assertTrue(backupJson.contains("security_notice"))

            // CRITICAL PRIVACY REQUIREMENT: Plaintext API keys must never appear in backup export
            assertFalse(backupJson.contains("AIzaSy"))
            assertFalse(backupJson.contains("custom_gemini_api_key"))

            // 3. Clear memory service to test actual restore
            memoryService.clearAllMemories()
            assertTrue(memoryService.getAllMemories().isEmpty())

            // 4. Perform actual restore via UserDataBackupService
            val restoreResult = backupService.restoreBackupJson(backupJson)
            assertTrue("Restore must succeed for valid backup", restoreResult)

            // 5. Verify restored data in actual services
            val restoredSessions = sessionService.getAllSessions()
            assertTrue(restoredSessions.any { it.title == "Nexa AI Architecture Review" })

            val restoredMemories = memoryService.getAllMemories()
            assertTrue(restoredMemories.any { it.content == "User prefers Bengali and English" })
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testSecurityScannerThreatDetection() {
        val badExecutable = SecurityScannerService.validateAttachment("payload.exe", 2048)
        assertFalse(badExecutable.safe)
        assertEquals("executable_blocked", badExecutable.threats.first().type)

        val badBat = SecurityScannerService.validateAttachment("script.bat", 1024)
        assertFalse(badBat.safe)

        val badCmd = SecurityScannerService.validateAttachment("command.cmd", 1024)
        assertFalse(badCmd.safe)

        val safeDoc = SecurityScannerService.validateAttachment("architecture_doc.pdf", 1024)
        assertTrue(safeDoc.safe)

        val maliciousUrl = SecurityScannerService.scanUrl("javascript:evilCode()")
        assertFalse(maliciousUrl.safe)

        val safeUrl = SecurityScannerService.scanUrl("https://github.com/rahatgazi266-lgtm/Nexa-AI")
        assertTrue(safeUrl.safe)
    }

    @Test
    fun testUpdateManagerRepositoryAndUrls() {
        assertEquals("rahatgazi266-lgtm/Nexa-AI", UpdateManager.DEFAULT_REPO)
        assertEquals("https://github.com/rahatgazi266-lgtm/Nexa-AI/releases/latest/download/app-release.apk", UpdateManager.DEFAULT_APK_DOWNLOAD_URL)
        assertEquals("https://api.github.com/repos/rahatgazi266-lgtm/Nexa-AI/releases/latest", UpdateManager.RELEASES_API_URL)
    }
}
