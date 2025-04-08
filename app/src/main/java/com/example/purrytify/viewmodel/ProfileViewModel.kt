package com.example.purrytify.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.R
import com.example.purrytify.model.ProfileUiState
import com.example.purrytify.repository.ProfileRepository
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val _uiState = mutableStateOf(ProfileUiState())
    val uiState: State<ProfileUiState> get() = _uiState

    private val profileRepository = ProfileRepository()

    // Misalnya, kita dapat memanggil API saat inisialisasi view model (gunakan token yang valid)
    init {
        // Ganti "example-token" dengan token autentikasi sesungguhnya
        fetchUserProfile("example-token")
    }

    // Fungsi untuk mengambil data profile dari API
    fun fetchUserProfile(token: String) {
        viewModelScope.launch {
            profileRepository.fetchUserProfile(token).onSuccess { userProfile ->
                // Lakukan mapping data dari API ke UI state
                _uiState.value = _uiState.value.copy(
                    username = userProfile.username,
                    email = userProfile.email,
                    // Karena API mengembalikan URL foto, untuk saat ini kita menetapkan placeholder
                    profilePhoto = R.drawable.profile_placeholder,
                    // Jumlah lagu (songsAdded, likedSongs, listenedSongs) belum didapatkan dari API profile,
                    // jadi diset sebagai 0. Anda bisa mengintegrasikan API lain untuk data tersebut
                    songsAdded = 0,
                    likedSongs = 0,
                    listenedSongs = 0
                )
            }.onFailure {
                // Tangani error misalnya log error atau update UI state dengan pesan error
            }
        }
    }

    // Fungsi update manual jika dibutuhkan
    fun updateUsername(username: String) {
        _uiState.value = _uiState.value.copy(username = username)
    }

    fun updateEmail(email: String) {
        _uiState.value = _uiState.value.copy(email = email)
    }

    fun updateProfilePhoto(profilePhoto: Int) {
        _uiState.value = _uiState.value.copy(profilePhoto = profilePhoto)
    }

    fun updateSongsAdded(songsAdded: Int) {
        _uiState.value = _uiState.value.copy(songsAdded = songsAdded)
    }

    fun updateLikedSongs(likedSongs: Int) {
        _uiState.value = _uiState.value.copy(likedSongs = likedSongs)
    }

    fun updateListenedSongs(listenedSongs: Int) {
        _uiState.value = _uiState.value.copy(listenedSongs = listenedSongs)
    }
}