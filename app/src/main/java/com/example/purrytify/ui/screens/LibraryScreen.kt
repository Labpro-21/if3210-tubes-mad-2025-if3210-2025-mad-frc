package com.example.purrytify.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.purrytify.data.SongRepository
import com.example.purrytify.data.AppDatabase
import com.example.purrytify.model.Song
import com.example.purrytify.viewmodel.SongViewModel
import com.example.purrytify.viewmodel.SongViewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.core.net.toUri
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import android.net.Uri
import androidx.lifecycle.ViewModelProvider
import com.example.purrytify.ui.InsertSongPopUp


@Composable
fun LibraryScreen(modifier: Modifier = Modifier, onBack: () -> Unit) {



    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val repository = remember { SongRepository(db.songDao()) }


    val viewModel: SongViewModel = viewModel(
        factory = SongViewModelFactory(repository)
    )

    val songs by viewModel.songs.collectAsState()

    // 👉 State untuk modal dan lagu yang dipilih
    val (showPlayer, setShowPlayer) = remember { mutableStateOf(false) }
    val (selectedSong, setSelectedSong) = remember { mutableStateOf<Song?>(null) }

    InsertSongPopUp(viewModel)

    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = "All Songs",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (songs.isEmpty()) {
            Text("No songs found.", color = Color.Gray)
        } else {
            LazyColumn {
                items(songs) { song ->
                    SongItem(song = song, onClick = {
                        setSelectedSong(song)
                        setShowPlayer(true)
                    })
                }
            }
        }
    }


    // 👉 Modal Bottom Sheet
    selectedSong?.let { song ->
        PlayerModalBottomSheet(
            showSheet = showPlayer,
            onDismiss = { setShowPlayer(false) },
            songTitle = song.title,
            artistName = song.artist,
            artworkUri = song.artworkPath?.toUri(),
            songUri = song.audioPath.toUri(),
            isPlaying = true,
            progress = 0.0f // bisa update dari ViewModel nanti
        )
    }
}


@Composable
fun SongItem(song: Song, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (song.artworkPath != null) {
                Image(
                    painter = rememberAsyncImagePainter(song.artworkPath.toUri()),
                    contentDescription = "Artwork",
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MusicNote, contentDescription = null)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(song.title, style = MaterialTheme.typography.titleMedium)
                Text(song.artist, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                Text(
                    formatDuration(song.duration),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
    }
}


fun formatDuration(seconds: Long): String {
    val minutes = seconds / 60000
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}

