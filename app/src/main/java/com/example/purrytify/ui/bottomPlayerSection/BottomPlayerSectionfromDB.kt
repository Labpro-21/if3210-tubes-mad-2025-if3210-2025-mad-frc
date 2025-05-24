package com.example.purrytify.ui.screens

import android.app.Activity
import android.util.Log
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale


@Composable
fun BottomPlayerSectionFromDB(
    songViewModel: SongViewModel,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSectionClick: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val imageSize = if (screenWidth > 360) 48.dp else 64.dp
    val iconButtonSize = if (screenWidth > 360) 56.dp else 72.dp
    val iconSize = if (screenWidth > 360) 24.dp else 36.dp

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

    // ✅ Tampilkan bottom player jika ada current song (baik dari DB maupun online)
    if (currentSong == null) {
        // Tidak tampilkan apa-apa jika tidak ada lagu yang sedang diputar
        return
    }

    LaunchedEffect(currentSong) {
        if (currentSong != null) {
            Log.d("BottomPlayer_Update", "Observed currentSong update: ${currentSong}")
        } else {
            Log.d("BottomPlayer_Update", "Observed currentSong update: null")
        }
    }

    currentSong?.let { song ->
        // Log untuk memastikan recomposition terjadi dengan lagu yang benar
        Log.d("BottomPlayer", "Recomposing for song: ${song.title} (ID: ${song.id})")

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.DarkGray) // Atau warna tema Anda
                .clickable { onSectionClick() }
                .padding(8.dp) // Padding disesuaikan
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Artwork
                if (!song.artworkPath.isNullOrEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = if (song.artworkPath!!.startsWith("http")) {
                                song.artworkPath
                            } else {
                                song.artworkPath!!.toUri()
                            }
                        ),
                        contentDescription = song.title,
                        modifier = Modifier
                            .size(48.dp) // Ukuran artwork disesuaikan
                            .clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "No artwork",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        color = Color.White,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        color = Color.Gray,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Tombol Play/Pause (dari PlayerViewModel, status isPlaying dilewatkan)
                IconButton(
                    onClick = { onPlayPause() },
                    modifier = Modifier
                        .size(48.dp) // Ukuran tombol disesuaikan
                        // .background(MaterialTheme.colorScheme.primary, shape = CircleShape) // Opsional background
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White, // Atau MaterialTheme.colorScheme.onPrimary
                        modifier = Modifier.size(32.dp) // Ukuran ikon disesuaikan
                    )
                }
            }
        }
    } ?: run {
        // Jika currentSong null, tidak tampilkan apa-apa atau placeholder
        // Box(modifier = Modifier.fillMaxWidth().height(56.dp)) // Placeholder kosong
        Log.d("BottomPlayer", "No current song, BottomPlayerSection is empty.")
    }
}