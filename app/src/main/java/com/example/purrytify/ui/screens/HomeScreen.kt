package com.example.purrytify.ui.screens

import android.app.Application
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.purrytify.R
import com.example.purrytify.data.AppDatabase
import com.example.purrytify.repository.SongRepository
import com.example.purrytify.viewmodel.PlayerViewModel
import com.example.purrytify.viewmodel.PlayerViewModelFactory
import com.example.purrytify.viewmodel.SongViewModel
import com.example.purrytify.viewmodel.SongViewModelFactory
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import com.example.purrytify.ui.navBar.BottomNavBar
import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import com.example.purrytify.ui.navBar.VerticalNavBar
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.purrytify.model.Song
import com.example.purrytify.ui.LockScreenOrientation

@Composable
fun HomeScreenContent(
    onNavigateToLibrary: () -> Unit,
    onNavigateToProfile: () -> Unit,
    songViewModel: SongViewModel,
    playerViewModel: PlayerViewModel,
    newSongsFromDb: List<Song>,
    recentlyPlayedFromDb: List<Song>
) {
    val context = LocalContext.current
    val appContext = if (!LocalInspectionMode.current)
        context.applicationContext as? Application
    else null

    if (appContext == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Preview Mode - AppContext tidak tersedia",
                color = Color.White
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            // Header untuk New Songs
            Text(
                text = "New songs",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (newSongsFromDb.isEmpty()) {
            item {
                Text(
                    text = "No new songs yet",
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }
        } else {
            item {
                LazyRow {
                    items(newSongsFromDb) { song ->
                        Column(
                            modifier = Modifier
                                .clickable {
                                    songViewModel.setCurrentSong(song)
                                    playerViewModel.prepareAndPlay(song.audioPath.toUri()) { }
                                }
                                .padding(end = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(song.artworkPath?.toUri()),
                                contentDescription = "Artwork",
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Text(
                                text = song.title,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
            // Header untuk Recently Played
            Text(
                text = "Recently played",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (recentlyPlayedFromDb.isEmpty()) {
            item {
                Text(
                    text = "No recently played songs, start listening now",
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }
        } else {
            items(recentlyPlayedFromDb) { song ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            songViewModel.setCurrentSong(song)
                            playerViewModel.prepareAndPlay(song.audioPath.toUri()) { }
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(song.artworkPath?.toUri()),
                        contentDescription = song.title,
                        modifier = Modifier.size(50.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = song.title,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = song.artist,
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenWithBottomNav(
    onNavigateToLibrary: () -> Unit,
    onNavigateToProfile: () -> Unit,
    songViewModel: SongViewModel,
    playerViewModel: PlayerViewModel,
    modifier: Modifier = Modifier,
    newSongsFromDb: List<Song>,
    recentlyPlayedFromDb: List<Song>
) {
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    // State untuk membuka modal bottom sheet
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
                    onSectionClick = { showPlayerSheet = true }
                )
                BottomNavBar(
                    currentRoute = "home",
                    onItemSelected = { route ->
                        when (route) {
                            "library" -> onNavigateToLibrary()
                            "profile" -> onNavigateToProfile()
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            HomeScreenContent(
                onNavigateToLibrary = onNavigateToLibrary,
                onNavigateToProfile = onNavigateToProfile,
                songViewModel = songViewModel,
                playerViewModel = playerViewModel,
                newSongsFromDb = newSongsFromDb,
                recentlyPlayedFromDb = recentlyPlayedFromDb
            )
        }
    }
}

@Composable
fun HomeScreenResponsive(
    onNavigateToLibrary: () -> Unit,
    onNavigateToProfile: () -> Unit,
    songViewModel: SongViewModel,
    playerViewModel: PlayerViewModel
) {
    val configuration = LocalConfiguration.current
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR)
    val context = LocalContext.current
    val appContext = context.applicationContext as? Application

    val newSongsFromDb by songViewModel.newSongs.collectAsState()
    val recentlyPlayedFromDb by songViewModel.recentlyPlayed.collectAsState()

    if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        // Landscape: Tampilan dengan navbar vertikal di sisi kiri dan konten di sisi kanan
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(200.dp)
                    .background(color = androidx.compose.ui.graphics.Color.Black)
                    .padding(8.dp)
            ) {
                // Navbar vertikal
                VerticalNavBar(
                    currentRoute = "home",
                    onItemSelected = { route ->
                        when (route) {
                            "library" -> onNavigateToLibrary()
                            "profile" -> onNavigateToProfile()
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                // Bottom player di bagian bawah navbar
                BottomPlayerSectionFromDB(
                    songViewModel = songViewModel,
                    isPlaying = playerViewModel.isPlaying.collectAsState().value,
                    onPlayPause = { playerViewModel.playPause() },
                    onSectionClick = { }
                )
            }
            // Konten utama (New Songs dan Recently Played)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentPadding = PaddingValues(16.dp)
            ) {
                item {
                    // Header untuk New Songs
                    Text(
                        text = "New songs",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                // Item header untuk kasus tidak ada data
                if (newSongsFromDb.isEmpty()) {
                    item {
                        Text(
                            text = "No new songs yet",
                            color = Color.White,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                } else {
                    item {
                        LazyRow {
                            items(newSongsFromDb) { song ->
                                Column(
                                    modifier = Modifier
                                        .clickable {
                                            songViewModel.setCurrentSong(song)
                                            playerViewModel.prepareAndPlay(song.audioPath.toUri()) { }
                                        }
                                        .padding(end = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    androidx.compose.foundation.Image(
                                        painter = rememberAsyncImagePainter(song.artworkPath?.toUri()),
                                        contentDescription = "Artwork",
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                    Text(
                                        text = song.title,
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
                // Header untuk Recently Played
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Recently played",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                // Data Recently Played
                if (recentlyPlayedFromDb.isEmpty()) {
                    item {
                        Text(
                            text = "No recently played songs, start listening now",
                            color = Color.White,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                } else {
                    items(recentlyPlayedFromDb) { song ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    songViewModel.setCurrentSong(song)
                                    playerViewModel.prepareAndPlay(song.audioPath.toUri()) { }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.foundation.Image(
                                painter = rememberAsyncImagePainter(song.artworkPath?.toUri()),
                                contentDescription = song.title,
                                modifier = Modifier.size(50.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = song.title,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = song.artist,
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        HomeScreenWithBottomNav(
            onNavigateToLibrary = onNavigateToLibrary,
            onNavigateToProfile = onNavigateToProfile,
            songViewModel = songViewModel,
            playerViewModel = playerViewModel,
            newSongsFromDb = newSongsFromDb,
            recentlyPlayedFromDb = recentlyPlayedFromDb
        )
    }
}