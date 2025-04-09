package com.example.purrytify.ui.screens

import android.app.Application
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.purrytify.viewmodel.PlayerViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.purrytify.viewmodel.PlayerViewModelFactory


@Composable
fun PlayerScreen(
    modifier: Modifier = Modifier,
    songId:Int,
    songTitle: String = "Song Title",
    artistName: String = "Artist Name",
    artworkUri: Uri? = null,
    songUri: Uri,
    isPlaying: Boolean = true,
    progress: Float = 0.3f, // 30% played
    onNext:()-> Unit,
    onPrevious:()-> Unit,
) {
    val context = LocalContext.current
    val appContext = LocalContext.current.applicationContext as Application

    val viewModel: PlayerViewModel = viewModel(
        factory = PlayerViewModelFactory(appContext)
    )

    val isPlaying by viewModel.isPlaying.collectAsState()
    val isLooping by viewModel.isLooping.collectAsState()
    val progress by viewModel.progress.collectAsState()

    LaunchedEffect(songUri) {
        viewModel.prepareAndPlay(songUri)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // Artwork
        Box(
            modifier = Modifier
                .size(320.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Gray),
            contentAlignment = Alignment.Center
        ) {
            if (artworkUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(artworkUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Artwork",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "No artwork",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(100.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Song Info
        Text(songTitle, style = MaterialTheme.typography.titleLarge)
        Text(artistName, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)

        Spacer(modifier = Modifier.height(24.dp))

        Slider(
            value = progress,
            onValueChange = { viewModel.seekTo(it) },
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )


        // Duration Row (Optional Static)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("1:15", style = MaterialTheme.typography.labelSmall)
//            Text(viewModel., style = MaterialTheme.typography.labelSmall)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Playback Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* Shuffle */ }) {
                Icon(Icons.Default.Shuffle, contentDescription = "Shuffle")
            }
            IconButton(onClick = { onPrevious() }) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous")
            }

            // Play/Pause Button
            IconButton(
                onClick = { viewModel.playPause() },
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }


            IconButton(onClick = { onNext() }) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next")
            }
            IconButton(onClick = { viewModel.toggleLoop() }) {
                Icon(
                    imageVector = if (isLooping) Icons.Default.RepeatOne else Icons.Default.Repeat,
                    contentDescription = "Repeat")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerModalBottomSheet(
    showSheet: Boolean,
    onDismiss: () -> Unit,
    songId: Int,
    songTitle: String,
    artistName: String,
    artworkUri: Uri?,
    songUri: Uri,
    isPlaying: Boolean,
    progress: Float,
    onSongChange: (Int) -> Unit
) {
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            PlayerScreen(
                songId = songId,
                songTitle = songTitle,
                artistName = artistName,
                artworkUri = artworkUri,
                songUri = songUri,
                isPlaying = isPlaying,
                progress = progress,
                onNext = { onSongChange(songId + 1) },
                onPrevious = { onSongChange(songId - 1) }
            )
        }
    }
}

