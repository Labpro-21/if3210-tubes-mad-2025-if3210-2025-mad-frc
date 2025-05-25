package com.example.purrytify.viewmodel

import android.app.Application
import android.media.AudioDeviceInfo
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.purrytify.data.AppDatabase
import com.example.purrytify.model.PlayHistory
import com.example.purrytify.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val context: Application
) : AndroidViewModel(context) {
    private val _exoPlayer: ExoPlayer


    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    val currentPositionSeconds: StateFlow<Float> = _progress.asStateFlow()

    val isLooping = MutableStateFlow(false)
    private var currentUri: Uri? = null
    private var currentPlayerListener: Player.Listener? = null
    private var onSongCompleteCallback: (() -> Unit)? = null

    var onPlaybackSecondTick: (() -> Unit)? = null

    private val _shouldClosePlayerSheet = MutableStateFlow(false)
    val shouldClosePlayerSheet: StateFlow<Boolean> = _shouldClosePlayerSheet.asStateFlow()



    private val _activeAudioDevice = MutableStateFlow<AudioDeviceInfo?>(null)
    val activeAudioDevice: StateFlow<AudioDeviceInfo?> = _activeAudioDevice.asStateFlow()

    fun closePlayerSheet() {
        _shouldClosePlayerSheet.value = true
    }

    fun resetCloseSheetFlag() {
        _shouldClosePlayerSheet.value = false
    }

    init {
        _exoPlayer = ExoPlayer.Builder(context).build().apply {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()
            setAudioAttributes(audioAttributes, true)

        }

        viewModelScope.launch {
            while (true) {
                delay(500)
                if (_exoPlayer.playbackState == Player.STATE_READY && _exoPlayer.isPlaying) {
                    val currentPosition = _exoPlayer.currentPosition
                    val duration = _exoPlayer.duration
                    val currentPositionMs = _exoPlayer.currentPosition
                    _progress.value = (currentPositionMs / 1000f)
                    onPlaybackSecondTick?.invoke()
                    if (duration > 0) {
                        _progress.value = (currentPosition / 1000).toFloat()
                    } else {
                        _progress.value = 0f
                    }
                } else if (_exoPlayer.playbackState == Player.STATE_ENDED && _progress.value != 0f) {

                    val duration = _exoPlayer.duration
                    if (duration > 0) _progress.value = (duration/1000).toFloat() else _progress.value = 0f
                }
            }
        }
    }

    @OptIn(UnstableApi::class)
    fun prepareAndPlay(uri: Uri, onSongComplete: () -> Unit = {}) {
        Log.d("PlayerVM", "prepareAndPlay called with URI: $uri. Current URI: $currentUri, IsPlaying: ${_isPlaying.value}")


        if (uri == currentUri && _exoPlayer.playbackState != Player.STATE_IDLE && _exoPlayer.playbackState != Player.STATE_ENDED) {
            Log.d("PlayerVM", "URI is the same and player is already prepared. Ensuring playback.")
            if (!_exoPlayer.isPlaying) {
                _exoPlayer.playWhenReady = true
            }

            return
        }

        Log.d("PlayerVM", "New URI or player not ready. Full preparation for URI: $uri")


        currentPlayerListener?.let {
            _exoPlayer.removeListener(it)
            Log.d("PlayerVM", "Previous listener removed.")
        }

        currentUri = uri
        val mediaItem = MediaItem.fromUri(uri)
        _exoPlayer.setMediaItem(mediaItem)
        _exoPlayer.prepare()
        _exoPlayer.playWhenReady = true

        currentPlayerListener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                Log.d("PlayerVM", "onPlaybackStateChanged: $playbackState, URI: $currentUri")
                when (playbackState) {
                    Player.STATE_ENDED -> {
                        val playbackError = _exoPlayer.playerError
                        if (playbackError != null) {
                            Log.e("PlayerVM", "STATE_ENDED for $currentUri due to error: ${playbackError.message}. NOT calling onSongComplete.")
                            _isPlaying.value = false
                        } else if (_exoPlayer.repeatMode == Player.REPEAT_MODE_ONE && isLooping.value) {
                            Log.d("PlayerVM", "STATE_ENDED for $currentUri: Looping current song.")
                            _exoPlayer.seekTo(0)
                            _exoPlayer.playWhenReady = true

                        } else {
                            Log.i("PlayerVM", "STATE_ENDED for $currentUri: Song finished naturally. Calling onSongComplete.")
                            _isPlaying.value = false
                            onSongComplete()
                        }
                    }
                }
            }

            override fun onIsPlayingChanged(isPlayingValue: Boolean) {
                Log.d("PlayerVM", "onIsPlayingChanged: $isPlayingValue for $currentUri")
                _isPlaying.value = isPlayingValue
                if (isPlayingValue) {
                    val currentPosition = _exoPlayer.currentPosition
                    _progress.value = (currentPosition / 1000).toFloat()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("PlayerVM", "onPlayerError for $currentUri: ${error.message}", error)
                _isPlaying.value = false
            }
        }
        _exoPlayer.addListener(currentPlayerListener!!)
        Log.d("PlayerVM", "New listener added for URI: $uri")
    }

    @OptIn(UnstableApi::class)
    fun playPause() {
        Log.d("PlayerViewModel", "playPause() invoked. ExoPlayer isPlaying: ${_exoPlayer.isPlaying}, playWhenReady: ${_exoPlayer.playWhenReady}")

        if (_exoPlayer.currentMediaItem == null) {
            if (currentUri != null) {
                Log.d("PlayerViewModel", "No media item, but currentUri exists. Preparing: $currentUri")
                _exoPlayer.setMediaItem(MediaItem.fromUri(currentUri!!))
                _exoPlayer.prepare()
            } else {
                Log.w("PlayerViewModel", "No media item or currentUri available to play/pause.")
                return
            }
        }

        if (_exoPlayer.isPlaying) {
            _exoPlayer.pause()
            Log.d("PlayerViewModel", "Player paused.")
        } else {
            if (_exoPlayer.playbackState == Player.STATE_IDLE && _exoPlayer.currentMediaItem != null) {
                _exoPlayer.prepare()
            }
            if (_exoPlayer.playbackState == Player.STATE_ENDED) {
                _exoPlayer.seekTo(0)
            }
            _exoPlayer.play()
            Log.d("PlayerViewModel", "Player play initiated.")
        }
    }

    @OptIn(UnstableApi::class)
    fun seekTo(seconds: Float) {
        if (_exoPlayer.playbackState != Player.STATE_IDLE && _exoPlayer.duration > 0) {
            _exoPlayer.seekTo((seconds * 1000).toLong())
            _progress.value = seconds
        } else {
            Log.w("PlayerVM", "Cannot seek. Player not ready or duration unknown.")
        }
    }

    @OptIn(UnstableApi::class)
    fun stopPlayer() {
        Log.d("PlayerVM", "stopPlayer called.")
        _exoPlayer.stop()
        _exoPlayer.clearMediaItems()
        _isPlaying.value = false
        _progress.value = 0f
        currentUri = null

        currentPlayerListener?.let {
            _exoPlayer.removeListener(it)
            currentPlayerListener = null
            Log.d("PlayerVM", "Listener removed on stopPlayer.")
        }
    }

    @OptIn(UnstableApi::class)
    override fun onCleared() {
        super.onCleared()
        Log.d("PlayerVM", "onCleared: Releasing ExoPlayer.")
        _exoPlayer.release()
        currentPlayerListener?.let {
            try {
                _exoPlayer.removeListener(it)
            } catch (e: Exception) {
                Log.w("PlayerVM", "Error removing listener onCleared: ${e.message}")
            }
            currentPlayerListener = null
        }
    }

    @OptIn(UnstableApi::class)
    fun toggleLoop() {
        isLooping.value = !isLooping.value
        if (isLooping.value) {
            _exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
            Log.d("PlayerVM", "Looping enabled: REPEAT_MODE_ONE")
        } else {
            _exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
            Log.d("PlayerVM", "Looping disabled: REPEAT_MODE_OFF")
        }
    }


    @OptIn(UnstableApi::class)
    fun setPreferredAudioOutput(deviceInfo: AudioDeviceInfo?) {
        try {
            _exoPlayer.setPreferredAudioDevice(deviceInfo)


            _activeAudioDevice.value = deviceInfo
            Log.d("PlayerViewModel", "Set preferred audio output to: ${deviceInfo?.productName ?: "System Default"}. Active device state updated to this.")
        } catch (e: Exception) {
            Log.e("PlayerViewModel", "Error setting preferred audio device", e)


        }
    }



    @OptIn(UnstableApi::class)
    fun revertToDefaultAudioOutput() {
        _exoPlayer.setPreferredAudioDevice(null)
        _activeAudioDevice.value = null
        Log.d("PlayerViewModel", "Reverted to default audio output. Active device state set to null.")
    }
}