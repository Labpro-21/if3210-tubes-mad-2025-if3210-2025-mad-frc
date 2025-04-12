package com.example.purrytify.ui.screens

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.purrytify.viewmodel.SongViewModel
import com.example.purrytify.viewmodel.PlayerViewModel
import com.example.purrytify.viewmodel.PlayerViewModelFactory
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.collectAsState


@Composable
fun BottomPlayerSectionFromDB(
    songViewModel: SongViewModel,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSectionClick: () -> Unit  // Callback untuk membuka PlayerModalBottomSheet
) {
    val context = LocalContext.current
    val appContext = if (!LocalInspectionMode.current) (context as? Activity)?.application else null

    if (appContext == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.DarkGray)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Preview Mode - Player tidak aktif", color = Color.White)
        }
        return
    }

    val currentSong by songViewModel.current_song.collectAsState()

    if (currentSong == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.DarkGray)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "No song playing", color = Color.White)
        }
        return
    }

    // Bungkus seluruh konten kecuali play/pause button dengan clickable
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.DarkGray)
            .clickable { onSectionClick() }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!currentSong!!.artworkPath.isNullOrEmpty())
                Image(
                    painter = rememberAsyncImagePainter(currentSong!!.artworkPath!!.toUri()),
                    contentDescription = currentSong!!.title,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            else
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "No artwork",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(64.dp)
                )

            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentSong!!.title,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = currentSong!!.artist,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
            // IconButton play/pause diletakkan di luar area clickable parent dengan Modifier.clickable tidak diterapkan,
            // sehingga ketika ditekan, hanya onPlayPause yang terpanggil.
            IconButton(
                onClick = { onPlayPause() },
                modifier = Modifier
                    .size(72.dp)
                    .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}