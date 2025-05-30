package com.example.purrytify.ui.screens

import android.content.pm.ActivityInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.example.purrytify.model.Song
import com.example.purrytify.viewmodel.SongViewModel
import coil.compose.rememberAsyncImagePainter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.core.net.toUri
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.example.purrytify.ui.InsertSongPopUp
import androidx.compose.material3.Scaffold
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.example.purrytify.ui.SongRecyclerView
import com.example.purrytify.ui.navBar.BottomNavBar
import com.example.purrytify.ui.LockScreenOrientation
import com.example.purrytify.viewmodel.AudioOutputViewModel
import com.example.purrytify.viewmodel.PlayerViewModel


@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(modifier: Modifier = Modifier, onBack: () -> Unit, songViewModel: SongViewModel, playerViewModel: PlayerViewModel, isOnline : Boolean, audioOutputViewModel: AudioOutputViewModel) {
    val context = LocalContext.current


    val currentSong by songViewModel.currentSong.collectAsState()
    val allSongs by songViewModel.songs.collectAsState()
    val likedSongs by songViewModel.likedSongs.collectAsState()
    var currentSongId by remember { mutableStateOf(0) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false,
        confirmValueChange = { true }
    )

    val tabs = listOf("All Songs", "Liked Songs")
    val (selectedTabIndex, setSelectedTabIndex) = remember { mutableIntStateOf(0) }

    val (showPlayer, setShowPlayer) = remember { mutableStateOf(false) }
    val (selectedSong, setSelectedSong) = remember { mutableStateOf<Song?>(null) }



    InsertSongPopUp(songViewModel)


    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = "Your Library",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { setSelectedTabIndex(index) },
                    text = { Text(title) }
                )
            }
        }

        val displayedSongs = if (selectedTabIndex == 0) allSongs else likedSongs

        if (displayedSongs.isEmpty()) {
            Text("No songs found.", color = Color.Gray, modifier = Modifier.padding(16.dp))
        } else {














            SongRecyclerView(
                songs = displayedSongs,
                onSongClick = { song ->
                    val index = allSongs.indexOf(song)
                    currentSongId = index
                    songViewModel.setCurrentSong(song)
                    songViewModel.sendSongsToMusicService()
                    playerViewModel.prepareAndPlay(index)

                    setShowPlayer(true)
                },
                onToggleLike = { song ->
                    songViewModel.toggleLikeSong(song)
                }
            )
        }
    }

    selectedSong?.let { song ->
        Log.d("Selected Song", "in selected song")
        PlayerModalBottomSheet(
            sheetState = sheetState,
            showSheet = showPlayer,
            onDismiss = {
                setShowPlayer(false) },
            song = song,
            songViewModel = songViewModel,
            onSongChange = { direction ->
                when {
                    direction > 0 -> {

                        currentSongId = (currentSongId + 1) % allSongs.size
                    }
                    direction < 0 -> {

                        currentSongId = if (currentSongId - 1 < 0) allSongs.size - 1 else currentSongId - 1
                    }
                }
                setSelectedSong(allSongs[currentSongId])
            },
            playerViewModel = playerViewModel,
            isOnline = isOnline,
            audioOutputViewModel = audioOutputViewModel
        )
        songViewModel.setCurrentSong(song)

    }
}


@Composable
fun SongItem(song: Song, onClick: () -> Unit, onToggleLike:(Song) -> Unit) {
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
            Spacer(modifier = Modifier.width(16.dp))

            IconButton(onClick = { onToggleLike(song) }) {
                Icon(
                    imageVector = if (song.liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Toggle Like"
                )
            }
        }
    }
}


fun formatDuration(miliseconds: Long): String {
    val seconds = miliseconds/1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreenWithBottomNav(
    onNavigateToHome: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onBack: () -> Unit,
    songViewModel: SongViewModel,
    playerViewModel: PlayerViewModel,
    modifier: Modifier = Modifier,
    isOnline: Boolean,
    audioOutputViewModel: AudioOutputViewModel
) {
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    var showPlayerSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false,
        confirmValueChange = { true }
    )
    val allSongs by songViewModel.songs.collectAsState()

    if (showPlayerSheet) {
        PlayerModalBottomSheet(
            showSheet = showPlayerSheet,
            onDismiss = { showPlayerSheet = false },
            song = songViewModel.currentSong.collectAsState(initial = null).value ?: return,
            songViewModel = songViewModel,
            onSongChange = { newId ->

                val newSongId = (newId + allSongs.size) % allSongs.size
                songViewModel.setCurrentSong(allSongs[newSongId])
                           },
            playerViewModel = playerViewModel,
            sheetState = sheetState,
            isOnline =isOnline,
            audioOutputViewModel = audioOutputViewModel
        )
    }

    Scaffold(
        bottomBar = {
            Column {
                BottomPlayerSectionFromDB(
                    songViewModel = songViewModel,
                    isPlaying = isPlaying,
                    onPlayPause = { playerViewModel.playPause() },
                    onSectionClick = {

                        showPlayerSheet = true
                    }
                )
                BottomNavBar(
                    currentRoute = "library",
                    onItemSelected = { route ->
                        when (route) {
                            "home" -> onNavigateToHome()
                            "profile" -> onNavigateToProfile()
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            LibraryScreen(
                onBack = onBack,
                songViewModel = songViewModel,
                playerViewModel = playerViewModel,
                isOnline = isOnline,
                audioOutputViewModel = audioOutputViewModel,
            )
        }
    }
}