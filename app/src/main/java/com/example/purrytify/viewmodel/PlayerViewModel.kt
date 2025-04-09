package com.example.purrytify.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    val isLooping = MutableStateFlow(false)

    private var duration = 1L

    private var currentUri: Uri? = null


    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        _exoPlayer.setAudioAttributes(audioAttributes, true)
        viewModelScope.launch {
            while (true) {
                delay(500)
                if (_exoPlayer.isPlaying) {
                    val currentPosition = _exoPlayer.currentPosition
                    val duration = _exoPlayer.duration.takeIf { it > 0 } ?: 1L
                    _progress.value = currentPosition.toFloat() / duration
                }
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
        val duration = _exoPlayer.duration.takeIf { it > 0 } ?: 1L
        _exoPlayer.seekTo((percent * duration).toLong())
    }


    fun updateProgress(value: Float) {
        _progress.value = value
    }

    override fun onCleared() {
        super.onCleared()
        _exoPlayer.release()
    }

    fun toggleLoop(){
        isLooping.value = !isLooping.value
        if (isLooping.value == true){
            _exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
        }else{
            _exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
        }


    }


}


