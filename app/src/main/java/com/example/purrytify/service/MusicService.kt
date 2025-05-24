package com.example.purrytify.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import android.widget.RemoteViews
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import com.example.purrytify.MyApp
import com.example.purrytify.R
import androidx.media3.exoplayer.ExoPlayer
import androidx.core.net.toUri
import androidx.media3.common.Player
import coil.imageLoader
import coil.request.ImageRequest
import com.example.purrytify.model.Song
import com.example.purrytify.utils.MusicServiceManager

class MusicService : Service() {

    private lateinit var exoPlayer: ExoPlayer
    var isPlaying: Boolean = false;
    val isLooping: Boolean
        get() = exoPlayer.repeatMode == Player.REPEAT_MODE_ONE

    var progress : Float = 0f;
    private var progressHandler: Handler? = null
    private var progressRunnable: Runnable? = null
    private var playlist: List<Song> = emptyList()
    private var currentSongId:Int? = 0;

    override fun onCreate() {
        super.onCreate()
        exoPlayer = ExoPlayer.Builder(this).build()
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    skipNext()
                    sendBroadcast(Intent("ACTION_SONG_COMPLETE"))
                }
            }
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val hasSongId = intent?.hasExtra("SONG_ID") ?: false
        Log.d("MusicService", "hasSongId: $hasSongId")

        if (hasSongId) {
            val songId = intent.getIntExtra("SONG_ID", 0)
            Log.d("MusicService", "SongId: $songId")

            currentSongId = songId
            playSong()
        }

        when(intent?.action) {
            "ACTION_SET_PLAYLIST" ->{
                @Suppress("DEPRECATION")
                val songs: ArrayList<Song>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra("playlist", Song::class.java)
                } else {
                    intent.getParcelableArrayListExtra("playlist") // deprecated, tapi masih bisa dipakai untuk API < 33
                }
                Log.d("MusicService", "Playlist updated: ${playlist.size} songs")

                if (songs != null) {
                    playlist = songs
                }
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

        startProgressUpdates()
        return START_STICKY
    }

    private fun startProgressUpdates() {
        progressHandler = Handler(Looper.getMainLooper())
        progressRunnable = object : Runnable {
            override fun run() {
                if (exoPlayer.isPlaying) {
                    val intent = Intent("ACTION_UPDATE_PROGRESS")
                    intent.putExtra("PROGRESS", exoPlayer.currentPosition)
//                    Log.d("MusicService", "current position:${exoPlayer.currentPosition}")
                    sendBroadcast(intent)
                }
                progressHandler?.postDelayed(this, 500)
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
        sendBroadcast(Intent("ACTION_LOOP_TOGGLED").apply {
            putExtra("IS_LOOPING", isLooping)
        })
    }

    private fun playSong(){
        if (playlist.isEmpty()) {
            Log.e("MusicService", "Playlist is empty, cannot play song.")
            return
        }

        if (currentSongId == null || currentSongId !in playlist.indices) {
            Log.e("MusicService", "Invalid currentSongId: $currentSongId")
            return
        }
        Log.d("MusicService", "playing song currentSongId: $currentSongId")
        val currentSong = playlist[currentSongId!!]
        val songUri = currentSong.audioPath.toUri()
        val mediaItem = MediaItem.fromUri(songUri)
        progress = 0f
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        startForeground(1, createNotification())

        exoPlayer.play()
        isPlaying = true
        onSongChanged(currentSong)
    }

    private fun createNotification(): Notification {
        Log.d("MusicService","CreatingNotification")
        val song = playlist[currentSongId!!]
//        val customView = RemoteViews(packageName, R.layout.notification_player)
//        // Set teks dan gambar
//        customView.setTextViewText(R.id.notification_title, song.title)
//        customView.setTextViewText(R.id.notification_artist, song.artist)
//        customView.setImageViewResource(R.id.notification_play_pause, if (exoPlayer.isPlaying) R.drawable.ic_pause else R.drawable.ic_play)



        val playPauseIcon = if (exoPlayer.isPlaying) R.drawable.ic_pause else R.drawable.ic_play

        val playPauseIntent = Intent(this, MusicService::class.java).apply {
            action = MyApp.ACTION_PLAY
        }
        val playPausePendingIntent = PendingIntent.getService(
            this, 0, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = Intent(this, MusicService::class.java).apply {
            action = MyApp.ACTION_NEXT
        }
        val nextPendingIntent = PendingIntent.getService(
            this, 1, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = Intent(this, MusicService::class.java).apply {
            action = MyApp.ACTION_PREV
        }
        val prevPendingIntent = PendingIntent.getService(
            this, 2, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mediaSession = MediaSessionCompat(this, "Purrytify")
        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
            .setMediaSession(mediaSession.sessionToken)
            .setShowActionsInCompactView(0, 1, 2)


        return NotificationCompat.Builder(this, MyApp.CHANNEL_ID)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setSmallIcon(R.mipmap.ic_launcher)
//            .setCustomContentView(customView)
            .addAction(R.drawable.ic_previous, "Previous", prevPendingIntent)
            .addAction(playPauseIcon, "Play/Pause", playPausePendingIntent)
            .addAction(R.drawable.ic_next, "Next", nextPendingIntent)
            .setOnlyAlertOnce(true)
            .setStyle(mediaStyle)
            .build()
    }

    private fun updateNotification() {
        val notification = createNotification()
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(1, notification)
    }

    private fun skipNext() {
        // contoh: skip ke lagu berikutnya
        Log.d("ONNEXT", "currentSongId: $currentSongId")

        currentSongId?.let {
            if (it < playlist.size - 1) {
                currentSongId = it + 1
            }else{
                currentSongId = 0
            }
        }
        Log.d("ONNEXT", "new currentSongId: $currentSongId")
        notifyCurrentSongChanged()
        playSong()
    }

    private fun skipPrevious() {
        // contoh: kembali ke lagu sebelumnya
        Log.d("ONPREVIOUS", "currentSongId: $currentSongId")

        currentSongId?.let {
            if (it > 0) {
                currentSongId = it - 1
            }else{
                currentSongId = playlist.size - 1
            }
        }
        Log.d("ONPREVIOUS", " new currentSongId: $currentSongId")

        playSong()
    }

    private fun notifyPlayingStateChanged(isPlaying: Boolean) {
        val intent = Intent("ACTION_PLAYING_STATE_CHANGED").apply {
            putExtra("IS_PLAYING", isPlaying)
            putExtra("CURRENT_POSITION", exoPlayer.currentPosition)
        }
        sendBroadcast(intent)
    }

    private fun play() {
        exoPlayer.play()
        notifyPlayingStateChanged(true)
    }

    private fun pause() {
        exoPlayer.pause()
        notifyPlayingStateChanged(false)
    }

    private fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
        notifyPlayingStateChanged(exoPlayer.isPlaying)
    }

    private fun notifyCurrentSongChanged(){
        Log.d("ONCHANGE", " notifying change into: ${playlist[currentSongId!!].title}")
        val intent = Intent("ACTION_CURRENT_SONG_CHANGED").apply{
            putExtra("current_song", playlist[currentSongId!!])
        }
        sendBroadcast(intent)

    }

    fun onSongChanged(newSong: Song) {
        MusicServiceManager.updateCurrentSong(newSong)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        progressHandler?.removeCallbacks(progressRunnable!!)
        exoPlayer.release()
    }

}
