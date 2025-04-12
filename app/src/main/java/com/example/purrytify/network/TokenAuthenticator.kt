package com.example.purrytify.network

import android.util.Log
import com.example.purrytify.utils.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import com.example.purrytify.network.RetrofitClient

class TokenAuthenticator(
    private val tokenManager: TokenManager
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        // Cegah loop tak terbatas, pastikan request belum pernah mencoba refresh.
        if (responseCount(response) >= 2) return null

        // Ambil access token yang sedang tersimpan
        val currentToken = tokenManager.getAccessToken() ?: return null

        // Lakukan fetch ke server untuk verifikasi token menggunakan RetrofitClient.verifyApiService
        val verifyResponse = runBlocking {
            try {
                val apiService = RetrofitClient.verifyApiService(currentToken)
                // Panggilan ke endpoint verify-token. Misalnya, ApiService mendefinisikan:
                // @GET("api/verify-token")
                // suspend fun verifyToken(@Header("Authorization") authHeader: String): Response<Unit>
                apiService.verifyToken("Bearer $currentToken")
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        println("Verify Response: $verifyResponse")

        // Jika server mengembalikan status selain 403, token dianggap valid
        if (verifyResponse != null && verifyResponse.code() != 403 && verifyResponse.isSuccessful) {
            Log.d("TokenAuthenticator", "Token valid: ${verifyResponse.code()}")
            return response.request.newBuilder()
                .header("Authorization", "Bearer $currentToken")
                .build()
        } else {
            // Jika token tidak valid, lakukan refresh token
            val tokenRefreshed = runBlocking { tokenManager.refreshToken() }
            return if (tokenRefreshed) {
                val newAccessToken = tokenManager.getAccessToken() ?: return null
                response.request.newBuilder()
                    .header("Authorization", "Bearer $newAccessToken")
                    .build()
            } else {
                null
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var priorResponse = response.priorResponse
        while (priorResponse != null) {
            count++
            priorResponse = priorResponse.priorResponse
        }
        return count
    }
}