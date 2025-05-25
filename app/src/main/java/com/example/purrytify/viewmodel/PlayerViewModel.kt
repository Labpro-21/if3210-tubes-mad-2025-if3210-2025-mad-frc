//package com.example.purrytify.viewmodel
//
//import android.app.Application
//import android.content.Intent
//import android.net.Uri
//import androidx.annotation.OptIn
//import androidx.core.content.ContextCompat
//import androidx.lifecycle.AndroidViewModel
//import androidx.lifecycle.viewModelScope
//import androidx.media3.common.AudioAttributes
//import androidx.media3.common.C
//import androidx.media3.common.MediaItem
//import androidx.media3.common.Player
//import androidx.media3.common.util.Log
//import androidx.media3.common.util.UnstableApi
//import androidx.media3.exoplayer.ExoPlayer
//import com.example.purrytify.service.MusicService
//import dagger.hilt.android.lifecycle.HiltViewModel
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.launch
//import javax.inject.Inject
//
//@HiltViewModel
//class PlayerViewModel @Inject constructor(
//    private val context: Application
//) : AndroidViewModel(context) {
//    private val _shouldClosePlayerSheet = MutableStateFlow(false)
//    val shouldClosePlayerSheet: StateFlow<Boolean> = _shouldClosePlayerSheet
//
//    private val _exoPlayer = ExoPlayer.Builder(context).build()
////    val exoPlayer: ExoPlayer get() = _exoPlayer
//
//    private val _isPlaying = MutableStateFlow(false)
//    val isPlaying = _isPlaying.asStateFlow()
//
//    private val _progress = MutableStateFlow(1f)
//    val progress = _progress.asStateFlow()
//
//    val isLooping = MutableStateFlow(false)
//
//    private var currentUri: Uri? = null
//
//
//    init {
//        val audioAttributes = AudioAttributes.Builder()
//            .setUsage(C.USAGE_MEDIA)
//            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
//            .build()
//
//        _exoPlayer.setAudioAttributes(audioAttributes, true)
//        viewModelScope.launch {
//            while (true) {
//                delay(500)
//                if (_exoPlayer.isPlaying) {
//                    val currentPosition = _exoPlayer.currentPosition
//                    _progress.value = (currentPosition / 1000).toFloat()
//                }
//            }
//        }
//    }
//
//
//    fun prepareAndPlay(uri: Uri, onSongComplete: () -> Unit = {}) {
//        if (currentUri == uri) return
//        currentUri = uri
//
//        _exoPlayer.setMediaItem(MediaItem.fromUri(uri))
//        _exoPlayer.prepare()
//        _exoPlayer.play()
//        _isPlaying.value = true
//
//        _exoPlayer.addListener(object : Player.Listener {
//            override fun onPlaybackStateChanged(state: Int) {
//                if (state == Player.STATE_ENDED) {
//                    _isPlaying.value = false
//                    onSongComplete()
//                }
//            }
//        })
//    }
//
//
//    @OptIn(UnstableApi::class)
//    fun playPause() {
//        Log.d("PlayerViewModel", "playPause() invoked. isPlaying before: ${_exoPlayer.isPlaying}")
//
//        if (_exoPlayer.currentMediaItem == null) {
//            if (currentUri != null) {
//                _exoPlayer.setMediaItem(MediaItem.fromUri(currentUri!!))
//                _exoPlayer.prepare()
//            } else {
//                Log.d("PlayerViewModel", "No media item or currentUri available")
//                return
//            }
//        }
//
//        if (_exoPlayer.isPlaying) {
//            _exoPlayer.pause()
//        } else {
//            _exoPlayer.play()
//        }
//
//        _isPlaying.value = _exoPlayer.isPlaying
//        Log.d("PlayerViewModel", "playPause() finished. isPlaying now: ${_exoPlayer.isPlaying}")
//    }
//
//    fun seekTo(seconds: Float) {
////        val duration = _exoPlayer.duration.takeIf { it > 0 } ?: 1L
//        _exoPlayer.seekTo((seconds*1000).toLong())
//    }
//
//    fun stopPlayer() {
//        _exoPlayer.stop()
//        _exoPlayer.clearMediaItems()
//        _isPlaying.value = false
//        _progress.value = 0f
//        currentUri = null
////        onCleared()
//    }
//
//    fun closePlayerSheet() {
//        _shouldClosePlayerSheet.value = true
//    }
//
//    fun resetCloseSheetFlag() {
//        _shouldClosePlayerSheet.value = false
//    }
//
//    override fun onCleared() {
//        super.onCleared()
//        _exoPlayer.release()
//    }
//
//    fun toggleLoop(){
//        isLooping.value = !isLooping.value
//        if (isLooping.value == true){
//            _exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
//        }else{
//            _exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
//        }
//
//
//    }
//
//
//
//}

package com.example.purrytify.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.example.purrytify.MyApp
import com.example.purrytify.model.Song
import com.example.purrytify.service.MusicService
import com.example.purrytify.utils.MusicServiceManager
import android.media.AudioDeviceInfo // Pastikan import ini benar
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
    private val _shouldClosePlayerSheet = MutableStateFlow(false)
    val shouldClosePlayerSheet: StateFlow<Boolean> = _shouldClosePlayerSheet


    val isPlaying : StateFlow<Boolean> = MusicServiceManager.isPlaying

    val progress: StateFlow<Float?> = MusicServiceManager.songProgress
    private val _exoPlayer: ExoPlayer
    // val exoPlayer: ExoPlayer get() = _exoPlayer // Jika ingin diekspos


    private val _currentPositionSeconds = MutableStateFlow(0f)
    val currentPositionSeconds: StateFlow<Float> = _currentPositionSeconds.asStateFlow()

    val isLooping = MutableStateFlow(false)
    private var currentUri: Uri? = null
    private var currentPlayerListener: Player.Listener? = null // Untuk me-manage listener    private var progressUpdateJob: Job? = null
    private var onSongCompleteCallback: (() -> Unit)? = null

    var onPlaybackSecondTick: (() -> Unit)? = null

    val isLooping: StateFlow<Boolean> = MusicServiceManager.isLooping

    private val _shouldClosePlayerSheet = MutableStateFlow(false)
    val shouldClosePlayerSheet: StateFlow<Boolean> = _shouldClosePlayerSheet.asStateFlow()

    // State ini sekarang akan lebih mencerminkan perangkat yang *diminta*
    // atau perangkat default setelah event sistem (seperti headset dicabut).
    private val _activeAudioDevice = MutableStateFlow<AudioDeviceInfo?>(null)
    val activeAudioDevice: StateFlow<AudioDeviceInfo?> = _activeAudioDevice.asStateFlow()

    val currentPositionSeconds: StateFlow<Float> = _progress.asStateFlow()


    fun playPause() {
        val intent = Intent(appContext, MusicService::class.java)
        intent.action = MyApp.ACTION_PLAY
        appContext.startService(intent)
    }

    fun skipNext() {
        val intent = Intent(appContext, MusicService::class.java)
        intent.action = MyApp.ACTION_NEXT
        appContext.startService(intent)
    }

    fun skipPrevious() {
        val intent = Intent(appContext, MusicService::class.java)
        intent.action = MyApp.ACTION_PREV
        appContext.startService(intent)
    }

    fun stopPlayer() {
        val intent = Intent(appContext, MusicService::class.java)
        appContext.stopService(intent)
    }


    fun closePlayerSheet() {
        _shouldClosePlayerSheet.value = true
    }

    fun resetCloseSheetFlag() {
        _shouldClosePlayerSheet.value = false
    }


    fun prepareAndPlay(id: Int) {

        // Kirim intent ke MusicService untuk memainkan lagu
        val intent = Intent(appContext, MusicService::class.java).apply {
            action = MyApp.ACTION_PLAY
            putExtra("SONG_ID", id)
        }
        appContext.startService(intent)

    }



    override fun onCleared() {
        super.onCleared()
    }

    fun seekTo(seconds: Float) {
        val intent = Intent(appContext, MusicService::class.java).apply {
            action = MyApp.ACTION_SEEK
            putExtra("SEEK_POSITION_MS", (seconds * 1000).toLong())
        }
        appContext.startService(intent)
    }

    fun toggleLoop() {

        val intent = Intent(appContext, MusicService::class.java).apply {
            action = MyApp.ACTION_TOGGLE_LOOP
        }

        appContext.startService(intent)
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
            _progress.value = seconds // Update progress langsung untuk responsifitas UI
        } else {
            Log.w("PlayerVM", "Cannot seek. Player not ready or duration unknown.")
        }
        _exoPlayer.seekTo((seconds * 1000).toLong())
        _currentPositionSeconds.value = seconds
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

    @OptIn(UnstableApi::class)
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
