package com.example.purrytify.network

import com.example.purrytify.utils.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator(
    private val tokenManager: TokenManager
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        // Cegah loop tak terbatas, pastikan request belum pernah mencoba refresh
        if (responseCount(response) >= 2) return null

        // Pertama, verifikasi token melalui endpoint verify-token
        val isTokenValidOnServer = runBlocking { tokenManager.isTokenValid() }
        if (isTokenValidOnServer) {
            // Jika token ternyata masih valid di server, gunakan token yang ada
            return response.request.newBuilder()
                .build()
        } else {
            // Jika verifikasi gagal, lakukan refresh token
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