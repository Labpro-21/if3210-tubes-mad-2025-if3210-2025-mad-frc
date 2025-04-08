package com.example.purrytify.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenManager(context: Context) {

    companion object {
        private const val PREFS_FILENAME = "encrypted_token_prefs" // Nama file prefs terenkripsi
        private const val ACCESS_TOKEN_KEY = "access_token"          // Kunci penyimpanan access token
        private const val REFRESH_TOKEN_KEY = "refresh_token"        // Kunci penyimpanan refresh token
        private const val TOKEN_EXPIRY_KEY = "token_expiry"            // Kunci penyimpanan waktu kadaluwarsa token

        // Misal JWT berlaku selama 5 menit (5 * 60 * 1000 milidetik)
        private const val TOKEN_VALIDITY_MILLIS = 5 * 60 * 1000L
    }

    // Buat MasterKey menggunakan Android Keystore dengan algoritma AES256_GCM
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    // Inisialisasi EncryptedSharedPreferences untuk penyimpanan data sensitif dengan enkripsi
    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        PREFS_FILENAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * Menyimpan access token, refresh token, dan token expiry ke storage terenkripsi.
     *
     * Karena API login tidak mengembalikan expiryTime, kita hitung expiry sebagai:
     * currentTime + TOKEN_VALIDITY_MILLIS.
     *
     * @param accessToken Token akses dari API.
     * @param refreshToken Token refresh dari API.
     */
    fun saveTokens(accessToken: String, refreshToken: String) {
        val expiryTimeMillis = System.currentTimeMillis() + TOKEN_VALIDITY_MILLIS
        encryptedPrefs.edit().apply {
            putString(ACCESS_TOKEN_KEY, accessToken)
            putString(REFRESH_TOKEN_KEY, refreshToken)
            putLong(TOKEN_EXPIRY_KEY, expiryTimeMillis)
            apply()
        }
    }

    /**
     * Mengambil access token yang tersimpan.
     *
     * @return Access token atau null jika belum tersimpan.
     */
    fun getAccessToken(): String? {
        return encryptedPrefs.getString(ACCESS_TOKEN_KEY, null)
    }

    /**
     * Mengambil refresh token yang tersimpan.
     *
     * @return Refresh token atau null jika belum tersimpan.
     */
    fun getRefreshToken(): String? {
        return encryptedPrefs.getString(REFRESH_TOKEN_KEY, null)
    }

    /**
     * Mengambil waktu kadaluwarsa token yang tersimpan.
     *
     * @return Waktu kadaluwarsa token dalam milidetik atau 0 jika tidak ada.
     */
    fun getTokenExpiry(): Long {
        return encryptedPrefs.getLong(TOKEN_EXPIRY_KEY, 0L)
    }

    /**
     * Mengecek apakah token masih valid berdasarkan perhitungan waktu kadaluwarsa.
     *
     * @return true jika token belum expired, false jika sudah expired.
     */
    fun isTokenValid(): Boolean {
        return System.currentTimeMillis() < getTokenExpiry()
    }

    /**
     * Mengecek apakah pengguna telah login dengan memastikan access token tersedia dan valid.
     *
     * @return true jika access token tidak null/kosong dan token belum expired.
     */
    fun isLoggedIn(): Boolean {
        return !getAccessToken().isNullOrEmpty() && isTokenValid()
    }

    /**
     * Menghapus seluruh token yang tersimpan.
     */
    fun clearTokens() {
        encryptedPrefs.edit().clear().apply()
    }

    /**
     * Melakukan refresh token secara otomatis.
     *
     * Implementasi API call refresh token disesuaikan dengan endpoint POST /api/refresh-token.
     *
     * Contoh (menggunakan Retrofit):
     *
     * suspend fun refreshToken(): Boolean {
     *     return try {
     *         val refreshToken = getRefreshToken() ?: return false
     *         val response = RetrofitClient.instance.refreshToken(RefreshTokenRequest(refreshToken))
     *         if (response.isSuccessful) {
     *             val newAccessToken = response.body()?.accessToken ?: return false
     *             val newRefreshToken = response.body()?.refreshToken ?: refreshToken
     *             // Set expiry baru dengan perhitungan yang sama
     *             saveTokens(newAccessToken, newRefreshToken)
     *             return true
     *         } else {
     *             false
     *         }
     *     } catch (e: Exception) {
     *         false
     *     }
     * }
     *
     * Karena ini contoh, fungsi ini hanya mengembalikan true.
     */
    suspend fun refreshToken(): Boolean {
        return try {
            val refreshToken = getRefreshToken() ?: return false

            // TODO: Implementasikan API call refresh token di sini (menggunakan Retrofit atau library lainnya)
            // Contoh:
            // val response = RetrofitClient.instance.refreshToken(RefreshTokenRequest(refreshToken))
            // if (response.isSuccessful) {
            //     val newAccessToken = response.body()?.accessToken ?: return false
            //     val newRefreshToken = response.body()?.refreshToken ?: refreshToken
            //     saveTokens(newAccessToken, newRefreshToken)
            //     return true
            // } else {
            //     false
            // }

            // Untuk contoh, mengembalikan true jika tidak terjadi exception.
            true
        } catch (e: Exception) {
            false
        }
    }
}