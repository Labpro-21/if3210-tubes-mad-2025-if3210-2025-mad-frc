package com.example.purrytify.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
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
import com.example.purrytify.repository.SongRepository
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import com.example.purrytify.ui.SongRecyclerView
import com.example.purrytify.ui.navBar.BottomNavBar
import com.example.purrytify.ui.LockScreenOrientation
import com.example.purrytify.viewmodel.PlayerViewModel
import com.example.purrytify.viewmodel.PlayerViewModelFactory


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(modifier: Modifier = Modifier, onBack: () -> Unit, songViewModel: SongViewModel, playerViewModel: PlayerViewModel) {
    val context = LocalContext.current


    val currentSong by songViewModel.current_song.collectAsState()
    val allSongs by songViewModel.songs.collectAsState()
    val likedSongs by songViewModel.likedSongs.collectAsState()
    var currentSongId by remember { mutableStateOf(0) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false, // allow partially expanded state
        confirmValueChange = { true } // allow transition freely
    )
    // Tab state
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
//            LazyColumn {
//                items(displayedSongs) { song ->
//                    SongItem(
//                        song = song,
//                        onClick = {
//                            songViewModel.setCurrentSong(song)
//                            playerViewModel.prepareAndPlay(song.audioPath.toUri()) { }
//                        },
//                        onToggleLike = { song ->
//                            songViewModel.toggleLikeSong(song)
//                        }
//                    )
//                }
//            }
            SongRecyclerView(
                songs = displayedSongs,
                onSongClick = { song ->
                    val index = allSongs.indexOf(song)
                    currentSongId = index
                    setSelectedSong(song)
                    setShowPlayer(true)
                },
                onToggleLike = { song ->
                    songViewModel.toggleLikeSong(song)
                }
            )
        }
    }

    selectedSong?.let { song ->
        PlayerModalBottomSheet(
            sheetState = sheetState,
            showSheet = showPlayer,
            onDismiss = {
                setShowPlayer(false) },
            song = song,
            songViewModel = songViewModel,
            onSongChange = { newId ->
                currentSongId = (newId + allSongs.size) % allSongs.size
                setSelectedSong(allSongs[currentSongId])

            },
            playerViewModel = playerViewModel
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
    modifier: Modifier = Modifier
) {
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    var showPlayerSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false, // allow partially expanded state
        confirmValueChange = { true } // allow transition freely
    )

    if (showPlayerSheet) {
        PlayerModalBottomSheet(
            showSheet = showPlayerSheet,
            onDismiss = { showPlayerSheet = false },
            song = songViewModel.current_song.collectAsState(initial = null).value ?: return,
            songViewModel = songViewModel,
            onSongChange = { /* logika perubahan lagu */ },
            playerViewModel = playerViewModel,
            sheetState = sheetState
        )
    }

    Scaffold(
        bottomBar = {
            Column {
                BottomPlayerSectionFromDB(
                    songViewModel = songViewModel,
                    isPlaying = isPlaying,
                    onPlayPause = { playerViewModel.playPause() },
                    onSectionClick = { showPlayerSheet = true }  // Buka modal bottom sheet saat area diklik
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
                playerViewModel = playerViewModel
            )
        }
    }
}