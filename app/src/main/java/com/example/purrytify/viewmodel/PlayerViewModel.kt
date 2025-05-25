package com.example.purrytify.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.purrytify.MyApp
import com.example.purrytify.data.AppDatabase
import com.example.purrytify.model.PlayHistory
import com.example.purrytify.model.Song
import com.example.purrytify.service.MusicService
import com.example.purrytify.utils.MusicServiceManager
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


@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val context: Application
) : AndroidViewModel(context) {

    val isPlaying : StateFlow<Boolean> = MusicServiceManager.isPlaying

    val progress: StateFlow<Float?> = MusicServiceManager.songProgress

    var onPlaybackSecondTick: (() -> Unit)? = null

    val isLooping: StateFlow<Boolean> = MusicServiceManager.isLooping

    private val _shouldClosePlayerSheet = MutableStateFlow(false)
    val shouldClosePlayerSheet: StateFlow<Boolean> = _shouldClosePlayerSheet.asStateFlow()

    // State ini sekarang akan lebih mencerminkan perangkat yang *diminta*
    // atau perangkat default setelah event sistem (seperti headset dicabut).
    private val _activeAudioDevice = MutableStateFlow<AudioDeviceInfo?>(null)
    val activeAudioDevice: StateFlow<AudioDeviceInfo?> = _activeAudioDevice.asStateFlow()



    fun playPause() {
        val intent = Intent(context, MusicService::class.java)
        intent.action = MyApp.ACTION_PLAY
        context.startService(intent)
    }

    fun skipNext() {
        val intent = Intent(context, MusicService::class.java)
        intent.action = MyApp.ACTION_NEXT
        context.startService(intent)
    }

    fun skipPrevious() {
        val intent = Intent(context, MusicService::class.java)
        intent.action = MyApp.ACTION_PREV
        context.startService(intent)
    }

    fun stopPlayer() {
        val intent = Intent(context, MusicService::class.java)
        context.stopService(intent)
    }


    fun closePlayerSheet() {
        _shouldClosePlayerSheet.value = true
    }

    fun resetCloseSheetFlag() {
        _shouldClosePlayerSheet.value = false
    }


    fun prepareAndPlay(id: Int) {
        Log.d("PlayerViewModel", "Mengirim intent ke music service untuk memainkan lagu dengan id:$id")
        // Kirim intent ke MusicService untuk memainkan lagu
        val intent = Intent(context, MusicService::class.java).apply {
            action = MyApp.ACTION_PLAY
            putExtra("SONG_ID", id)
        }
        context.startService(intent)

    }



//    override fun onCleared() {
//        super.onCleared()
//    }

    fun seekTo(seconds: Float) {
        val intent = Intent(context, MusicService::class.java).apply {
            action = MyApp.ACTION_SEEK
            putExtra("SEEK_POSITION_MS", (seconds * 1000).toLong())
        }
        context.startService(intent)
    }

    fun toggleLoop() {

        val intent = Intent(context, MusicService::class.java).apply {
            action = MyApp.ACTION_TOGGLE_LOOP
        }

        context.startService(intent)
    }

    init {


    }
//
//    @OptIn(UnstableApi::class)
//    fun prepareAndPlay(uri: Uri) {
//        Log.d("PlayerVM", "prepareAndPlay called with URI: $uri. Current URI: $currentUri, IsPlaying: ${_isPlaying.value}")
//
//        // Jika URI sama dengan yang sedang aktif dan player sudah siap (prepared)
//        if (uri == currentUri && _exoPlayer.playbackState != Player.STATE_IDLE && _exoPlayer.playbackState != Player.STATE_ENDED) {
//            Log.d("PlayerVM", "URI is the same and player is already prepared. Ensuring playback.")
//            if (!_exoPlayer.isPlaying) { // Jika sedang dijeda, lanjutkan
//                _exoPlayer.playWhenReady = true
//            }
//            // Tidak perlu setMediaItem, prepare, atau menambahkan listener ulang jika hanya melanjutkan
//            return
//        }
//
//        Log.d("PlayerVM", "New URI or player not ready. Full preparation for URI: $uri")
//
//        // Hapus listener lama sebelum menambahkan yang baru
//        currentPlayerListener?.let {
//            _exoPlayer.removeListener(it)
//            Log.d("PlayerVM", "Previous listener removed.")
//        }
//
//        currentUri = uri
//        val mediaItem = MediaItem.fromUri(uri)
//        _exoPlayer.setMediaItem(mediaItem) // Selalu set media item baru jika berbeda atau belum siap
//        _exoPlayer.prepare()               // Selalu prepare ulang
//        _exoPlayer.playWhenReady = true    // Selalu mulai/lanjutkan dari awal setelah prepare
//
//        currentPlayerListener = object : Player.Listener {
//            override fun onPlaybackStateChanged(playbackState: Int) {
//                Log.d("PlayerVM", "onPlaybackStateChanged: $playbackState, URI: $currentUri")
//                when (playbackState) {
//                    Player.STATE_ENDED -> {
//                        val playbackError = _exoPlayer.playerError
//                        if (playbackError != null) {
//                            Log.e("PlayerVM", "STATE_ENDED for $currentUri due to error: ${playbackError.message}. NOT calling onSongComplete.")
//                            _isPlaying.value = false
//                        } else if (_exoPlayer.repeatMode == Player.REPEAT_MODE_ONE && isLooping.value) {
//                            Log.d("PlayerVM", "STATE_ENDED for $currentUri: Looping current song.")
//                            _exoPlayer.seekTo(0)
//                            _exoPlayer.playWhenReady = true
//                            // _isPlaying.value akan diupdate oleh onIsPlayingChanged
//                        } else {
//                            Log.i("PlayerVM", "STATE_ENDED for $currentUri: Song finished naturally. Calling onSongComplete.")
//                            _isPlaying.value = false
//                        }
//                    }
//                }
//            }
//
//            override fun onIsPlayingChanged(isPlayingValue: Boolean) {
//                Log.d("PlayerVM", "onIsPlayingChanged: $isPlayingValue for $currentUri")
//                _isPlaying.value = isPlayingValue
//                if (isPlayingValue) { // Jika mulai bermain, update progress ticker
//                    val currentPosition = _exoPlayer.currentPosition
//                    _progress.value = (currentPosition / 1000).toFloat()
//                }
//            }
//
//            override fun onPlayerError(error: PlaybackException) {
//                Log.e("PlayerVM", "onPlayerError for $currentUri: ${error.message}", error)
//                _isPlaying.value = false
//                if (playbackState == Player.STATE_ENDED) {
//                    _isPlaying.value = false
//                    stopProgressUpdates()
//                    onSongCompleteCallback?.invoke()
//                    if (isLooping.value) {
//                        _exoPlayer.seekTo(0)
//                        _exoPlayer.play()
//                    } else {
//                        _currentPositionSeconds.value = (_exoPlayer.duration / 1000).toFloat().coerceAtLeast(0f)
//                    }
//                }
//            }
//
//            // Listener onAudioDeviceInfoChanged dan onDeviceInfoChanged dihapus
//            // karena API yang dibutuhkan (getAudioDeviceInfo) tidak tersedia.
//        })
//        // Tidak bisa set _activeAudioDevice dari ExoPlayer di sini jika API tidak ada.
//        // Akan di-set ke null atau default saat UI membutuhkan atau saat setPreferredAudioOutput.
//        Log.d("PlayerViewModel", "Initial audio device state is null (pending selection or system default).")
//    }


//    @OptIn(UnstableApi::class)
//    fun stopPlayer() {
//        currentUri = null // Reset currentUri
//        // Hapus listener jika ada, agar tidak ada callback yang tertinggal
//        currentPlayerListener?.let {
////            _exoPlayer.removeListener(it)
//            currentPlayerListener = null
//            Log.d("PlayerVM", "Listener removed on stopPlayer.")
//        }
//
//    }

//    override fun onCleared() {
//        super.onCleared()
//        Log.d("PlayerVM", "onCleared: Releasing ExoPlayer.")
//        _exoPlayer.release()
//        currentPlayerListener?.let {
//            try {
//                _exoPlayer.removeListener(it) // Mungkin error jika _exoPlayer sudah direlease
//            } catch (e: Exception) {
//                Log.w("PlayerVM", "Error removing listener onCleared: ${e.message}")
//            }
//            currentPlayerListener = null
//        }
//    }

    // Fungsi untuk memilih perangkat output audio
    @OptIn(UnstableApi::class)
    @Suppress("DEPRECATION")
    fun setPreferredAudioOutput(deviceInfo: AudioDeviceInfo?) {
        try {
            val intent = Intent(context, MusicService::class.java).apply {
                action = "ACTION_SET_OUTPUT"
                putExtra("audioDevice", deviceInfo?.id ?: -1)
            }
            context.startService(intent)

            Log.d("PlayerViewModel", "Set preferred audio output to: ${deviceInfo?.productName ?: "System Default"}. Active device state updated to this.")
        } catch (e: Exception) {
            Log.e("PlayerViewModel", "Error setting preferred audio device", e)
        }
    }


    // Fungsi ini bisa dipanggil dari MainActivity (BroadcastReceiver)
    // saat headset dicabut, misalnya.
    @OptIn(UnstableApi::class)
    fun revertToDefaultAudioOutput() {
//        _exoPlayer.setPreferredAudioDevice(null) // Minta ExoPlayer kembali ke default
//        _activeAudioDevice.value = null // Set state kita ke null (mewakili speaker internal/default)
        Log.d("PlayerViewModel", "Reverted to default audio output. Active device state set to null.")
    }
}
