package com.example.purrytify.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import androidx.media3.common.util.Log
import com.example.purrytify.model.Song
import com.example.purrytify.viewmodel.SongViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.net.URL
import java.util.Date

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
fun downloadSong(
    context: Context,
    songToDownload: Song, // Mengganti nama parameter agar lebih jelas
    songViewModel: SongViewModel,
    sessionManager: SessionManager,
    onComplete: () -> Unit
) {
    val tag = "DownloadSong"
    Log.d(tag, "=== DOWNLOAD PROCESS STARTED ===")
    Log.d(tag, "Song to download: ${songToDownload.title} by ${songToDownload.artist}")
    Log.d(tag, "Original audioPath: ${songToDownload.audioPath}")
    Log.d(tag, "Original artworkPath: ${songToDownload.artworkPath}")
    Log.d(tag, "User ID: ${sessionManager.getUserId()}")

    // Validasi URL audio
    if (!songToDownload.audioPath.startsWith("http")) {
        Log.e(tag, "ERROR: audioPath is not a valid URL: ${songToDownload.audioPath}")
        onComplete()
        return
    }

    // Validasi User ID
    if (sessionManager.getUserId() <= 0) {
        Log.e(tag, "ERROR: Invalid user ID: ${sessionManager.getUserId()}")
        onComplete()
        return
    }

    try {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        Log.d(tag, "DownloadManager service obtained.")

        // Membuat nama file yang aman untuk audio
        val safeTitleAudio = songToDownload.title.replace(Regex("[^a-zA-Z0-9\\s]"), "_").take(50)
        val safeArtistAudio = songToDownload.artist.replace(Regex("[^a-zA-Z0-9\\s]"), "_").take(50)
        val audioFileName = "${safeTitleAudio}_${safeArtistAudio}.mp3"
        Log.d(tag, "Safe audio filename: $audioFileName")

        val request = DownloadManager.Request(Uri.parse(songToDownload.audioPath))
            .setTitle("${songToDownload.title} - ${songToDownload.artist}")
            .setDescription("Downloading song...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_MUSIC,
                audioFileName
            )
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadId = downloadManager.enqueue(request)
        Log.d(tag, "Song download enqueued with ID: $downloadId")

        val receiver = object : BroadcastReceiver() {
            @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
            override fun onReceive(ctx: Context, intent: Intent) {
                Log.d(tag, "BroadcastReceiver - Action: ${intent.action}")
                val receivedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                Log.d(tag, "BroadcastReceiver - Received download ID: $receivedId, Expected ID: $downloadId")

                if (receivedId == downloadId) {
                    Log.d(tag, "Download ID matches. Processing song download result...")
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    var successful = false
                    var localSongUriString: String? = null

                    try {
                        downloadManager.query(query).use { cursor ->
                            if (cursor != null && cursor.moveToFirst()) {
                                val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                                val localUriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                                val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)

                                if (statusIndex != -1 && localUriIndex != -1) {
                                    val status = cursor.getInt(statusIndex)
                                    val reason = if (reasonIndex != -1) cursor.getInt(reasonIndex) else 0
                                    localSongUriString = cursor.getString(localUriIndex)

                                    when (status) {
                                        DownloadManager.STATUS_SUCCESSFUL -> {
                                            Log.i(tag, "Song download SUCCESSFUL. Local URI: $localSongUriString")
                                            successful = true
                                        }
                                        DownloadManager.STATUS_FAILED -> {
                                            Log.e(tag, "Song download FAILED. Reason: $reason, Local URI: $localSongUriString")
                                        }
                                        DownloadManager.STATUS_PAUSED -> Log.w(tag, "Song download PAUSED. Reason: $reason")
                                        DownloadManager.STATUS_PENDING -> Log.d(tag, "Song download PENDING.")
                                        DownloadManager.STATUS_RUNNING -> Log.d(tag, "Song download RUNNING.")
                                        else -> Log.w(tag, "Unknown song download status: $status")
                                    }
                                } else {
                                    Log.e(tag, "ERROR: Critical column indices not found in cursor.")
                                }
                            } else {
                                Log.e(tag, "ERROR: Cursor is null or empty for song download query.")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "ERROR processing song download cursor: ${e.message}", e)
                    }

                    // Proses artwork dan simpan ke database jika lagu berhasil diunduh
                    if (successful && localSongUriString != null) {
                        CoroutineScope(Dispatchers.IO).launch {
                            var finalArtworkPath: String? = songToDownload.artworkPath // Default ke path asli/remote
                            if (!songToDownload.artworkPath.isNullOrEmpty() && songToDownload.artworkPath.startsWith("http")) {
                                Log.d(tag, "Attempting to download artwork from: ${songToDownload.artworkPath}")
                                try {
                                    val safeTitleArtwork = songToDownload.title.replace(Regex("[^a-zA-Z0-9\\s]"), "_").take(50)
                                    // Ekstensi mungkin berbeda, idealnya periksa dari URL atau Content-Type
                                    val artworkFileName = "cover_${safeTitleArtwork}.jpg"
                                    val artworkFile = File(
                                        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), // Simpan di Pictures
                                        artworkFileName
                                    )
                                    URL(songToDownload.artworkPath).openStream().use { input ->
                                        artworkFile.outputStream().use { output ->
                                            input.copyTo(output)
                                        }
                                    }
                                    finalArtworkPath = artworkFile.absolutePath
                                    Log.i(tag, "Artwork downloaded successfully to: $finalArtworkPath")
                                } catch (e: Exception) {
                                    Log.e(tag, "ERROR downloading artwork: ${e.message}", e)
                                    // Biarkan finalArtworkPath menggunakan URL asli jika download gagal
                                }
                            } else {
                                Log.d(tag, "Artwork path is null, empty, or not an HTTP URL. Skipping artwork download.")
                            }

                            val newDownloadedSong = Song(
                                id = 0, // ID akan di-generate oleh Room
                                title = songToDownload.title,
                                artist = songToDownload.artist,
                                duration = songToDownload.duration, // Asumsikan durasi sudah ada di objek songToDownload
                                artworkPath = finalArtworkPath,
                                audioPath = localSongUriString!!, // Non-null karena 'successful' true
                                lastPlayed = null,
                                addedDate = Date(),
                                liked = false, // Default untuk lagu baru
                                userId = sessionManager.getUserId()
                            )
                            Log.d(tag, "Preparing to add new song to database: $newDownloadedSong")
                            songViewModel.addSong(newDownloadedSong)
                            Log.i(tag, "New song '${newDownloadedSong.title}' added to database.")
                            onComplete()
                        }
                    } else {
                        // Jika download lagu tidak berhasil
                        Log.w(tag, "Song download was not successful or local URI is null. Calling onComplete.")
                        onComplete()
                    }

                    // Unregister receiver
                    try {
                        ctx.applicationContext.unregisterReceiver(this)
                        Log.d(tag, "BroadcastReceiver unregistered successfully.")
                    } catch (e: IllegalArgumentException) {
                        Log.e(tag, "ERROR unregistering receiver (already unregistered?): ${e.message}", e)
                    } catch (e: Exception) {
                        Log.e(tag, "ERROR unregistering receiver: ${e.message}", e)
                    }
                } else {
                    Log.d(tag, "BroadcastReceiver - Download ID mismatch. Ignoring.")
                }
            }
        }

        val intentFilter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        try {
            // Gunakan applicationContext untuk mendaftarkan receiver
            context.applicationContext.registerReceiver(receiver, intentFilter, Context.RECEIVER_EXPORTED)
            Log.d(tag, "BroadcastReceiver registered for ACTION_DOWNLOAD_COMPLETE.")
        } catch (e: Exception) {
            Log.e(tag, "ERROR registering receiver: ${e.message}", e)
            onComplete() // Panggil onComplete jika registrasi gagal
            return
        }
        Log.d(tag, "=== SONG DOWNLOAD SETUP COMPLETE ===")

    } catch (e: Exception) {
        Log.e(tag, "CRITICAL ERROR in downloadOnlineSong setup: ${e.message}", e)
        e.printStackTrace()
        onComplete()
    }
}