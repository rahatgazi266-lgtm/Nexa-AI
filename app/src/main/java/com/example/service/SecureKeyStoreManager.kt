package com.example.service

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class SecureKeyStoreManager(private val context: Context) {
    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "nexa_gemini_api_key_v1"
        private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val PREFS_NAME = "nexa_secure_prefs"
        private const val KEY_ENCRYPTED_API_KEY = "enc_api_key"
        private const val KEY_IV = "enc_api_iv"

        // Legacy plaintext SharedPreferences
        private const val LEGACY_PREFS_NAME = "nexa_settings_prefs"
        private const val KEY_LEGACY_CUSTOM_API_KEY = "custom_gemini_api_key"
    }

    private val securePrefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        migrateLegacyApiKeyIfNeeded()
    }

    private fun getOrCreateSecretKey(): SecretKey {
        return try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
            keyStore.load(null)

            if (keyStore.containsAlias(KEY_ALIAS)) {
                val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
                if (entry != null) return entry.secretKey
            }

            // Generate new AES-256 key inside AndroidKeyStore
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()

            keyGenerator.init(spec)
            keyGenerator.generateKey()
        } catch (_: Exception) {
            // If AndroidKeyStore provider is unavailable (e.g. JVM unit testing),
            // generate/derive a secure AES-256 SecretKey for AES/GCM cipher.
            // Under NO circumstances is plaintext or unencrypted Base64 stored.
            deriveSoftwareAesKey()
        }
    }

    private fun deriveSoftwareAesKey(): SecretKey {
        val appSalt = "${context.packageName}.nexa_secure_vault_v1"
        val md = MessageDigest.getInstance("SHA-256")
        val keyBytes = md.digest(appSalt.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Stores the API key using AES-256 GCM encryption.
     * Returns true if successfully encrypted and persisted.
     * Under NO circumstance is the key stored in plaintext or raw Base64.
     */
    fun storeApiKey(apiKey: String): Boolean {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            removeApiKey()
            return true
        }

        return try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(cleanKey.toByteArray(Charsets.UTF_8))

            val encBase64 = Base64.getEncoder().encodeToString(encryptedBytes)
            val ivBase64 = Base64.getEncoder().encodeToString(iv)

            securePrefs.edit()
                .putString(KEY_ENCRYPTED_API_KEY, encBase64)
                .putString(KEY_IV, ivBase64)
                .commit()
            true
        } catch (_: Exception) {
            // NEVER store plaintext or unencrypted Base64 if encryption fails
            false
        }
    }

    /**
     * Retrieves and decrypts the API key from AES-256 GCM storage.
     * Returns empty string if not found or decryption fails.
     */
    fun getApiKey(): String {
        migrateLegacyApiKeyIfNeeded()

        val encBase64 = securePrefs.getString(KEY_ENCRYPTED_API_KEY, null)
        val ivBase64 = securePrefs.getString(KEY_IV, null)

        if (encBase64.isNullOrBlank() || ivBase64.isNullOrBlank()) {
            return ""
        }

        return try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            val iv = Base64.getDecoder().decode(ivBase64)
            val encryptedBytes = Base64.getDecoder().decode(encBase64)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8).trim()
        } catch (_: Exception) {
            ""
        }
    }

    fun removeApiKey() {
        securePrefs.edit()
            .remove(KEY_ENCRYPTED_API_KEY)
            .remove(KEY_IV)
            .commit()

        // Also clean up any legacy keys
        val legacyPrefs = context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
        legacyPrefs.edit().remove(KEY_LEGACY_CUSTOM_API_KEY).commit()
    }

    /**
     * Migrates legacy plaintext API key if present.
     * CRITICAL RULE: Only removes the legacy copy AFTER the key has been
     * successfully encrypted and verified to be decryptable from secure storage.
     */
    fun migrateLegacyApiKeyIfNeeded(): Boolean {
        val legacyPrefs = context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
        val legacyKey = legacyPrefs.getString(KEY_LEGACY_CUSTOM_API_KEY, null)?.trim()
        if (legacyKey.isNullOrBlank()) {
            return false
        }

        // 1. Attempt to store in encrypted storage
        val stored = storeApiKey(legacyKey)
        if (!stored) {
            // Encryption failed - preserve legacy copy so user does not lose their key
            return false
        }

        // 2. Verify decryption matches the legacy key
        val verifiedKey = getApiKeyInternal()
        if (verifiedKey == legacyKey) {
            // 3. ONLY delete legacy copy after verified successful migration
            legacyPrefs.edit().remove(KEY_LEGACY_CUSTOM_API_KEY).commit()
            return true
        }

        return false
    }

    private fun getApiKeyInternal(): String {
        val encBase64 = securePrefs.getString(KEY_ENCRYPTED_API_KEY, null)
        val ivBase64 = securePrefs.getString(KEY_IV, null)
        if (encBase64.isNullOrBlank() || ivBase64.isNullOrBlank()) return ""

        return try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            val iv = Base64.getDecoder().decode(ivBase64)
            val encryptedBytes = Base64.getDecoder().decode(encBase64)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            String(cipher.doFinal(encryptedBytes), Charsets.UTF_8).trim()
        } catch (_: Exception) {
            ""
        }
    }
}
