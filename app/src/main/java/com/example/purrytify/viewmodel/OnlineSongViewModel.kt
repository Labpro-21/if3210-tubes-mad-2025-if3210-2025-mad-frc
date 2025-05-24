package com.example.purrytify.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.model.Song
import com.example.purrytify.network.ApiService
import com.example.purrytify.utils.SessionManager
import com.example.purrytify.utils.toLocalSong
// import dagger.hilt.android.lifecycle.HiltViewModel // Jika menggunakan Hilt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// @HiltViewModel // Jika menggunakan Hilt
class OnlineSongViewModel(
    private val api: ApiService,
    private val session: SessionManager
    // @Inject constructor(...) // Jika menggunakan Hilt
) : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _online = MutableStateFlow<List<Song>>(emptyList())
    val onlineSongs: StateFlow<List<Song>> = _online

    // State untuk lagu tunggal yang di-fetch by ID (untuk deep link)
    private val _singleFetchedSong = MutableStateFlow<Song?>(null)
    val singleFetchedSong: StateFlow<Song?> = _singleFetchedSong

    fun loadTopSongs(country: String?) = viewModelScope.launch {
        _isLoading.value = true
        try {
            val resp = if (country == null) api.getGlobalTopSongs()
                       else api.getTopSongsByCountry(country)
            if (resp.isSuccessful) {
                val userId = session.getUserId().let { if (it == -1) 0 else it } // Ambil userId dari sesi
                _online.value = resp.body()?.map { it.toLocalSong(userId) } ?: emptyList()
            } else {
                _online.value = emptyList()
                Log.e("OnlineSongViewModel", "Failed to load top songs: ${resp.code()} - ${resp.message()}")
            }
        } catch (e: Exception) {
            _online.value = emptyList()
            Log.e("OnlineSongViewModel", "Exception loading top songs: ${e.message}", e)
        }finally {
            _isLoading.value = false
        }
    }

    // Fungsi baru untuk mengambil satu lagu berdasarkan ID
    suspend fun fetchSongById(songId: Int): Song? {
        _isLoading.value = true
        var fetchedSong: Song? = null
        try {
            val response = api.getSongDetail(songId) // Menggunakan fungsi dari ApiService
            if (response.isSuccessful) {
                response.body()?.let { networkSong ->
                    val userId = session.getUserId().let { if (it == -1) 0 else it } // Ambil userId dari sesi
                    fetchedSong = networkSong.toLocalSong(userId) // Konversi ke model Song lokal
                    _singleFetchedSong.value = fetchedSong // Update state jika perlu
                }
            } else {
                 Log.e("OnlineSongViewModel", "Failed to fetch song $songId: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("OnlineSongViewModel", "Exception fetching song $songId: ${e.message}", e)
        } finally {
            _isLoading.value = false
        }
        return fetchedSong
    }
}