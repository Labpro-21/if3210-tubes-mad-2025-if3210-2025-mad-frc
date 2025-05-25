package com.example.purrytify.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.AppDatabase
import com.example.purrytify.model.ProfileUiState
import com.example.purrytify.model.SoundCapsule
import com.example.purrytify.repository.AnalyticsRepository
import com.example.purrytify.repository.ProfileRepository
import com.example.purrytify.repository.UserRepository
import com.example.purrytify.utils.SessionManager
import com.example.purrytify.utils.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.time.YearMonth
import java.util.Locale

class ProfileViewModel(
    @SuppressLint("StaticFieldLeak") private val context: Context,
    private val tokenManager: TokenManager,
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    // UiState untuk profil
    private val _uiState = mutableStateOf(ProfileUiState())
    val uiState: State<ProfileUiState> get() = _uiState

    // loading & error state untuk editProfile
    var isLoading by mutableStateOf(false)
        private set
    var errorMsg by mutableStateOf<String?>(null)
        private set

    // repository untuk edit profile
    private val profileRepository = ProfileRepository(tokenManager)

    // analytics
    private val analyticsRepo = AnalyticsRepository(
        dao = AppDatabase.getDatabase(context).songDao(),
        context = context
    )
    private val _analytics = MutableStateFlow(SoundCapsule())
    val analytics: StateFlow<SoundCapsule> = _analytics

    fun fetchUserProfile() {
        viewModelScope.launch {
            profileRepository.fetchUserProfile()
                .onSuccess { apiProfile ->
                    val uid = sessionManager.getUserId()
                    val stats = userRepository.getUserById(uid)
                    _uiState.value = ProfileUiState(
                        username     = apiProfile.username,
                        email        = apiProfile.email,
                        profilePhoto = apiProfile.profilePhoto,
                        country      = parseCountryCode(apiProfile.location),
                        songsAdded   = stats?.songs ?: 0,
                        likedSongs   = stats?.likedSongs ?: 0,
                        listenedSongs= stats?.listenedSongs ?: 0
                    )
                    Log.d("ProfileVM", "Profile fetched: ${_uiState.value}")
                }
                .onFailure { t ->
                    Log.e("ProfileVM", "Error fetching profile", t)
                }
        }
    }

    fun loadAnalytics(month: YearMonth = YearMonth.now()) {
        viewModelScope.launch {
            val uid = sessionManager.getUserId()
            _analytics.value = analyticsRepo.getMonthlyAnalytics(uid, month.toString())
        }
    }

    fun exportCsv(): File {
        return analyticsRepo.exportToCsv(_analytics.value)
    }

    /** Update profile dengan optional lokasi & foto */
    fun updateProfile(
        location: String?,
        photoUri: Uri?,
        onResult: (success: Boolean) -> Unit
    ) {
        viewModelScope.launch {
            isLoading = true
            errorMsg = null
            profileRepository
                .editProfile(location, photoUri, context)
                .onSuccess {
                    // update UI state…
                    onResult(true)      // <-- sukses
                }
                .onFailure { e ->
                    errorMsg = e.message
                    onResult(false)     // <-- gagal
                }
            isLoading = false
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