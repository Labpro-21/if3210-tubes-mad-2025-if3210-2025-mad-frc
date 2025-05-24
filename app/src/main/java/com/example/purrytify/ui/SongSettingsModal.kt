package com.example.purrytify.ui

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.media3.common.util.Log
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
import androidx.media3.common.util.UnstableApi
import com.example.purrytify.model.Song
import com.example.purrytify.utils.SessionManager
import com.example.purrytify.viewmodel.PlayerViewModel
import com.example.purrytify.viewmodel.SongViewModel
import com.example.purrytify.utils.downloadSong
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
    val session = remember<SessionManager> { SessionManager(context) }

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
                    val currentSong by songViewModel.current_song.collectAsState()
                    InsertSongPopUp(songViewModel,currentSong)
                    ConfirmDelete(songViewModel, playerViewModel)
                } else {
                    val currentSong by songViewModel.current_song.collectAsState()
                    DownloadSongOption(context, songViewModel,session, currentSong)
                    ShareSongOption(context, currentSong)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun DownloadSongOption(
    context: Context,
    songViewModel: SongViewModel,
    sessionManager: SessionManager,
    currentSong: com.example.purrytify.model.Song?
) {
    var isDownloading by remember { mutableStateOf(false) }
    var downloadComplete by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                currentSong?.let { song ->
                    downloadSong(context, song, songViewModel, sessionManager) {
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