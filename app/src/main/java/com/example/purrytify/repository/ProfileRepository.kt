package com.example.purrytify.repository

import com.example.purrytify.model.UserProfile
import com.example.purrytify.network.RetrofitClient
import com.example.purrytify.utils.TokenManager
import retrofit2.HttpException
import java.io.IOException

class ProfileRepository(private val tokenManager: TokenManager) {

    // Fungsi fetchUserProfile menggunakan RetrofitClient yang sudah diciptakan dengan TokenAuthenticator, sehingga header Authorization ditambahkan otomatis
    suspend fun fetchUserProfile(): Result<UserProfile> {
        return try {
            // Catatan: interface ApiService harus diupdate agar getUserProfile() tidak lagi membutuhkan parameter header
            val response = RetrofitClient.create(tokenManager).getUserProfile()
            if (response.isSuccessful) {
                response.body()?.let { userProfile ->
                    Result.success(userProfile)
                } ?: Result.failure(Exception("Response body is null"))
            } else {
                Result.failure(Exception("Failed to fetch profile with code ${response.code()}"))
            }
        } catch (e: HttpException) {
            Result.failure(e)
        } catch (e: IOException) {
            Result.failure(e)
        }
    }
}