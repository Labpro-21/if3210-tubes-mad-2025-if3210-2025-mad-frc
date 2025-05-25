package com.example.purrytify.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import com.example.purrytify.MyApp
import com.example.purrytify.R
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.example.purrytify.model.Song
import com.example.purrytify.utils.MusicServiceManager
import androidx.media3.session.MediaSession
import com.example.purrytify.utils.BitmapUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.delay


class MusicService : Service() {

    private lateinit var exoPlayer: ExoPlayer
    var isPlaying: Boolean = false;
    val isLooping: Boolean
        get() = exoPlayer.repeatMode == Player.REPEAT_MODE_ONE

    val progress : Float
        get() = exoPlayer.currentPosition.toFloat();

    private var progressHandler: Handler? = null
    private var progressRunnable: Runnable? = null
    private var playlist: List<Song> = emptyList()
    private var currentSongId:Int? = null
        get() = exoPlayer.currentMediaItemIndex
    private lateinit var mediaSession: MediaSession
    private var mediaList: List<MediaItem> = emptyList()
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        Log.d("MusicService", "Service onCreate dipanggil")
        exoPlayer = ExoPlayer.Builder(this).build().apply {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()
            setAudioAttributes(audioAttributes, true)
        }
        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setId("purrytify_session")
            .build()

        val notification = NotificationCompat.Builder(this, MyApp.CHANNEL_ID)
            .setContentTitle("Purrytify")
            .setContentText("Preparing music...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setStyle(androidx.media3.session.MediaStyleNotificationHelper.MediaStyle(mediaSession))
            .build()

        startForeground(1, notification)

        exoPlayer.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                when (reason) {
                    Player.MEDIA_ITEM_TRANSITION_REASON_SEEK -> {

                        onSongChanged()
                        Log.d("PLAYER", "Transition by seek (manual skip)")
                    }
                    Player.MEDIA_ITEM_TRANSITION_REASON_AUTO -> {

                        skipNext()
                        Log.d("PLAYER", "Transition by auto (end of media)")
                    }
                }
            }
        })
        serviceScope.launch {
            updateNotification()
        }
        startProgressUpdates()
    }

    @OptIn(UnstableApi::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val hasSongId = intent?.hasExtra("SONG_ID") ?: false
        Log.d("MusicService", "hasSongId: $hasSongId")

        if (hasSongId) {
            val songId = intent.getIntExtra("SONG_ID", 0)
            Log.d("MusicService", "SongId: $songId")
            exoPlayer.seekTo(songId,0)
            playSong()
        }
        @Suppress("DEPRECATION", "UNCHECKED_CAST")
        when(intent?.action) {
            "ACTION_SET_PLAYLIST" ->{
                val songs: ArrayList<Song>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra("playlist", Song::class.java)
                } else {
                    intent.getParcelableArrayListExtra("playlist")
                }
                Log.d("MusicService", "Playlist updated: ${playlist.size} songs")

                if (songs != null) {
                    playlist = songs
                    updateMediaList()
                }
            }
            "ACTION_SET_OUTPUT" ->{
                val audioDeviceId: Int? = intent.getIntExtra("audioDevice", -1)
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)

                val selectedDevice = devices.firstOrNull { it.id == audioDeviceId }


                exoPlayer.setPreferredAudioDevice(selectedDevice)
            }
            "ACTION_PLAY_SONG" ->{

            }
            MyApp.ACTION_PLAY -> {
                if (exoPlayer.isPlaying) {
                    pause()
                } else {
                    play()
                }
                updateNotification()
            }
            MyApp.ACTION_NEXT -> {
                skipNext()
                updateNotification()
            }
            MyApp.ACTION_PREV -> {
                skipPrevious()
                updateNotification()
            }
            MyApp.ACTION_SEEK -> {
                val position = intent.getLongExtra("SEEK_POSITION_MS", 0L)
                seekTo(position)
                sendBroadcast(Intent("ACTION_PROGRESS_CHANGED").apply {
                    putExtra("PROGRESS", position)
                })
            }
            MyApp.ACTION_TOGGLE_LOOP -> {
                toggleLoop()
            }

        }

        return START_STICKY
    }



    private fun startProgressUpdates() {
        progressHandler = Handler(Looper.getMainLooper())
        progressRunnable = object : Runnable {
            override fun run() {
                if (exoPlayer.isPlaying) {

                    MusicServiceManager.updateSongProgress(progress/1000)
                    updateNotification()
                }

                progressHandler?.postDelayed(this, 1000)
            }
        }
        progressHandler?.post(progressRunnable!!)
    }



    private fun toggleLoop(){
        exoPlayer.repeatMode = if (!isLooping) {
            Player.REPEAT_MODE_ONE
        } else {
            Player.REPEAT_MODE_OFF
        }
        MusicServiceManager.updateLooping(isLooping)

    }

    private fun playSong(){
        onSongChanged()
        play()
    }

    @OptIn(UnstableApi::class)
    private suspend fun createNotification(): Notification {




        if (currentSongId == null || playlist.isEmpty()) {


            return NotificationCompat.Builder(this, MyApp.CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("No song Currently Playing")
                .setStyle(
                    androidx.media3.session.MediaStyleNotificationHelper.MediaStyle(mediaSession)
                )
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()

        }

        val song = playlist[currentSongId!!]
        val bitmap = BitmapUtils.getBitmapFromString(this, song.artworkPath!!)
            ?: BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)

        return NotificationCompat.Builder(this, MyApp.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setLargeIcon(bitmap)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setStyle(
                androidx.media3.session.MediaStyleNotificationHelper.MediaStyle(mediaSession)
            )
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

    }


    private fun updateNotification() {
        serviceScope.launch {
            val notification = createNotification()
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.notify(1, notification)
        }
    }

    private fun skipNext() {

        Log.d("ONNEXT", "currentSongId: $currentSongId")
        exoPlayer.seekToNext()
        Log.d("ONNEXT", "new currentSongId: $currentSongId")
        playSong()
    }

    private fun skipPrevious() {

        Log.d("ONPREVIOUS", "currentSongId: $currentSongId")
        exoPlayer.seekToPrevious()
        Log.d("ONPREVIOUS", " new currentSongId: $currentSongId")

        playSong()
    }

    private fun play() {
        exoPlayer.play()
        MusicServiceManager.updateIsPlaying(true)
    }

    private fun pause() {
        exoPlayer.pause()
        MusicServiceManager.updateIsPlaying(false)

    }

    private fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
        MusicServiceManager.updateIsPlaying(true)

    }


    fun onSongChanged() {
        Log.d("MusicService","SongChanged into ${playlist[exoPlayer.currentMediaItemIndex].title}")
        MusicServiceManager.updateCurrentSong(playlist[exoPlayer.currentMediaItemIndex])
    }

    fun updateMediaList(){
        mediaList = playlist.map { MediaItem.fromUri(it.audioPath) }
        exoPlayer.setMediaItems(mediaList)
        exoPlayer.prepare()

    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        progressHandler?.removeCallbacks(progressRunnable!!)
        exoPlayer.release()
        mediaSession.release()
    }

}
