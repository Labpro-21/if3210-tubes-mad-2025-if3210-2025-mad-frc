package com.example.purrytify.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.AppDatabase
import com.example.purrytify.model.Song
import com.example.purrytify.repository.RecommendationRepository
import com.example.purrytify.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class RecommendationViewModel(
    private val application: Application, // Ditambahkan untuk SessionManager jika diperlukan
    private val recommendationRepository: RecommendationRepository,
    private val onlineSongViewModel: OnlineSongViewModel, // Untuk mendapatkan lagu server
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _dailyMix = MutableStateFlow<List<Song>>(emptyList())
    val dailyMix: StateFlow<List<Song>> = _dailyMix.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        val userId = sessionManager.getUserId()
        if (userId > 0) {
            refreshDailyMix()
        }
    }

    fun refreshDailyMix() {
        val userId = sessionManager.getUserId()
        if (userId <= 0) {
            Log.w("RecommendationVM", "Cannot refresh daily mix, invalid userId: $userId")
            _dailyMix.value = emptyList()
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Ambil kandidat lagu lokal
                val localAddedSongs = recommendationRepository.getRandomExplicitlyAddedSongs(userId, 10) // Ambil lebih banyak untuk pool
                val localLikedSongs = recommendationRepository.getRandomLikedSongs(userId, 10)  // Ambil lebih banyak untuk pool

                // 2. Ambil lagu server
                if (onlineSongViewModel.onlineSongs.value.isEmpty()) {
                    Log.d("RecommendationVM", "Online songs empty, fetching global top songs.")
                    onlineSongViewModel.loadTopSongs(null) // Muat jika kosong
                    // Mungkin perlu delay atau mekanisme untuk menunggu onlineSongViewModel selesai memuat
                    // Untuk sekarang, kita asumsikan pemanggilan berikutnya akan memiliki data.
                }
                val serverSongsPool = onlineSongViewModel.onlineSongs.value.shuffled()
                Log.d("RecommendationVM", "Server songs pool size: ${serverSongsPool.size}")


                // 3. Gabungkan dan filter
                val candidateSongs = mutableListOf<Song>()
                candidateSongs.addAll(localAddedSongs)
                candidateSongs.addAll(localLikedSongs)

                // Tambahkan lagu server yang belum ada di lokal (cek berdasarkan audioPath)
                val localAudioPaths = (localAddedSongs + localLikedSongs).map { it.audioPath }.toSet()
                serverSongsPool.forEach { serverSong ->
                    if (serverSong.audioPath !in localAudioPaths) {
                        // Periksa juga ke DB untuk memastikan tidak ada duplikat yang tidak terambil oleh query random awal
                        if (!recommendationRepository.doesSongExistByAudioPath(userId, serverSong.audioPath)) {
                            candidateSongs.add(serverSong)
                        }
                    }
                }

                // Pastikan tidak ada duplikat berdasarkan audioPath dan acak hasilnya
                val finalMix = candidateSongs.distinctBy { it.audioPath }.shuffled().take(Random.nextInt(5, 16)) // Ambil 5-15 lagu

                _dailyMix.value = finalMix
                Log.d("RecommendationVM", "Daily Mix generated with ${finalMix.size} songs.")

            } catch (e: Exception) {
                Log.e("RecommendationVM", "Error refreshing daily mix: ${e.message}", e)
                _dailyMix.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}

class RecommendationViewModelFactory(
    private val application: Application,
    private val onlineSongViewModel: OnlineSongViewModel
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecommendationViewModel::class.java)) {
            val db = AppDatabase.getDatabase(application)
            val recommendationRepository = RecommendationRepository(db.songDao())
            val sessionManager = SessionManager(application)
            @Suppress("UNCHECKED_CAST")
            return RecommendationViewModel(application, recommendationRepository, onlineSongViewModel, sessionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class for RecommendationViewModelFactory")
    }
}