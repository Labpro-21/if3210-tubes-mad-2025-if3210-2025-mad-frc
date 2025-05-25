package com.example.purrytify.repository

import com.example.purrytify.model.LoginRequest
import com.example.purrytify.model.LoginResponse
import com.example.purrytify.network.RetrofitClient
import retrofit2.HttpException
import java.io.IOException

class LoginRepository {


    suspend fun login(email: String, password: String): Result<LoginResponse> {
        return try {
            val request = LoginRequest(email, password)
            val response = RetrofitClient.loginApiService.login(request)
            if (response.isSuccessful) {
                response.body()?.let { loginResponse ->
                    Result.success(loginResponse)
                } ?: Result.failure(Exception("Response body is null"))
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Unknown error"
                Result.failure(Exception("Login failed with code ${response.code()}: $errorMessage"))
            }
        } catch (e: HttpException) {
            Result.failure(e)
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}