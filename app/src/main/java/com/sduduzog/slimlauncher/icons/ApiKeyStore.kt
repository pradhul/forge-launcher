package com.sduduzog.slimlauncher.icons

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class ApiKeyStore(context: Context) {

    private val prefs: SharedPreferences = createPrefs(context)

    fun hasKey(): Boolean = !get().isNullOrBlank()

    fun get(): String? = prefs.getString(KEY_API_KEY, null)?.takeIf { it.isNotBlank() }

    fun save(apiKey: String) {
        prefs.edit { putString(KEY_API_KEY, apiKey.trim()) }
    }

    fun clear() {
        prefs.edit { remove(KEY_API_KEY) }
    }

    private fun createPrefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                ENCRYPTED_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Exception) {
            context.getSharedPreferences(FALLBACK_FILE, Context.MODE_PRIVATE)
        }
    }

    companion object {
        private const val ENCRYPTED_FILE = "forge_secrets"
        private const val FALLBACK_FILE = "forge_secrets_fallback"
        private const val KEY_API_KEY = "google_ai_studio_key"
    }
}
