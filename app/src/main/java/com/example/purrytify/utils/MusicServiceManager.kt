package com.example.purrytify.utils

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.purrytify.model.Song  // sesuaikan path model Song-mu

object MusicServiceManager {
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    fun updateCurrentSong(song: Song?) {
        _currentSong.value = song
        Log.d("MusicServiceManager", "Current song in musicservicemanager changed to: ${song!!.title}")
    }
}
