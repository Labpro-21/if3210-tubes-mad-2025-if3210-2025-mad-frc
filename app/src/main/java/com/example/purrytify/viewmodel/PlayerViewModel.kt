package com.example.purrytify.viewmodel

import android.app.Application
import android.net.Uri
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val _exoPlayer = ExoPlayer.Builder(application).build()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionSeconds = MutableStateFlow(0f) // Progress dalam detik
    val currentPositionSeconds: StateFlow<Float> = _currentPositionSeconds.asStateFlow()

    val isLooping = MutableStateFlow(false)
    private var currentUri: Uri? = null
    private var progressUpdateJob: Job? = null
    private var onSongCompleteCallback: (() -> Unit)? = null

    var onPlaybackSecondTick: (() -> Unit)? = null // Callback untuk SongViewModel

    private val _shouldClosePlayerSheet = MutableStateFlow(false)
    val shouldClosePlayerSheet: StateFlow<Boolean> = _shouldClosePlayerSheet.asStateFlow() // Pastikan expose sebagai StateFlow

    fun closePlayerSheet() {
        _shouldClosePlayerSheet.value = true
    }

    fun resetCloseSheetFlag() {
        _shouldClosePlayerSheet.value = false
    }

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        _exoPlayer.setAudioAttributes(audioAttributes, true)
        _exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingValue: Boolean) {
                _isPlaying.value = isPlayingValue
                if (isPlayingValue) {
                    startProgressUpdates()
                } else {
                    stopProgressUpdates()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    _isPlaying.value = false // Pastikan ini juga diupdate
                    stopProgressUpdates()
                    onSongCompleteCallback?.invoke()
                    if (isLooping.value) {
                        _exoPlayer.seekTo(0)
                        _exoPlayer.play()
                    } else {
                         _currentPositionSeconds.value = (_exoPlayer.duration / 1000).toFloat().coerceAtLeast(0f) // Set ke durasi penuh jika selesai
                    }
                }
            }
        })
    }

    private fun startProgressUpdates() {
        stopProgressUpdates()
        progressUpdateJob = viewModelScope.launch {
            while (true) { // Loop terus menerus selama job aktif
                if (_exoPlayer.isPlaying) {
                    _currentPositionSeconds.value = (_exoPlayer.currentPosition / 1000).toFloat()
                    onPlaybackSecondTick?.invoke() // Panggil callback
                }
                delay(1000) // Setiap detik
            }
        }
    }

    private fun stopProgressUpdates() {
        progressUpdateJob?.cancel()
        progressUpdateJob = null
    }

    fun prepareAndPlay(uri: Uri, onSongComplete: () -> Unit = {}) {
        if (_exoPlayer.isPlaying) {
            _exoPlayer.stop() // Hentikan pemutaran lama
        }
        stopProgressUpdates() // Hentikan update progress lama
        _currentPositionSeconds.value = 0f // Reset progress

        currentUri = uri
        onSongCompleteCallback = onSongComplete // Simpan callback
        _exoPlayer.setMediaItem(MediaItem.fromUri(uri))
        _exoPlayer.prepare()
        _exoPlayer.play() // isPlaying akan diupdate oleh listener
    }

    fun playPause() {
        if (_exoPlayer.isPlaying) {
            _exoPlayer.pause()
        } else {
            if (_exoPlayer.playbackState == Player.STATE_ENDED) { // Jika lagu sudah selesai dan ingin play lagi
                _exoPlayer.seekTo(0)
            }
            _exoPlayer.play()
        }
    }

    fun seekTo(seconds: Float) {
        _exoPlayer.seekTo((seconds * 1000).toLong())
        _currentPositionSeconds.value = seconds // Langsung update UI progress
    }

    fun stopPlayer() {
        _exoPlayer.stop()
        // isPlaying akan diupdate ke false oleh listener
        // stopProgressUpdates() akan dipanggil oleh listener
        _currentPositionSeconds.value = 0f
        currentUri = null
        onSongCompleteCallback = null
    }

    // closePlayerSheet dan resetCloseSheetFlag tidak ada di PlayerViewModel Anda,
    // jadi saya hapus dari sini. Jika ada, bisa ditambahkan kembali.

    override fun onCleared() {
        super.onCleared()
        _exoPlayer.release()
        stopProgressUpdates()
    }

    fun toggleLoop() {
        isLooping.value = !isLooping.value
        _exoPlayer.repeatMode = if (isLooping.value) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }
}