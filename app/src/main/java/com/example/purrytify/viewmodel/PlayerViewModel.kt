package com.example.purrytify.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val context: Application
) : AndroidViewModel(context) {

    private val _exoPlayer = ExoPlayer.Builder(context).build()
//    val exoPlayer: ExoPlayer get() = _exoPlayer

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress = _progress.asStateFlow()

    private var duration = 1L

    private var currentUri: Uri? = null

    init {
        viewModelScope.launch {
            while (true) {
                if (_exoPlayer.isPlaying) {
                    duration = _exoPlayer.duration.takeIf { it > 0 } ?: 1L
                    _progress.value = _exoPlayer.currentPosition / duration.toFloat()
                    _isPlaying.value = _exoPlayer.isPlaying
                }
                delay(500)
            }
        }
    }

//    fun prepare(uri: Uri) {
//        _exoPlayer.setMediaItem(MediaItem.fromUri(uri))
//        _exoPlayer.prepare()
//    }

    fun prepareAndPlay(uri: Uri) {
        if (currentUri == uri) return
        currentUri = uri
        _exoPlayer.setMediaItem(MediaItem.fromUri(uri))
        _exoPlayer.prepare()
        _exoPlayer.play()
        _isPlaying.value = true
    }


    fun playPause() {
        if (_exoPlayer.isPlaying) {
            _exoPlayer.pause()
        } else {
            _exoPlayer.play()
        }
        _isPlaying.value = _exoPlayer.isPlaying
    }

    fun seekTo(percent: Float) {
        duration = _exoPlayer.duration.takeIf { it > 0 } ?: 1L
        _exoPlayer.seekTo((percent * duration).toLong())
    }

    override fun onCleared() {
        super.onCleared()
        _exoPlayer.release()
    }
}


