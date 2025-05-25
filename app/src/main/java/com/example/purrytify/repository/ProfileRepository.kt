package com.example.purrytify.repository

import android.content.Context
import android.net.Uri
import com.example.purrytify.model.UserProfile
import com.example.purrytify.network.RetrofitClient
import com.example.purrytify.network.ApiService
import com.example.purrytify.utils.TokenManager
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.IOException

class ProfileRepository(private val tokenManager: TokenManager) {

    private val api: ApiService = RetrofitClient.create(tokenManager)


    suspend fun fetchUserProfile(): Result<UserProfile> {
        return try {

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

    suspend fun editProfile(
    location: String?,
    photoUri: Uri?,
    context: Context
    ): Result<UserProfile> {

        val locPart = location?.toRequestBody("text/plain".toMediaType())


        val photoPart = photoUri?.let { uri ->
            val stream = context.contentResolver.openInputStream(uri)!!
            val temp = File.createTempFile("profile", ".jpg", context.cacheDir)
            temp.outputStream().use { out -> stream.copyTo(out) }
            val reqFile = temp.asRequestBody("image/jpeg".toMediaType())
            MultipartBody.Part.createFormData("profilePhoto", temp.name, reqFile)
        }

        return try {
            val resp = api.editProfile(locPart, photoPart)
            if (resp.isSuccessful && resp.body()!=null)
                Result.success(resp.body()!!)
            else
                Result.failure(Exception("Failed: ${resp.code()}"))
        } catch(e: Exception) {
            Result.failure(e)
        }
    }
}