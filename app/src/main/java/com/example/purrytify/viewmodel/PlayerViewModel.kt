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

        val intent = Intent(context, MusicService::class.java).apply {
            action = MyApp.ACTION_PLAY
            putExtra("SONG_ID", id)
        }
        context.startService(intent)

    }

    fun playSingleSong(song: Song){
        val songList = listOf<Song>(song)
        val intent = Intent(context, MusicService::class.java).apply {
            action = "ACTION_SET_PLAYLIST"
            putParcelableArrayListExtra("playlist", ArrayList(songList))
            Log.d("PlayerViewModel", "Sending Playlist of ${ArrayList(songList).size} length")
        }
        context.startService(intent)

        val intent2 = Intent(context, MusicService::class.java).apply {
            action = MyApp.ACTION_PLAY
            putExtra("SONG_ID", 0)
            Log.d("PlayerViewModel", "Playing ${ArrayList(songList).size} length")
        }
        context.startService(intent2)

    }

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




    @OptIn(UnstableApi::class)
    fun revertToDefaultAudioOutput() {


        Log.d("PlayerViewModel", "Reverted to default audio output. Active device state set to null.")
    }
}
