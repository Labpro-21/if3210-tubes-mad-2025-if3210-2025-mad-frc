package com.example.purrytify.viewmodel

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.model.Song
import com.example.purrytify.network.ApiService
import com.example.purrytify.service.MusicService
import com.example.purrytify.utils.SessionManager
import com.example.purrytify.utils.toLocalSong

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


class OnlineSongViewModel(
    private val api: ApiService,
    private val session: SessionManager,
    private val context: Application,
) : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _online = MutableStateFlow<List<Song>>(emptyList())
    val onlineSongs: StateFlow<List<Song>> = _online


    private val _singleFetchedSong = MutableStateFlow<Song?>(null)
    val singleFetchedSong: StateFlow<Song?> = _singleFetchedSong

    fun loadTopSongs(country: String?) = viewModelScope.launch {
        _isLoading.value = true
        try {
            val resp = if (country == null) api.getGlobalTopSongs()
                       else api.getTopSongsByCountry(country)
            if (resp.isSuccessful) {
                val userId = session.getUserId().let { if (it == -1) 0 else it }
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

    fun sendSongsToMusicService() {
        val songList = _online.value
        Log.d("OnlineSongViewModel", "Sending Playlist of ${songList.size} length")
        val intent = Intent(context, MusicService::class.java).apply {
            action = "ACTION_SET_PLAYLIST"
            putParcelableArrayListExtra("playlist", ArrayList(songList))
            Log.d("OnlineSongViewModel", "Sending Playlist of ${ArrayList(songList).size} length")
        }
        context.startService(intent)
    }

    suspend fun fetchSongById(songId: Int): Song? {
        _isLoading.value = true
        var fetchedSong: Song? = null
        try {
            val response = api.getSongDetail(songId)
            if (response.isSuccessful) {
                response.body()?.let { networkSong ->
                    val userId = session.getUserId().let { if (it == -1) 0 else it }
                    fetchedSong = networkSong.toLocalSong(userId)
                    _singleFetchedSong.value = fetchedSong
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

    fun getSongIndex(song : Song):Int{
        return onlineSongs.value.indexOf(song)
    }
}