package com.example.purrytify.utils

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.purrytify.model.Song  // sesuaikan path model Song-mu

object MusicServiceManager {
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _songProgress = MutableStateFlow<Float?>(null)
    val songProgress: StateFlow<Float?> = _songProgress.asStateFlow()

    private val _isLooping = MutableStateFlow<Boolean>(false)
    val isLooping: StateFlow<Boolean> = _isLooping.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    fun updateCurrentSong(song: Song?) {
        _currentSong.value = song
        Log.d("MusicServiceManager", "Current song in musicservicemanager changed to: ${song!!.title}")
    }

    fun updateSongProgress(position: Float) {
        _songProgress.value = position
        Log.d("MusicServiceManager", "Current song is ${position}s in")
    }

    fun updateLooping(isLooping: Boolean) {
        _isLooping.value = isLooping
        Log.d("MusicServiceManager", "Current song is looping : $isLooping")
    }

    fun updateIsPlaying(isPlaying: Boolean) {
        _isPlaying.value = isPlaying
        Log.d("MusicServiceManager", "Current song is looping : $isPlaying")
    }
}
