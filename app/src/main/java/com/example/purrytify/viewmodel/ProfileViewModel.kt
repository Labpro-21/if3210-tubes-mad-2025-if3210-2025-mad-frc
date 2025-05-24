package com.example.purrytify.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.AppDatabase
import com.example.purrytify.data.DailyListenDuration
import com.example.purrytify.model.ProfileUiState
import com.example.purrytify.model.SoundCapsule
import com.example.purrytify.repository.AnalyticsRepository
import com.example.purrytify.repository.ProfileRepository
import com.example.purrytify.repository.UserRepository
import com.example.purrytify.utils.SessionManager
import com.example.purrytify.utils.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow       
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class ProfileViewModel(
    // ... (dependensi yang sudah ada) ...
    @SuppressLint("StaticFieldLeak") private val context: Context, // Jika analyticsRepo membutuhkan context
    private val tokenManager: TokenManager,
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    // ... (State dan fungsi yang sudah ada untuk ProfileUiState dan SoundCapsule) ...
    private val _uiState = mutableStateOf(ProfileUiState())
    val uiState: State<ProfileUiState> get() = _uiState

    private val profileRepository = ProfileRepository(tokenManager) // Asumsi ini ada

    // AnalyticsRepository sekarang membutuhkan DAO, jadi kita perlu AppDatabase
    private val appDatabase = AppDatabase.getDatabase(context) // Dapatkan instance AppDatabase
    private val analyticsRepo = AnalyticsRepository(
        dao = appDatabase.songDao(), // Berikan SongDao
        context = context
    )

    private val _analytics = MutableStateFlow(SoundCapsule())
    val analytics: StateFlow<SoundCapsule> = _analytics.asStateFlow() // Gunakan asStateFlow()

    // StateFlow baru untuk data durasi harian
    private val _dailyListenData = MutableStateFlow<List<DailyListenDuration>>(emptyList())
    val dailyListenData: StateFlow<List<DailyListenDuration>> = _dailyListenData.asStateFlow()

    private val _selectedMonthData = MutableStateFlow<YearMonth>(YearMonth.now())
    val selectedMonthData: StateFlow<YearMonth> = _selectedMonthData.asStateFlow()

    // Fungsi yang sudah ada untuk mengambil data profil utama
    fun fetchUserProfile() {
        viewModelScope.launch {
            val currentUserId = sessionManager.getUserId()
            if (currentUserId <= 0) {
                Log.w("ProfileViewModel", "fetchUserProfile: Invalid user ID ($currentUserId)")
                // Handle error, mungkin set uiState ke default atau error state
                _uiState.value = ProfileUiState(username = "Error", email = "N/A")
                return@launch
            }

            profileRepository.fetchUserProfile().onSuccess { apiProfile ->
                val userStats = userRepository.getUserById(currentUserId)
                _uiState.value = ProfileUiState(
                    username = apiProfile.username,
                    email = apiProfile.email,
                    profilePhoto = apiProfile.profilePhoto,
                    country = parseCountryCode(apiProfile.location),
                    songsAdded = userStats?.songs ?: 0,
                    likedSongs = userStats?.likedSongs ?: 0,
                    listenedSongs = userStats?.listenedSongs ?: 0
                )
                Log.d("ProfileViewModel", "Profile updated: ${_uiState.value}")
            }.onFailure { throwable ->
                Log.e("ProfileViewModel", "Gagal fetch profile", throwable)
                 // Ambil data dari DB lokal jika API gagal
                val userStats = userRepository.getUserById(currentUserId)
                if (userStats != null) {
                     _uiState.value = ProfileUiState(
                        username = userStats.email.substringBefore('@'), // Contoh username dari email
                        email = userStats.email,
                        profilePhoto = "", // Tidak ada foto dari DB lokal
                        country = "Indonesia", // Default
                        songsAdded = userStats.songs,
                        likedSongs = userStats.likedSongs,
                        listenedSongs = userStats.listenedSongs
                    )
                } else {
                    _uiState.value = ProfileUiState(username = "User", email = "N/A") // Fallback jika semua gagal
                }
            }
        }
    }


    private fun parseCountryCode(code: String): String {
        return try {
            Locale("", code.uppercase()).displayCountry
        } catch (e: Exception) {
            code // Kembalikan kode asli jika gagal parse
        }
    }


    // Fungsi yang sudah ada untuk mengambil data SoundCapsule ringkasan
    fun loadMonthlySummaryAnalytics(month: YearMonth = YearMonth.now()) {
        viewModelScope.launch {
            val uid = sessionManager.getUserId()
            if (uid > 0) {
                val yearMonthString = month.format(DateTimeFormatter.ofPattern("yyyy-MM"))
                Log.d("ProfileViewModel", "Loading monthly summary analytics for user $uid, month $yearMonthString")
                _analytics.value = analyticsRepo.getMonthlyAnalytics(uid, yearMonthString)
            } else {
                Log.w("ProfileViewModel", "Cannot load monthly summary, invalid userId: $uid")
                _analytics.value = SoundCapsule(month = month)
            }
        }
    }

    // Fungsi baru untuk mengambil data durasi harian untuk grafik
    fun loadDailyListenDetailsForMonth(month: YearMonth = YearMonth.now()) {
        _selectedMonthData.value = month // Simpan bulan yang dipilih
        viewModelScope.launch {
            val uid = sessionManager.getUserId()
            if (uid > 0) {
                val startOfMonthMillis = month.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val endOfMonthMillis = month.plusMonths(1).atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                Log.d("ProfileViewModel", "Loading daily listen details for user $uid, month $month, range: $startOfMonthMillis to $endOfMonthMillis")
                _dailyListenData.value = appDatabase.songDao().getDailyListenDurationsForMonth(uid, startOfMonthMillis, endOfMonthMillis)
                Log.d("ProfileViewModel", "Fetched daily data count: ${_dailyListenData.value.size}")
            } else {
                Log.w("ProfileViewModel", "Cannot load daily listen details, invalid userId: $uid")
                _dailyListenData.value = emptyList()
            }
        }
    }


    fun exportCsv(): File? { // Ubah return type menjadi nullable
        return if (_analytics.value.month != null) { // Hanya ekspor jika ada data analitik
            analyticsRepo.exportToCsv(_analytics.value)
        } else {
            Log.w("ProfileViewModel", "Cannot export CSV, no analytics data loaded.")
            null
        }
    }
}