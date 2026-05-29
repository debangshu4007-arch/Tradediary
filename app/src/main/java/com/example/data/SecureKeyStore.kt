package com.example.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure storage for sensitive secrets (the Groq API key) using
 * AndroidX EncryptedSharedPreferences (AES-256 GCM, hardware-backed master key).
 * Never persists secrets in plain text.
 */
class SecureKeyStore(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "secure_secrets",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getGroqApiKey(): String = prefs.getString(KEY_GROQ, "") ?: ""

    fun setGroqApiKey(value: String) {
        prefs.edit().putString(KEY_GROQ, value).apply()
    }

    fun clearGroqApiKey() {
        prefs.edit().remove(KEY_GROQ).apply()
    }

    companion object {
        private const val KEY_GROQ = "groq_api_key"
    }
}
