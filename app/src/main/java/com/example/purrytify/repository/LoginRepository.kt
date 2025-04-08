package com.example.purrytify.repository

import com.example.purrytify.model.LoginRequest
import com.example.purrytify.model.LoginResponse
import com.example.purrytify.network.RetrofitClient
import retrofit2.HttpException
import java.io.IOException

class LoginRepository {

    // Fungsi login mengembalikan Result yang berisi LoginResponse jika berhasil,
    // atau Exception jika terjadi error.
    suspend fun login(email: String, password: String): Result<LoginResponse> {
        return try {
            val request = LoginRequest(email, password)
            val response = RetrofitClient.apiService.login(request)
            if (response.isSuccessful) {
                // Jika response body tidak null, kembalikan hasil sukses, jika null, kembalikan error.
                response.body()?.let { loginResponse ->
                    Result.success(loginResponse)
                } ?: Result.failure(Exception("Response body is null"))
            } else {
                // Sertakan errorBody() dalam pesan error untuk informasi lebih lengkap.
                val errorMessage = response.errorBody()?.string() ?: "Unknown error"
                Result.failure(Exception("Login failed with code ${response.code()}: $errorMessage"))
            }
        } catch (e: HttpException) {
            Result.failure(e)
        } catch (e: IOException) {
            // IOException terjadi jika ada masalah jaringan, parsing, atau koneksi.
            Result.failure(e)
        } catch (e: Exception) {
            // Menangani exception umum lainnya.
            Result.failure(e)
        }
    }
}