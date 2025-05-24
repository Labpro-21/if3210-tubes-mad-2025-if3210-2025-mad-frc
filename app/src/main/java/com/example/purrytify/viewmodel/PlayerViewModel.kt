package com.example.purrytify.viewmodel

import android.app.Application
import android.net.Uri
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.purrytify.data.AppDatabase
import com.example.purrytify.model.Song
import com.example.purrytify.model.PlayHistory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val context: Application
) : AndroidViewModel(context) {
    private val _shouldClosePlayerSheet = MutableStateFlow(false)
    val shouldClosePlayerSheet: StateFlow<Boolean> = _shouldClosePlayerSheet

    private val _exoPlayer: ExoPlayer
    // val exoPlayer: ExoPlayer get() = _exoPlayer // Jika ingin diekspos

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _progress = MutableStateFlow(0f) // Default ke 0f
    val progress: StateFlow<Float> = _progress.asStateFlow()

    val isLooping = MutableStateFlow(false)
    private var currentUri: Uri? = null
    private var currentPlayerListener: Player.Listener? = null // Untuk me-manage listener

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
                    if (duration > 0) {
                        _progress.value = (currentPosition / 1000).toFloat()
                    } else {
                        _progress.value = 0f
                    }
                } else if (_exoPlayer.playbackState == Player.STATE_ENDED && _progress.value != 0f) {
                    // Jika sudah berakhir dan progress belum direset, reset progress
                    val duration = _exoPlayer.duration
                    if (duration > 0) _progress.value = (duration/1000).toFloat() else _progress.value = 0f
                }
            }
        }
    }

    @OptIn(UnstableApi::class)
    fun prepareAndPlay(uri: Uri, onSongComplete: () -> Unit = {}) {
        Log.d("PlayerVM", "prepareAndPlay called with URI: $uri. Current URI: $currentUri, IsPlaying: ${_isPlaying.value}")

        // Jika URI sama dengan yang sedang aktif dan player sudah siap (prepared)
        if (uri == currentUri && _exoPlayer.playbackState != Player.STATE_IDLE && _exoPlayer.playbackState != Player.STATE_ENDED) {
            Log.d("PlayerVM", "URI is the same and player is already prepared. Ensuring playback.")
            if (!_exoPlayer.isPlaying) { // Jika sedang dijeda, lanjutkan
                _exoPlayer.playWhenReady = true
            }
            // Tidak perlu setMediaItem, prepare, atau menambahkan listener ulang jika hanya melanjutkan
            return
        }

        Log.d("PlayerVM", "New URI or player not ready. Full preparation for URI: $uri")

        // Hapus listener lama sebelum menambahkan yang baru
        currentPlayerListener?.let {
            _exoPlayer.removeListener(it)
            Log.d("PlayerVM", "Previous listener removed.")
        }

        currentUri = uri
        val mediaItem = MediaItem.fromUri(uri)
        _exoPlayer.setMediaItem(mediaItem) // Selalu set media item baru jika berbeda atau belum siap
        _exoPlayer.prepare()               // Selalu prepare ulang
        _exoPlayer.playWhenReady = true    // Selalu mulai/lanjutkan dari awal setelah prepare

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
                            // _isPlaying.value akan diupdate oleh onIsPlayingChanged
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
                if (isPlayingValue) { // Jika mulai bermain, update progress ticker
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
            _exoPlayer.pause() // Ini akan memicu onIsPlayingChanged(false)
            Log.d("PlayerViewModel", "Player paused.")
        } else {
            // Jika tidak sedang playing, kita ingin play.
            if (_exoPlayer.playbackState == Player.STATE_IDLE && _exoPlayer.currentMediaItem != null) {
                _exoPlayer.prepare()
            }
            _exoPlayer.play() // Ini akan memicu onIsPlayingChanged(true) jika berhasil
            Log.d("PlayerViewModel", "Player play initiated.")
        }
    }

    @OptIn(UnstableApi::class)
    fun seekTo(seconds: Float) {
        if (_exoPlayer.playbackState != Player.STATE_IDLE && _exoPlayer.duration > 0) {
            _exoPlayer.seekTo((seconds * 1000).toLong())
            _progress.value = seconds // Update progress langsung untuk responsifitas UI
        } else {
            Log.w("PlayerVM", "Cannot seek. Player not ready or duration unknown.")
        }
    }

    @OptIn(UnstableApi::class)
    fun stopPlayer() {
        Log.d("PlayerVM", "stopPlayer called.")
        _exoPlayer.stop()
        _exoPlayer.clearMediaItems() // Hapus item media juga
        _isPlaying.value = false
        _progress.value = 0f
        currentUri = null // Reset currentUri
        // Hapus listener jika ada, agar tidak ada callback yang tertinggal
        currentPlayerListener?.let {
            _exoPlayer.removeListener(it)
            currentPlayerListener = null
            Log.d("PlayerVM", "Listener removed on stopPlayer.")
        }

    }

    fun closePlayerSheet() {
        _shouldClosePlayerSheet.value = true
    }

    fun resetCloseSheetFlag() {
        _shouldClosePlayerSheet.value = false
    }

    @OptIn(UnstableApi::class)
    override fun onCleared() {
        super.onCleared()
        Log.d("PlayerVM", "onCleared: Releasing ExoPlayer.")
        _exoPlayer.release()
        currentPlayerListener?.let {
            try {
                _exoPlayer.removeListener(it) // Mungkin error jika _exoPlayer sudah direlease
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

    // Fungsi recordPlay tetap sama, pastikan AppDatabase dan PlayHistory di-import dengan benar
    @OptIn(UnstableApi::class)
    fun recordPlay(song: Song, listenedMs: Long) {
        val app: Application = getApplication()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dao = AppDatabase.getDatabase(app).songDao() // Atau PlayHistoryDao jika ada
                val playHistoryEntry = PlayHistory(
                    // id = 0, // jika autoGenerate
                    song_id = song.id,
                    user_id = song.userId,
                    played_at = Date(System.currentTimeMillis()),
                    duration_ms = listenedMs,
                )
                 dao.insertPlayHistory(playHistoryEntry)
                Log.d("PlayerVM", "Play recorded for songId: ${song.id}, duration: $listenedMs ms")
            } catch (e: Exception) {
                Log.e("PlayerVM", "Error recording play history: ${e.message}", e)
            }
        }
    }
}