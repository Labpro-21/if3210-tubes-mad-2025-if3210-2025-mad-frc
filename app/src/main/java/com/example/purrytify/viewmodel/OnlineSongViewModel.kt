package com.example.purrytify.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.model.Song
import com.example.purrytify.network.ApiService
import com.example.purrytify.utils.SessionManager
import com.example.purrytify.utils.toLocalSong
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class OnlineSongViewModel(
    private val api: ApiService,
    private val session: SessionManager
) : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _online = MutableStateFlow<List<Song>>(emptyList())
    val onlineSongs: StateFlow<List<Song>> = _online

    fun loadTopSongs(country: String?) = viewModelScope.launch {
        _isLoading.value = true
        try {
            val resp = if (country == null) api.getGlobalTopSongs()
                       else api.getTopSongsByCountry(country)
            if (resp.isSuccessful) {
                val userId = session.getUserId()
                _online.value = resp.body()?.map { it.toLocalSong(userId) } ?: emptyList()
            } else {
                _online.value = emptyList()
            }
        } catch (e: Exception) {
            _online.value = emptyList()
        }finally {
            _isLoading.value = false
        }
    }
}