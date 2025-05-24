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
import com.example.purrytify.service.MusicService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application
    private val _shouldClosePlayerSheet = MutableStateFlow(false)
    val shouldClosePlayerSheet: StateFlow<Boolean> = _shouldClosePlayerSheet


    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _progress = MutableStateFlow(1f)
    val progress = _progress.asStateFlow()

    val isLooping = MutableStateFlow(false)

    private var currentUri: Uri? = null
    private var onSongCompleteCallback: (() -> Unit)? = null

    private val songCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "ACTION_SONG_COMPLETE") {
                _isPlaying.value = false
                onSongCompleteCallback?.invoke()
            }
        }
    }

    private val isLoopingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "ACTION_LOOP_TOGGLED") {
                val loopingStatus = intent.getBooleanExtra("IS_LOOPING", false)
                isLooping.value = loopingStatus
            }
        }
    }


    private val progressReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "ACTION_UPDATE_PROGRESS") {
                val newProgress = intent.getLongExtra("PROGRESS",0L)
                Log.d("PlayerViewModel", "newProgress:${newProgress}")
                _progress.value = newProgress.toFloat()
            }
        }
    }



    init {
        val filter = IntentFilter("ACTION_SONG_COMPLETE")
        ContextCompat.registerReceiver(
            appContext,
            songCompleteReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        val filter2 = IntentFilter("ACTION_LOOP_TOGGLED")
        ContextCompat.registerReceiver(
            appContext,
            isLoopingReceiver,
            filter2,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        val filter3 = IntentFilter("ACTION_UPDATE_PROGRESS")
        ContextCompat.registerReceiver(
            appContext,
            progressReceiver,
            filter3,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

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

    fun prepareAndPlay(uri: Uri, onSongComplete: () -> Unit = {}) {
        currentUri = uri
        onSongCompleteCallback = onSongComplete

        // Kirim intent ke MusicService untuk memainkan lagu
        val intent = Intent(appContext, MusicService::class.java).apply {
            action = MyApp.ACTION_PLAY
            putExtra("SONG_URI", uri.toString())
        }
        appContext.startService(intent)

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
        appContext.unregisterReceiver(songCompleteReceiver)
        appContext.unregisterReceiver(isLoopingReceiver)
        appContext.unregisterReceiver(progressReceiver)


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
    }



}

