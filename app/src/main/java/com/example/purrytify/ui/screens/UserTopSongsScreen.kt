package com.example.purrytify.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.purrytify.model.Song
import com.example.purrytify.ui.navBar.BottomNavBar
import com.example.purrytify.viewmodel.AudioOutputViewModel
import com.example.purrytify.viewmodel.PlayerViewModel
import com.example.purrytify.viewmodel.ProfileViewModel
import com.example.purrytify.viewmodel.SongViewModel
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserTopSongsScreen(
    profileViewModel: ProfileViewModel,
    songViewModel: SongViewModel,
    playerViewModel: PlayerViewModel,
    audioOutputViewModel: AudioOutputViewModel,
    yearMonthString: String,
    onBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToProfile: () -> Unit,
) {
    val topPlayedSongs by profileViewModel.userTopPlayedSongs.collectAsState()
    val currentGlobalSong by songViewModel.currentSong.collectAsState()
    val isGlobalPlaying by playerViewModel.isPlaying.collectAsState()
    var showPlayerSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    val yearMonth = remember(yearMonthString) { YearMonth.parse(yearMonthString) }
    val monthDisplay = yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

    LaunchedEffect(yearMonth) {
        profileViewModel.loadUserTopPlayedSongs(yearMonth)
    }

    var currentPlaylistIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(currentGlobalSong, topPlayedSongs) {
        currentGlobalSong?.let { song ->
            val index = topPlayedSongs.indexOfFirst { it.id == song.id && it.userId == song.userId }
            if (index != -1) {
                currentPlaylistIndex = index
            }
        }
    }
//
//    fun playNextInTopSongs() {
//        if (topPlayedSongs.isNotEmpty()) {
//            currentPlaylistIndex = (currentPlaylistIndex + 1) % topPlayedSongs.size
//            val nextSong = topPlayedSongs[currentPlaylistIndex]
//            songViewModel.setCurrentSong(nextSong)
//            playerViewModel.prepareAndPlay(nextSong.audioPath.toUri()) { playNextInTopSongs() }
//        }
//    }
//
//    fun playPreviousInTopSongs() {
//        if (topPlayedSongs.isNotEmpty()) {
//            currentPlaylistIndex = if (currentPlaylistIndex - 1 < 0) topPlayedSongs.size - 1 else currentPlaylistIndex - 1
//            val prevSong = topPlayedSongs[currentPlaylistIndex]
//            songViewModel.setCurrentSong(prevSong)
//            playerViewModel.prepareAndPlay(prevSong.audioPath.toUri()) { playNextInTopSongs() }
//        }
//    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Top songs") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF121212),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            Column {
                if (currentGlobalSong != null) {
                    BottomPlayerSectionFromDB(
                        songViewModel = songViewModel,
                        isPlaying = isGlobalPlaying,
                        onPlayPause = { playerViewModel.playPause() },
                        onSectionClick = { showPlayerSheet = true }
                    )
                }
                BottomNavBar(
                    currentRoute = "profile_top_songs",
                    onItemSelected = { route ->
                        when (route) {
                            "home" -> onNavigateToHome()
                            "library" -> onNavigateToLibrary()
                            "profile" -> onNavigateToProfile()
                        }
                    }
                )
            }
        },
        containerColor = Color(0xFF121212)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = monthDisplay,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Your most played tracks this month.",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Gray
                )
            }

            if (topPlayedSongs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No songs played enough this month to rank.", color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(16.dp))
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(topPlayedSongs) { index, song ->
                        TopPlayedSongItem(
                            rank = index + 1,
                            song = song,
                            isCurrentPlaying = song.id == currentGlobalSong?.id && song.userId == currentGlobalSong?.userId,
                            onSongClick = {
                                currentPlaylistIndex = index
                                songViewModel.setCurrentSong(song)
                                profileViewModel.sendSongsToMusicService()
                                playerViewModel.prepareAndPlay(index)
//                                playerViewModel.prepareAndPlay(song.audioPath.toUri()) {
//                                    playNextInTopSongs()
//                                }
                                showPlayerSheet = true
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TopPlayedSongItem(
    rank: Int,
    song: Song,
    isCurrentPlaying: Boolean,
    onSongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isCurrentPlaying) Color.DarkGray.copy(alpha = 0.5f) else Color.Transparent)
            .clickable { onSongClick() }
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "%02d".format(rank),
            style = MaterialTheme.typography.titleMedium.copy(
                color = if (isCurrentPlaying) MaterialTheme.colorScheme.primary else Color.White,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.width(32.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        AsyncImage(
            model = song.artworkPath?.toUri(),
            contentDescription = "Artwork for ${song.title}",
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Crop,
            error = rememberAsyncImagePainter(model = com.example.purrytify.R.drawable.placeholder_music)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (isCurrentPlaying) MaterialTheme.colorScheme.primary else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (isCurrentPlaying) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Currently Playing",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp).padding(start = 8.dp)
            )
        }
    }
}