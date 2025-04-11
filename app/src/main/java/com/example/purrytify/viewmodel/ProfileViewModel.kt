package com.example.purrytify.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.model.ProfileUiState
import com.example.purrytify.repository.ProfileRepository
import com.example.purrytify.utils.TokenManager
import kotlinx.coroutines.launch
import java.util.Locale

class ProfileViewModel(private val tokenManager: TokenManager) : ViewModel() {

    private val _uiState = mutableStateOf(ProfileUiState())
    val uiState: State<ProfileUiState> get() = _uiState

    private val profileRepository = ProfileRepository(tokenManager)

    fun fetchUserProfile() {
        viewModelScope.launch {
            profileRepository.fetchUserProfile().onSuccess { userProfile ->
                Log.d("ProfileViewModel", "User profile fetched: $userProfile")
                _uiState.value = ProfileUiState(
                    username = userProfile.username,
                    email = userProfile.email,
                    profilePhoto = userProfile.profilePhoto,
                    country = parseCountryCode(userProfile.location),
                    songsAdded = 0,   // Sesuaikan jika ada data
                    likedSongs = 0,
                    listenedSongs = 0
                )
            }.onFailure { throwable ->
                Log.e("ProfileViewModel", "Gagal fetch profile", throwable)
            }
        }
    }

    private fun parseCountryCode(code: String): String {
        return try {
            // Membuat Locale dengan kode negara (pastikan kode sudah sesuai dengan ISO 3166-1 alpha-2)
            Locale("", code.uppercase()).displayCountry
        } catch (e: Exception) {
            code // Jika terjadi error, kembalikan kode aslinya
        }
    }
}