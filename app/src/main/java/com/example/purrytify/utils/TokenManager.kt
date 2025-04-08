package com.example.purrytify.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("token_prefs", Context.MODE_PRIVATE)

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "encrypted_token_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val ACCESS_TOKEN_KEY = "access_token"
        private const val REFRESH_TOKEN_KEY = "refresh_token"
        private const val TOKEN_EXPIRY_KEY = "token_expiry"
    }

    // ==================== Token Operations ====================
    // Method to save tokens
    fun saveTokens(accessToken: String, refreshToken: String) {
        sharedPreferences.edit().apply {
            putString("ACCESS_TOKEN", accessToken)
            putString("REFRESH_TOKEN", refreshToken)
            apply() // or commit() if you want to save synchronously
        }
    }

    // Method to retrieve access token
    fun getAccessToken(): String? {
        return sharedPreferences.getString("ACCESS_TOKEN", null)
    }

    // Method to clear tokens
    fun clearTokens() {
        sharedPreferences.edit().clear().apply() // Clear all preferences
    }

    fun getRefreshToken(): String? {
        return encryptedPrefs.getString(REFRESH_TOKEN_KEY, null)
    }

    // ==================== Token Validation ====================
    fun isTokenValid(): Boolean {
        val expiryTime = encryptedPrefs.getLong(TOKEN_EXPIRY_KEY, 0L)
        return System.currentTimeMillis() < expiryTime
    }

    fun isLoggedIn(): Boolean {
        return !getAccessToken().isNullOrEmpty() && isTokenValid()
    }

    // ==================== Automatic Token Refresh ====================
    suspend fun refreshToken(): Boolean {
        return try {
            val refreshToken = getRefreshToken() ?: return false
            // Implementasi refresh token API call
            true
        } catch (e: Exception) {
            false
        }
    }
}