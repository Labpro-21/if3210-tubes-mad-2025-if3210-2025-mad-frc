package com.example.purrytify.viewmodel

import android.app.Application
import android.media.AudioDeviceInfo // Pastikan import ini benar
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi

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

    private val _shouldClosePlayerSheet = MutableStateFlow(false)
    val shouldClosePlayerSheet: StateFlow<Boolean> = _shouldClosePlayerSheet.asStateFlow()

    // State ini sekarang akan lebih mencerminkan perangkat yang *diminta*
    // atau perangkat default setelah event sistem (seperti headset dicabut).
    private val _activeAudioDevice = MutableStateFlow<AudioDeviceInfo?>(null)
    val activeAudioDevice: StateFlow<AudioDeviceInfo?> = _activeAudioDevice.asStateFlow()

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
                    _isPlaying.value = false
                    stopProgressUpdates()
                    onSongCompleteCallback?.invoke()
                    if (isLooping.value) {
                        _exoPlayer.seekTo(0)
                        _exoPlayer.play()
                    } else {
                        _currentPositionSeconds.value = (_exoPlayer.duration / 1000).toFloat().coerceAtLeast(0f)
                    }
                }
            }

            // Listener onAudioDeviceInfoChanged dan onDeviceInfoChanged dihapus
            // karena API yang dibutuhkan (getAudioDeviceInfo) tidak tersedia.
        })
        // Tidak bisa set _activeAudioDevice dari ExoPlayer di sini jika API tidak ada.
        // Akan di-set ke null atau default saat UI membutuhkan atau saat setPreferredAudioOutput.
        Log.d("PlayerViewModel", "Initial audio device state is null (pending selection or system default).")
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
        // Setelah prepare, kita tidak bisa langsung tahu device sebenarnya dari ExoPlayer
        // Jika _activeAudioDevice masih null, UI bisa menganggapnya speaker internal.
        Log.d("PlayerViewModel", "Prepared and playing. Assumed audio device: ${_activeAudioDevice.value?.productName ?: "System Default"}")
    }

    fun playPause() {
        if (_exoPlayer.isPlaying) {
            _exoPlayer.pause()
        } else {
            if (_exoPlayer.playbackState == Player.STATE_ENDED) {
                _exoPlayer.seekTo(0)
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
        _currentPositionSeconds.value = 0f
        currentUri = null
        onSongCompleteCallback = null
        // Saat stop, kita bisa asumsikan kembali ke default atau biarkan state _activeAudioDevice apa adanya
        // sampai ada interaksi baru. Untuk konsistensi, bisa di-set ke null.
        // _activeAudioDevice.value = null
        Log.d("PlayerViewModel", "Player stopped. Assumed audio device: ${_activeAudioDevice.value?.productName ?: "System Default"}")
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

    // Fungsi untuk memilih perangkat output audio
    @OptIn(UnstableApi::class)
    fun setPreferredAudioOutput(deviceInfo: AudioDeviceInfo?) {
        try {
            _exoPlayer.setPreferredAudioDevice(deviceInfo)
            // Karena kita tidak bisa mendapatkan konfirmasi dari ExoPlayer melalui listener API lama,
            // kita perbarui _activeAudioDevice secara optimistis.
            _activeAudioDevice.value = deviceInfo
            Log.d("PlayerViewModel", "Set preferred audio output to: ${deviceInfo?.productName ?: "System Default"}. Active device state updated to this.")
        } catch (e: Exception) {
            Log.e("PlayerViewModel", "Error setting preferred audio device", e)
            // Jika gagal, mungkin _activeAudioDevice harus di-reset ke state sebelumnya atau null.
            // Untuk sekarang, kita biarkan _activeAudioDevice seperti yang di-set (optimis).
        }
    }

    // Fungsi ini bisa dipanggil dari MainActivity (BroadcastReceiver)
    // saat headset dicabut, misalnya.
    @OptIn(UnstableApi::class)
    fun revertToDefaultAudioOutput() {
        _exoPlayer.setPreferredAudioDevice(null) // Minta ExoPlayer kembali ke default
        _activeAudioDevice.value = null // Set state kita ke null (mewakili speaker internal/default)
        Log.d("PlayerViewModel", "Reverted to default audio output. Active device state set to null.")
    }
}