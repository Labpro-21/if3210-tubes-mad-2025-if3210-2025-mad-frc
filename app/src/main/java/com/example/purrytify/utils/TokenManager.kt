package com.example.purrytify.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.purrytify.model.RefreshTokenRequest
import com.example.purrytify.network.RetrofitClient

class TokenManager(context: Context) {

    companion object {
        private const val PREFS_FILENAME = "encrypted_token_prefs"
        private const val ACCESS_TOKEN_KEY = "access_token"
        private const val REFRESH_TOKEN_KEY = "refresh_token"
        private const val TOKEN_EXPIRY_KEY = "token_expiry"
        private const val TOKEN_VALIDITY_MILLIS = 5 * 60 * 1000L

        private const val PROACTIVE_THRESHOLD   = 60 * 1000L
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        PREFS_FILENAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveTokens(accessToken: String, refreshToken: String) {
        val expiryTimeMillis = System.currentTimeMillis() + TOKEN_VALIDITY_MILLIS
        encryptedPrefs.edit().apply {
            putString(ACCESS_TOKEN_KEY, accessToken)
            putString(REFRESH_TOKEN_KEY, refreshToken)
            putLong(TOKEN_EXPIRY_KEY, expiryTimeMillis)
            apply()
        }
    }

    fun getAccessToken(): String? = encryptedPrefs.getString(ACCESS_TOKEN_KEY, null)

    fun hasAccessToken(): Boolean = getAccessToken()?.isNotBlank() ?: false

    fun getRefreshToken(): String? = encryptedPrefs.getString(REFRESH_TOKEN_KEY, null)

    fun getTokenExpiry(): Long = encryptedPrefs.getLong(TOKEN_EXPIRY_KEY, 0L)

    fun isTokenValid(): Boolean = System.currentTimeMillis() < getTokenExpiry() && !getAccessToken().isNullOrEmpty()

    fun isLoggedIn(): Boolean = !getAccessToken().isNullOrEmpty() && isTokenValid()

    fun clearTokens() {
        encryptedPrefs.edit().clear().apply()
    }


    suspend fun refreshToken(): Boolean {
        return try {
            val refreshToken = getRefreshToken() ?: return false
            val request = RefreshTokenRequest(refreshToken)
            val apiService = RetrofitClient.create(this)
            val response = apiService.refreshToken(request)
            if (response.isSuccessful) {
                val loginResponse = response.body() ?: return false
                saveTokens(loginResponse.accessToken, loginResponse.refreshToken)
                println("Refresh token berhasil")
                true
            } else {
                println("refreshtoken gagal")
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}