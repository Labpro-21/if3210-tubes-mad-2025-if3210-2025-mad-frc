package com.example.purrytify.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.purrytify.viewmodel.PlayerViewModel
import com.example.purrytify.viewmodel.SongViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongSettingsModal(
    songViewModel: SongViewModel,
    playerViewModel: PlayerViewModel
) {
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
            val currentSong by songViewModel.current_song.collectAsState()
            InsertSongPopUp(songViewModel,currentSong)
            ConfirmDelete(songViewModel,playerViewModel)

        }
    }
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
                        songViewModel.deleteSong(it.id)
                        playerViewModel.stopPlayer()
                        playerViewModel.closePlayerSheet() // Ini yang trigger penutupan sheet

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


