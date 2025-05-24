package com.example.purrytify.ui

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.purrytify.viewmodel.PlayerViewModel
import com.example.purrytify.viewmodel.SongViewModel
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongSettingsModal(
    songViewModel: SongViewModel,
    playerViewModel: PlayerViewModel,
    isOnlineSong: Boolean = false
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()
    var showSheet by remember { mutableStateOf(false) }

    IconButton(onClick = { showSheet = true }) {
        Icon(Icons.Default.MoreVert, contentDescription = "More Options")
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                if (!isOnlineSong) {
                    // ✅ Options for LOCAL songs
                    val currentSong by songViewModel.current_song.collectAsState()
                    EditSongOption(songViewModel, currentSong)
                    ConfirmDelete(songViewModel, playerViewModel)
                } else {
                    // ✅ Options for ONLINE songs
                    val currentSong by songViewModel.current_song.collectAsState()
                    DownloadSongOption(context, songViewModel, currentSong)
                    ShareSongOption(context, currentSong)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ✅ Download option for online songs
@Composable
private fun DownloadSongOption(
    context: Context,
    songViewModel: SongViewModel,
    currentSong: com.example.purrytify.model.Song?
) {
    var isDownloading by remember { mutableStateOf(false) }
    var downloadComplete by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                currentSong?.let { song ->
                    downloadSong(context, song, songViewModel) {
                        isDownloading = false
                        downloadComplete = true
                    }
                    isDownloading = true
                }
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Download,
            contentDescription = "Download Song"
        )
        Spacer(modifier = Modifier.width(16.dp))

        if (isDownloading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Downloading...", style = MaterialTheme.typography.bodyLarge)
        } else if (downloadComplete) {
            Text("Downloaded ✓", style = MaterialTheme.typography.bodyLarge)
        } else {
            Text("Download Song", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

// ✅ Share option for online songs
@Composable
private fun ShareSongOption(
    context: Context,
    currentSong: com.example.purrytify.model.Song?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                currentSong?.let { song ->
                    shareSong(context, song)
                }
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Share,
            contentDescription = "Share Song"
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text("Share Song", style = MaterialTheme.typography.bodyLarge)
    }
}

// ✅ Edit option for local songs
@Composable
private fun EditSongOption(
    songViewModel: SongViewModel,
    currentSong: com.example.purrytify.model.Song?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // TODO: Navigate to edit screen
                currentSong?.let {
                    // Implement edit functionality
                }
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "Edit Song"
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text("Edit Song", style = MaterialTheme.typography.bodyLarge)
    }
}

// ✅ Download function implementation
private fun downloadSong(
    context: Context,
    song: com.example.purrytify.model.Song,
    songViewModel: SongViewModel,
    onComplete: () -> Unit
) {
    try {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val req = DownloadManager.Request(Uri.parse(song.audioPath))
            .setTitle("${song.title} - ${song.artist}")
            .setDescription("Downloading song...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_MUSIC,
                "${song.title} - ${song.artist}.mp3"
            )
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadId = dm.enqueue(req)

        // ✅ Listen for download completion
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    dm.query(query).use { cursor ->
                        if (cursor.moveToFirst()) {
                            val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                            if (columnIndex >= 0) {
                                val status = cursor.getInt(columnIndex)
                                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                    // ✅ Save to local database
                                    val localUriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                                    if (localUriIndex >= 0) {
                                        val localUri = cursor.getString(localUriIndex)
                                        val localSong = song.copy(
                                            id = 0, // Auto-generate new ID
                                            audioPath = localUri,
                                            addedDate = Date(),
                                            lastPlayed = null
                                        )
                                        songViewModel.addSong(localSong)
                                    }
                                    onComplete()
                                }
                            }
                        }
                    }
                    ctx.unregisterReceiver(this)
                }
            }
        }

        context.registerReceiver(
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )

    } catch (e: Exception) {
        e.printStackTrace()
        onComplete()
    }
}

// ✅ Share function implementation
private fun shareSong(context: Context, song: com.example.purrytify.model.Song) {
    val shareText = "🎵 Check out this song: ${song.title} by ${song.artist}\n\n${song.audioPath}"
    val intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, shareText)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(intent, "Share Song"))
}

@Composable
private fun ConfirmDelete(
    songViewModel: SongViewModel,
    playerViewModel: PlayerViewModel
) {
    var showDialog by remember { mutableStateOf(false) }
    val currentSong by songViewModel.current_song.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Delete, contentDescription = "Delete Button")
        Spacer(modifier = Modifier.width(16.dp))
        Text("Delete Song", style = MaterialTheme.typography.bodyLarge)
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    currentSong?.let {
                        songViewModel.deleteSong(it)
                        playerViewModel.stopPlayer()
                        playerViewModel.closePlayerSheet()
                    }
                    showDialog = false
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Confirm Delete") },
            text = {
                Text("Are you sure you want to delete this song?")
            }
        )
    }
}