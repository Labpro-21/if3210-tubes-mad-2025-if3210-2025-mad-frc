package com.example.purrytify.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.model.ProfileUiState
import com.example.purrytify.repository.UserRepository
import com.example.purrytify.repository.ProfileRepository
import com.example.purrytify.utils.SessionManager
import com.example.purrytify.utils.TokenManager
import kotlinx.coroutines.launch
import java.util.Locale

class ProfileViewModel(
    private val tokenManager: TokenManager,
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = mutableStateOf(ProfileUiState())
    val uiState: State<ProfileUiState> get() = _uiState

    // Buat instance ProfileRepository untuk memanggil API
    private val profileRepository = ProfileRepository(tokenManager)

    fun fetchUserProfile() {
        viewModelScope.launch {
            // Pertama, ambil profil dari API
            profileRepository.fetchUserProfile().onSuccess { apiProfile ->
                // Ambil user id dari sesi untuk mendapatkan statistik dari database lokal
                val currentUserId = sessionManager.getUserId()
                val userStats = userRepository.getUserById(currentUserId)
                // Gabungkan data API dan statistik dari DB
                _uiState.value = ProfileUiState(
                    username = apiProfile.username,
                    email = apiProfile.email,
                    profilePhoto = apiProfile.profilePhoto, // URL foto profil dari API
                    country = parseCountryCode(apiProfile.location),
                    songsAdded = userStats?.songs ?: 0,
                    likedSongs = userStats?.likedSongs ?: 0,
                    listenedSongs = userStats?.listenedSongs ?: 0
                )
                Log.d("ProfileViewModel", "Profile updated: ${_uiState.value}")
            }.onFailure { throwable ->
                Log.e("ProfileViewModel", "Gagal fetch profile", throwable)
            }
        }
    }

    private fun parseCountryCode(code: String): String {
        return try {
            // Asumsikan 'code' adalah kode negara ISO, misalnya "ID"
            Locale("", code.uppercase()).displayCountry
        } catch (e: Exception) {
            code
        }
    }
}