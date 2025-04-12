package com.example.purrytify.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.Log
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

    private val _progress = MutableStateFlow(1f)
    val progress = _progress.asStateFlow()

    val isLooping = MutableStateFlow(false)

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
                    _progress.value = (currentPosition / 1000).toFloat()
                }
            }
        }
    }


    fun prepareAndPlay(uri: Uri, onSongComplete: () -> Unit = {}) {
        if (currentUri == uri) return
        currentUri = uri

        _exoPlayer.setMediaItem(MediaItem.fromUri(uri))
        _exoPlayer.prepare()
        _exoPlayer.play()
        _isPlaying.value = true

        _exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    _isPlaying.value = false
                    onSongComplete()
                }
            }
        })
    }



    fun playPause() {
        Log.d("PlayerViewModel", "playPause() invoked. isPlaying before: ${_exoPlayer.isPlaying}")

        // Jika belum ada media item, coba atur ulang dari currentUri (jika ada)
        if (_exoPlayer.currentMediaItem == null) {
            if (currentUri != null) {
                _exoPlayer.setMediaItem(MediaItem.fromUri(currentUri!!))
                _exoPlayer.prepare()
            } else {
                Log.d("PlayerViewModel", "No media item or currentUri available")
                return
            }
        }

        if (_exoPlayer.isPlaying) {
            _exoPlayer.pause()
        } else {
            _exoPlayer.play()
        }
        
        // Jangan lupa update state setelah perubahan
        _isPlaying.value = _exoPlayer.isPlaying
        Log.d("PlayerViewModel", "playPause() finished. isPlaying now: ${_exoPlayer.isPlaying}")
    }

    fun seekTo(seconds: Float) {
//        val duration = _exoPlayer.duration.takeIf { it > 0 } ?: 1L
        _exoPlayer.seekTo((seconds*1000).toLong())
    }

//
//    fun updateProgress(value: Float) {
//        _progress.value = value
//    }

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


