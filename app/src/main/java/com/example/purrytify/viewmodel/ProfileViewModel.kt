package com.example.purrytify.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.model.ProfileUiState
import com.example.purrytify.data.UserRepository
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

    fun fetchUserProfile() {
        viewModelScope.launch {
            // Ambil userId dari sesi
            val currentUserId = sessionManager.getUserId()
            val user = userRepository.getUserById(currentUserId)
            if (user != null) {
                Log.d("ProfileViewModel", "User profile fetched from DB: $user")
                _uiState.value = ProfileUiState(
                    username = user.email.substringBefore("@"), // atau gunakan field username jika tersedia
                    email = user.email,
                    profilePhoto = "", // update sesuai dengan field di database jika ada
                    country = parseCountryCode(user.email.takeLast(2)), // Contoh: ambil 2 karakter terakhir untuk kode negara
                    songsAdded = user.songs,
                    likedSongs = user.likedSongs,
                    listenedSongs = user.listenedSongs
                )
            } else {
                Log.e("ProfileViewModel", "User not found in DB for id: $currentUserId")
            }
        }
    }

    private fun parseCountryCode(code: String): String {
        return try {
            Locale("", code.uppercase()).displayCountry
        } catch (e: Exception) {
            code
        }
    }
}