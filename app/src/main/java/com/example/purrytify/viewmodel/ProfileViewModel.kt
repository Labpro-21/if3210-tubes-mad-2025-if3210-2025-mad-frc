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
import com.example.purrytify.data.ArtistRankInfo
import com.example.purrytify.data.DailyListenDuration
import com.example.purrytify.model.ProfileUiState
import com.example.purrytify.model.Song
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
import kotlinx.coroutines.flow.first

class ProfileViewModel(
    @SuppressLint("StaticFieldLeak") private val context: Context,
    private val tokenManager: TokenManager,
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {


    private val _uiState = mutableStateOf(ProfileUiState())
    val uiState: State<ProfileUiState> get() = _uiState


    var isLoading by mutableStateOf(false)
        private set
    var errorMsg by mutableStateOf<String?>(null)
        private set


    private val profileRepository = ProfileRepository(tokenManager)


    private val appDatabase = AppDatabase.getDatabase(context)
    private val analyticsRepo = AnalyticsRepository(
        dao = appDatabase.songDao(),
        context = context
    )
    private val _analytics = MutableStateFlow(SoundCapsule())
    val analytics: StateFlow<SoundCapsule> = _analytics.asStateFlow()

    private val _dailyListenData = MutableStateFlow<List<DailyListenDuration>>(emptyList())
    val dailyListenData: StateFlow<List<DailyListenDuration>> = _dailyListenData.asStateFlow()

    private val _selectedMonthData = MutableStateFlow<YearMonth>(YearMonth.now())
    val selectedMonthData: StateFlow<YearMonth> = _selectedMonthData.asStateFlow()

    private val _userTopPlayedSongs = MutableStateFlow<List<Song>>(emptyList())
    val userTopPlayedSongs: StateFlow<List<Song>> = _userTopPlayedSongs.asStateFlow()

    private val _selectedMonthForTopPlayed = MutableStateFlow<YearMonth>(YearMonth.now())

    private val _userTopPlayedArtists = MutableStateFlow<List<ArtistRankInfo>>(emptyList())
    val userTopPlayedArtists: StateFlow<List<ArtistRankInfo>> = _userTopPlayedArtists.asStateFlow()

    private val _totalDistinctArtists = MutableStateFlow(0)
    val totalDistinctArtists: StateFlow<Int> = _totalDistinctArtists.asStateFlow()

    val currentAnalytics = _analytics.value
    val currentMonth = currentAnalytics.month ?: YearMonth.now()

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


    fun loadDailyListenDetailsForMonth(month: YearMonth = YearMonth.now()) {
        _selectedMonthData.value = month
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

    suspend fun prepareAndExportCsv(): File? { 
        val currentAnalytics = _analytics.value 
        val currentMonth = currentAnalytics.month ?: YearMonth.now() 

        val yearMonthString = currentMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val uid = sessionManager.getUserId()

        if (uid <= 0) {
            Log.w("ProfileViewModel", "Cannot export CSV, invalid user ID.")
            return null
        }
        if (currentAnalytics.month == null) {
            Log.w("ProfileViewModel", "Cannot export CSV, analytics data not loaded yet.")
            return null
        }
        
        val topArtistsList = analyticsRepo.getTopPlayedArtistsForMonth(uid, yearMonthString).first()
        val topSongsList = analyticsRepo.getTopPlayedSongsForMonth(uid, yearMonthString).first()

        if (topArtistsList.isEmpty() && topSongsList.isEmpty() && currentAnalytics.timeListenedMillis == null) {
            Log.w("ProfileViewModel", "No data to export for $yearMonthString.")
            return null
        }


        return analyticsRepo.exportToCsv(currentAnalytics, topArtistsList, topSongsList)
    }


    fun clearErrorMsg() {
        errorMsg = null
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

                    onResult(true)
                }
                .onFailure { e ->
                    errorMsg = e.message
                    onResult(false)
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

    fun loadUserTopPlayedSongs(month: YearMonth = YearMonth.now()) {
        _selectedMonthForTopPlayed.value = month
        viewModelScope.launch {
            val uid = sessionManager.getUserId()
            if (uid > 0) {
                val yearMonthString = month.format(DateTimeFormatter.ofPattern("yyyy-MM"))
                Log.d("ProfileViewModel", "Loading top played songs for user $uid, month $yearMonthString")
                analyticsRepo.getTopPlayedSongsForMonth(uid, yearMonthString)
                    .collect { songs ->
                        _userTopPlayedSongs.value = songs
                        Log.d("ProfileViewModel", "Fetched top played songs count: ${songs.size}")
                    }
            } else {
                Log.w("ProfileViewModel", "Cannot load top played songs, invalid userId: $uid")
                _userTopPlayedSongs.value = emptyList()
            }
        }
    }

    fun loadUserTopPlayedArtists(month: YearMonth = YearMonth.now()) {

        _selectedMonthForTopPlayed.value = month
        viewModelScope.launch {
            val uid = sessionManager.getUserId()
            if (uid > 0) {
                val yearMonthString = month.format(DateTimeFormatter.ofPattern("yyyy-MM"))
                Log.d("ProfileViewModel", "Loading top played artists for user $uid, month $yearMonthString")


                _totalDistinctArtists.value = analyticsRepo.getTotalDistinctArtistsForMonth(uid, yearMonthString)


                analyticsRepo.getTopPlayedArtistsForMonth(uid, yearMonthString)
                    .collect { artists ->
                        _userTopPlayedArtists.value = artists
                        Log.d("ProfileViewModel", "Fetched top played artists count: ${artists.size}")
                        Log.d("ProfileViewModel", "Total distinct artists for $yearMonthString: ${_totalDistinctArtists.value}")
                    }
            } else {
                Log.w("ProfileViewModel", "Cannot load top played artists, invalid userId: $uid")
                _userTopPlayedArtists.value = emptyList()
                _totalDistinctArtists.value = 0
            }
        }
    }
}