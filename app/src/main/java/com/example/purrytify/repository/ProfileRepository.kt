package com.example.purrytify.repository

import com.example.purrytify.model.UserProfile
import com.example.purrytify.network.RetrofitClient
import retrofit2.HttpException
import java.io.IOException

class ProfileRepository {

    // Fungsi fetchUser Profile mengembalikan Result yang berisi UserProfile jika berhasil,
    // atau Exception jika terjadi error
    suspend fun fetchUserProfile(token: String): Result<UserProfile> {
        return try {
            // Pastikan header "Authorization" diisi dengan format "Bearer {token}"
            val authHeader = "Bearer $token"
            val response = RetrofitClient.apiService.getUserProfile(authHeader)
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
            // IOException terjadi jika terdapat masalah jaringan atau parsing
            Result.failure(e)
        }
    }
}