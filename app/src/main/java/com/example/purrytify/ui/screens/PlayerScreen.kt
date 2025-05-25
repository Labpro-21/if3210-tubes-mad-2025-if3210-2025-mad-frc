package com.example.purrytify.ui.screens

import android.app.Application
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
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
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.purrytify.viewmodel.PlayerViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.purrytify.model.Song
import com.example.purrytify.ui.InsertSongPopUp
import com.example.purrytify.ui.SongSettingsModal
import com.example.purrytify.viewmodel.PlayerViewModelFactory
import com.example.purrytify.viewmodel.SongViewModel
import com.example.purrytify.ui.LockScreenOrientation
import android.content.pm.ActivityInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.mutableStateOf
import com.example.purrytify.utils.shareServerSong
import androidx.compose.runtime.*
import androidx.compose.material.icons.filled.VolumeUp // Contoh ikon untuk output device
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextOverflow
import com.example.purrytify.ui.components.AudioOutputSelectorBottomSheet
import com.example.purrytify.viewmodel.AudioOutputViewModel
import kotlinx.coroutines.launch
import androidx.compose.runtime.*


@Composable
fun PlayerScreen(
    modifier: Modifier = Modifier,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    songViewModel: SongViewModel,
    playerViewModel: PlayerViewModel,
    audioOutputViewModel: AudioOutputViewModel,
) {

    val context = LocalContext.current

    val currentSong by songViewModel.current_song.collectAsState()

    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val isLooping by playerViewModel.isLooping.collectAsState()
    val currentPositionSeconds by playerViewModel.currentPositionSeconds.collectAsState(initial = 0f)
    val activeAudioDevice by playerViewModel.activeAudioDevice.collectAsState()

    val songUri = currentSong?.audioPath?.toUri()

    var showAudioOutputSelector by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val activeDeviceState by playerViewModel.activeAudioDevice.collectAsState()

    LaunchedEffect(songUri) {
        songUri?.let {
            playerViewModel.prepareAndPlay(it, onSongComplete = onNext)
        }
    }

    if (showAudioOutputSelector) {
        AudioOutputSelectorBottomSheet(
            playerViewModel = playerViewModel,
            onDismiss = { showAudioOutputSelector = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            activeAudioDevice?.let { device ->
                Text(
                    text = "Playing on: ${activeDeviceState?.let { audioOutputViewModel.getDeviceName(it) } ?: "Device Speaker"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } ?: Text(
                text = "Playing on: Device Speaker",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            )

            Row {
                IconButton(onClick = { showAudioOutputSelector = true }) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "Select Output Device")
                }
                SongSettingsModal(songViewModel, playerViewModel)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(256.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Gray),
            contentAlignment = Alignment.Center
        ) {
            currentSong?.artworkPath?.toUri()?.let { artworkUri ->
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(artworkUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Artwork",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } ?: run {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "No artwork",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(100.dp)
                )
            }

        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(currentSong?.title ?: "-", style = MaterialTheme.typography.titleLarge)
                Text(
                    currentSong?.artist ?: "-",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            currentSong?.let { song ->
                if (song.audioPath.startsWith("http") && !song.isExplicitlyAdded && song.serverId != null) {
                    IconButton(onClick = { shareServerSong(context, song) }) {
                        Icon(Icons.Default.Share, contentDescription = "Share Song")
                    }
                }
            }

            IconButton(onClick = { currentSong?.let { songViewModel.toggleLikeSong(it) } }) {
                Icon(
                    imageVector = if (currentSong?.liked == true)
                        Icons.Default.Favorite
                    else
                        Icons.Default.FavoriteBorder,
                    contentDescription = "Toggle Like"
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Slider(
            value = currentPositionSeconds,
            onValueChange = { playerViewModel.seekTo(it) },
            valueRange = 0f..((currentSong?.duration ?: 1000) / 1000f),
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                formatDuration(currentPositionSeconds.toLong() * 1000),
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                formatDuration(currentSong?.duration ?: 0),
                style = MaterialTheme.typography.labelSmall
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {}) {
                Icon(Icons.Default.Shuffle, contentDescription = "Shuffle")
            }
            IconButton(onClick = { onPrevious() }) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous")
            }

            IconButton(
                onClick = { playerViewModel.playPause() },
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

            IconButton(onClick = { onNext() }) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next")
            }
            IconButton(onClick = { playerViewModel.toggleLoop() }) {
                Icon(
                    imageVector = if (isLooping) Icons.Default.RepeatOne else Icons.Default.Repeat,
                    contentDescription = "Repeat"
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerModalBottomSheet(
    sheetState: SheetState,
    showSheet: Boolean,
    onDismiss: () -> Unit,
    song: Song,
    songViewModel: SongViewModel,
    onSongChange: (Int) -> Unit,
    playerViewModel: PlayerViewModel,
    audioOutputViewModel: AudioOutputViewModel,
    isOnline: Boolean
) {
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
    val shouldClose by playerViewModel.shouldClosePlayerSheet.collectAsState()
    if (showSheet) {
        LaunchedEffect(shouldClose) {
            if (shouldClose) {
                sheetState.hide()
                onDismiss()
                playerViewModel.resetCloseSheetFlag()
            }
        }
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            PlayerScreen(
                onNext = { onSongChange(1) },
                onPrevious = { onSongChange(-1) },
                songViewModel = songViewModel,
                playerViewModel = playerViewModel,
                audioOutputViewModel = audioOutputViewModel,
            )
        }
    }
}



