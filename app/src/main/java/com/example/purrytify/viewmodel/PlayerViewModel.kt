package com.example.purrytify.viewmodel

import android.Manifest // Pastikan import ini ada
import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager // Import untuk PackageManager
import android.media.AudioDeviceInfo // Import untuk AudioDeviceInfo
import android.net.Uri
import android.os.Build // Import untuk Build.VERSION
import androidx.annotation.OptIn
import androidx.core.app.ActivityCompat // Import untuk ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
// import androidx.media3.common.util.UnstableApi // Hanya jika Anda menggunakan API yang ditandai UnstableApi
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

    private val _currentPositionSeconds = MutableStateFlow(0f)
    val currentPositionSeconds: StateFlow<Float> = _currentPositionSeconds.asStateFlow()

    val isLooping = MutableStateFlow(false)
    private var currentUri: Uri? = null
    private var progressUpdateJob: Job? = null
    private var onSongCompleteCallback: (() -> Unit)? = null

    var onPlaybackSecondTick: (() -> Unit)? = null

    // StateFlow dan fungsi untuk menutup sheet (jika Anda memindahkannya kembali ke sini atau membutuhkannya)
    private val _shouldClosePlayerSheetInternal = MutableStateFlow(false)
    val shouldClosePlayerSheet: StateFlow<Boolean> = _shouldClosePlayerSheetInternal.asStateFlow()


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
                    _isPlaying.value = false
                    stopProgressUpdates()
                    onSongCompleteCallback?.invoke()
                    if (isLooping.value) {
                        _exoPlayer.seekTo(0)
                        _exoPlayer.play()
                    } else {
                        val durationSeconds = (_exoPlayer.duration / 1000).toFloat().coerceAtLeast(0f)
                        if (durationSeconds > 0) { // Hanya set ke durasi jika durasi valid
                            _currentPositionSeconds.value = durationSeconds
                        }
                    }
                }
            }
        })
    }

    private fun startProgressUpdates() {
        stopProgressUpdates()
        progressUpdateJob = viewModelScope.launch {
            while (true) {
                if (_exoPlayer.isPlaying) {
                    _currentPositionSeconds.value = (_exoPlayer.currentPosition / 1000).toFloat()
                    onPlaybackSecondTick?.invoke()
                }
                delay(1000)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressUpdateJob?.cancel()
        progressUpdateJob = null
    }

    fun prepareAndPlay(uri: Uri, onSongComplete: () -> Unit = {}) {
        if (_exoPlayer.isPlaying) {
            _exoPlayer.stop()
        }
        stopProgressUpdates()
        _currentPositionSeconds.value = 0f

        currentUri = uri
        onSongCompleteCallback = onSongComplete
        _exoPlayer.setMediaItem(MediaItem.fromUri(uri))
        _exoPlayer.prepare()
        _exoPlayer.play()
    }

    fun triggerClosePlayerSheet() {
        _shouldClosePlayerSheetInternal.value = true
    }

    fun resetClosePlayerSheetFlag() {
        _shouldClosePlayerSheetInternal.value = false
    }

    fun playPause() {
        if (_exoPlayer.isPlaying) {
            _exoPlayer.pause()
        } else {
            if (_exoPlayer.playbackState == Player.STATE_ENDED) {
                _exoPlayer.seekTo(0)
            }
            // Jika media item belum ada (misal setelah stopPlayer lalu playPause)
            if (_exoPlayer.currentMediaItem == null && currentUri != null) {
                _exoPlayer.setMediaItem(MediaItem.fromUri(currentUri!!))
                _exoPlayer.prepare()
            }
            _exoPlayer.play()
        }
    }

    fun seekTo(seconds: Float) {
        _exoPlayer.seekTo((seconds * 1000).toLong())
        _currentPositionSeconds.value = seconds
    }

    fun stopPlayer() {
        _exoPlayer.stop()
        _exoPlayer.clearMediaItems() // Hapus media item juga
        // isPlaying akan diupdate ke false oleh listener
        // stopProgressUpdates() akan dipanggil oleh listener
        _currentPositionSeconds.value = 0f
        currentUri = null
        onSongCompleteCallback = null
    }

    override fun onCleared() {
        super.onCleared()
        _exoPlayer.release()
        stopProgressUpdates()
    }

    fun toggleLoop() {
        isLooping.value = !isLooping.value
        _exoPlayer.repeatMode = if (isLooping.value) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    // Fungsi untuk mengatur output device audio (untuk API 31+)
    @OptIn(UnstableApi::class)
    @SuppressLint("MissingPermission") // Izin akan dicek secara eksplisit
    fun setAudioOutputDevice(audioDeviceInfo: AudioDeviceInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Hanya untuk Android S (API 31) ke atas
            // Izin MODIFY_AUDIO_ROUTING adalah izin 'signatureOrSystem',
            // jadi aplikasi pihak ketiga biasanya tidak bisa mendapatkannya kecuali kondisi tertentu.
            // Namun, ExoPlayer.setAudioDeviceInfo mungkin tidak selalu memerlukan izin ini secara eksplisit
            // jika perangkat output adalah bagian dari rute yang valid.
            // Kita tetap bisa mencoba memanggilnya.
            Log.d("PlayerViewModel", "Attempting to set audio output device to: ${audioDeviceInfo.productName} (ID: ${audioDeviceInfo.id}, Type: ${audioDeviceInfo.type})")
            try {
                // Pengecekan izin MODIFY_AUDIO_ROUTING mungkin tidak diperlukan/efektif di sini
                // karena aplikasi pihak ketiga umumnya tidak bisa memintanya.
                // val hasPermission = ActivityCompat.checkSelfPermission(getApplication(), Manifest.permission.MODIFY_AUDIO_ROUTING) == PackageManager.PERMISSION_GRANTED
                // if (hasPermission) {
                val success = _exoPlayer.setAudioDeviceInfo(audioDeviceInfo) // Ini adalah metode ExoPlayer
                Log.d("PlayerViewModel", "ExoPlayer.setAudioDeviceInfo success: $success")
                if (!success) {
                    Log.w("PlayerViewModel", "ExoPlayer.setAudioDeviceInfo returned false.")
                }
                // } else {
                //    Log.w("PlayerViewModel", "MODIFY_AUDIO_ROUTING permission not granted. Cannot explicitly set audio device.")
                // }
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Error setting audio device info on ExoPlayer: ${e.message}", e)
            }
        } else {
            Log.i("PlayerViewModel", "setAudioOutputDevice is only available on API 31+.")
        }
    }
}
