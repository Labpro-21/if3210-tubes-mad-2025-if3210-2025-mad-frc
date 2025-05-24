package com.example.purrytify.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import com.example.purrytify.MyApp
import com.example.purrytify.R
import androidx.media3.exoplayer.ExoPlayer
import androidx.core.net.toUri
import androidx.media3.common.Player

class MusicService : Service() {

    private lateinit var exoPlayer: ExoPlayer
    var isPlaying: Boolean = false;
    var isLooping: Boolean = false;
    var progress : Float = 0f;
    private var progressHandler: Handler? = null
    private var progressRunnable: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        exoPlayer = ExoPlayer.Builder(this).build()

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val songUriString = intent?.getStringExtra("SONG_URI")
        if (songUriString != null) {
            val uri = songUriString.toUri()
            playFromUri(uri)
        }

        when(intent?.action) {
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
                    Log.d("MusicService", "current position:${exoPlayer.currentPosition}")
                    sendBroadcast(intent)
                }
                progressHandler?.postDelayed(this, 500)
            }
        }
        progressHandler?.post(progressRunnable!!)
    }

    private fun toggleLoop(){
        isLooping = !isLooping
        exoPlayer.repeatMode = if (isLooping) {
            Player.REPEAT_MODE_ONE
        } else {
            Player.REPEAT_MODE_OFF
        }
        sendBroadcast(Intent("ACTION_LOOP_TOGGLED").apply {
            putExtra("IS_LOOPING", isLooping)
        })
    }

    private fun playFromUri(uri: Uri) {
        val mediaItem = MediaItem.fromUri(uri)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        startForeground(1, createNotification())

        exoPlayer.play()
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    sendBroadcast(Intent("ACTION_SONG_COMPLETE"))
                }
            }
        })
        isPlaying = true
    }


    private fun createNotification(): Notification {
        Log.d("MusicService","CreatingNotification")
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
            .setContentTitle("Purrytify")
            .setContentText("Playing music")
            .setSmallIcon(R.mipmap.ic_launcher)
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
    }

    private fun skipPrevious() {
        // contoh: kembali ke lagu sebelumnya
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



    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer.release()
    }
}
