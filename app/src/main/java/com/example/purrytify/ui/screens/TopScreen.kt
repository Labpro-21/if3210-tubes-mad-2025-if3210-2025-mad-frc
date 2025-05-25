package com.example.purrytify.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.rememberAsyncImagePainter
import com.example.purrytify.model.Song
import com.example.purrytify.ui.components.SongSettingsModal
import com.example.purrytify.ui.navBar.BottomNavBar
import com.example.purrytify.utils.SessionManager
import com.example.purrytify.utils.downloadSong
import com.example.purrytify.viewmodel.OnlineSongViewModel
import com.example.purrytify.viewmodel.PlayerViewModel
import com.example.purrytify.viewmodel.SongViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopScreen(
    chartType: String,
    onlineViewModel: OnlineSongViewModel,
    songViewModel: SongViewModel,
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    val onlineSongs by onlineViewModel.onlineSongs.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val currentSong by songViewModel.current_song.collectAsState()
    var showPlayerSheet by remember { mutableStateOf(false) }
    var currentPlaylistIndex by remember { mutableStateOf(-1) }

    var showSongSettings by remember { mutableStateOf(false) }
    var selectedOnlineSong by remember { mutableStateOf<Song?>(null) }

    var isDownloadingAll by remember { mutableStateOf(false) }
    var downloadedCount by remember { mutableStateOf(0) }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false,
        confirmValueChange = { true }
    )

    val title = if (chartType == "global") "Top 50 - Global" else "Top 10 - Indonesia"
    val description = if (chartType == "global")
        "Your daily update of the most played tracks right now - Global"
    else
        "Your daily update of the most played tracks right now - Indonesia"

    SongSettingsModal(
        song = currentSong,
        visible = showSongSettings,
        onDismiss = { showSongSettings = false },
        onEdit = { }, // Not used for online songs
        onDelete = { }, // Not used for online songs
        onShareUrl = {},
        isOnlineSong = currentSong?.audioPath?.startsWith("http") == true
    )

    LaunchedEffect(chartType) {
        if (chartType == "global") {
            onlineViewModel.loadTopSongs(null)
        } else {
            onlineViewModel.loadTopSongs(chartType)
        }
    }

    LaunchedEffect(currentSong, onlineSongs) {
        currentSong?.let { song ->
            if (onlineSongs.isNotEmpty()) {
                val index = onlineSongs.indexOfFirst { onlineSongItem ->
                    // dan onlineSongItem dari OnlineSongViewModel juga memiliki serverId yang sama.
                    (song.serverId != null && onlineSongItem.serverId == song.serverId) ||
                            (song.serverId == null && onlineSongItem.audioPath == song.audioPath) // Fallback jika tidak ada serverId (misalnya lagu lokal murni)
                }
                if (index != -1) {
                    currentPlaylistIndex = index
                    Log.d("Online Song", "currenPlayListIndex: ${currentPlaylistIndex}")
                }
            }
        }
    }

    fun playNextInSequence() {
        if (onlineSongs.isNotEmpty() && currentPlaylistIndex >= 0) {
            val nextIndex = (currentPlaylistIndex + 1) % onlineSongs.size
            val nextSong = onlineSongs[nextIndex]
            Log.d("Online Song", "next index: ${nextIndex}")
            Log.d("Online Song", "Nest Online Songs : ${onlineSongs[nextIndex].title}")
            currentPlaylistIndex = nextIndex
            songViewModel.setCurrentSong(nextSong)

            playerViewModel.prepareAndPlay(nextSong.audioPath.toUri()) {
                playNextInSequence()
            }
        }
    }

    fun playNext() {
        playNextInSequence()
    }

    fun playPrevious() {
        if (onlineSongs.isNotEmpty() && currentPlaylistIndex >= 0) {
            val prevIndex = if (currentPlaylistIndex - 1 < 0) {
                onlineSongs.size - 1
            } else {
                currentPlaylistIndex - 1
            }
            val prevSong = onlineSongs[prevIndex]
            currentPlaylistIndex = prevIndex
            songViewModel.setCurrentSong(prevSong)

            playerViewModel.prepareAndPlay(prevSong.audioPath.toUri()) {
                playNextInSequence()
            }
        }
    }

    fun downloadAll() {
        if (onlineSongs.isEmpty()) return
        isDownloadingAll = true
        downloadedCount = 0

        onlineSongs.forEach { song ->
            // langsung enqueue tanpa tunggu callback terakhir
            downloadSong(context, song, songViewModel, sessionManager) {
                downloadedCount++
                if (downloadedCount == onlineSongs.size) isDownloadingAll = false
            }
        }
    }

    if (showPlayerSheet) {
        currentSong?.let { song ->
            PlayerModalBottomSheet(
                showSheet = showPlayerSheet,
                onDismiss = { showPlayerSheet = false },
                song = song,
                songViewModel = songViewModel,
                onSongChange = { direction ->
                    when {
                        direction > 0 -> playNext()
                        direction < 0 -> playPrevious()
                        else -> { }
                    }
                },
                playerViewModel = playerViewModel,
                sheetState = sheetState
            )
        }
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
                    currentRoute = "top",
                    onItemSelected = { route ->
                        when (route) {
                            "home" -> onBack()
                            "library" -> onNavigateToLibrary()
                            "profile" -> onNavigateToProfile()
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
                .padding(paddingValues)
        ) {
            // Header dengan back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Cover dan description
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Cover image (gradient box)
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (chartType == "global")
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    listOf(Color(0xFF0B4870), Color(0xFF16BFFD))
                                )
                            else
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    listOf(Color(0xFFE34981), Color(0xFFFFB25E))
                                )
                        ),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = if (chartType == "global") "Top 50" else "Top 10",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (chartType == "global") "GLOBAL" else "INDONESIA",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Description
                Text(
                    text = description,
                    color = Color.Gray,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Download dan Play buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Download button
                    OutlinedButton(
                        onClick = { if (!isDownloadingAll) downloadAll() },
                        modifier = Modifier.size(50.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(Color.White)
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download All",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Button(
                        onClick = {
                            if (onlineSongs.isNotEmpty()) {
                                val firstSong = onlineSongs.first()
                                currentPlaylistIndex = 0
                                songViewModel.setCurrentSong(firstSong)

                                playerViewModel.prepareAndPlay(firstSong.audioPath.toUri()) {
                                    playNextInSequence()
                                }
                            }
                        },
                        modifier = Modifier.size(50.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1ED760)
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play All",
                            tint = Color.Black,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (onlineSongs.isEmpty()) {
                    item {
                        Text(
                            text = "Loading songs...",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    itemsIndexed(onlineSongs) { index, song ->
                        TopSongItem(
                            song = song,
                            rank = index + 1,
                            isCurrentSong = currentSong?.let {
                                it.id == song.id || it.title == song.title
                            } ?: false,
                            onClick = {
                                currentPlaylistIndex = index
                                songViewModel.setCurrentSong(song)
                                playerViewModel.prepareAndPlay(song.audioPath.toUri()) {
                                    playNextInSequence()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TopSongItem(
    song: Song,
    rank: Int,
    isCurrentSong: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 6.dp)
            .background(
                if (isCurrentSong) Color(0xFF1ED760).copy(alpha = 0.1f)
                else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank number
        Text(
            text = "$rank",
            color = if (isCurrentSong) Color(0xFF1ED760) else Color.Gray,
            fontSize = 14.sp,
            fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.width(28.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Album art - Handle online song artwork
        Image(
            painter = rememberAsyncImagePainter(
                model = if (song.artworkPath?.startsWith("http") == true) {
                    song.artworkPath // Online song - URL langsung
                } else {
                    song.artworkPath?.toUri() // Local song - convert ke URI
                }
            ),
            contentDescription = song.title,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Song info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = if (isCurrentSong) Color(0xFF1ED760) else Color.White,
                fontSize = 13.sp,
                fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                color = Color.Gray,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Duration
        Text(
            text = formatDuration(song.duration),
            color = Color.Gray,
            fontSize = 11.sp
        )

        // Play indicator untuk current song
        if (isCurrentSong) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Currently Playing",
                tint = Color(0xFF1ED760),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
