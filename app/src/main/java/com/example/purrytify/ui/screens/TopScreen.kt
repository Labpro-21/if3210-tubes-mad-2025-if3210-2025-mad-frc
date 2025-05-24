package com.example.purrytify.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.purrytify.viewmodel.OnlineSongViewModel
import com.example.purrytify.viewmodel.PlayerViewModel
import com.example.purrytify.viewmodel.SongViewModel

@Composable
fun TopScreen(
    chartType: String, // "global" atau country code seperti "ID"
    onlineViewModel: OnlineSongViewModel,
    songViewModel: SongViewModel,
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val onlineSongs by onlineViewModel.onlineSongs.collectAsState()
    
    val title = if (chartType == "global") "Top 50 - Global" else "Top 50 - Indonesia"
    val description = if (chartType == "global") 
        "Your daily update of the most played tracks right now - Global"
    else 
        "Your daily update of the most played tracks right now - Country"

    LaunchedEffect(chartType) {
        if (chartType == "global") {
            onlineViewModel.loadTopSongs(null)
        } else {
            onlineViewModel.loadTopSongs(chartType)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
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
        }

        // Cover dan description
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Cover image (gradient box seperti track view)
            Box(
                modifier = Modifier
                    .size(280.dp)
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
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Top 50",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (chartType == "global") "GLOBAL" else "INDONESIA",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Description
            Text(
                text = description,
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Download dan Play buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Download button
                OutlinedButton(
                    onClick = { /* Implementasi download semua */ },
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Color.White)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download All",
                        tint = Color.White
                    )
                }

                // Play button
                Button(
                    onClick = {
                        // Play first song in list
                        if (onlineSongs.isNotEmpty()) {
                            val firstSong = onlineSongs.first()
                            onlineViewModel.sendSongsToMusicService()
                            songViewModel.setCurrentSong(firstSong)
                            //playerViewModel.prepareAndPlay(firstSong.audioPath.toUri()) { }
                            playerViewModel.prepareAndPlay(0)
                        }
                    },
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1ED760) // Spotify green
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play All",
                        tint = Color.Black,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // List lagu vertical
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            itemsIndexed(onlineSongs) { index, song ->
                Top50SongItem(
                    song = song,
                    rank = index + 1,
                    onClick = {
                        onlineViewModel.sendSongsToMusicService()
                        songViewModel.setCurrentSong(song)
//                        playerViewModel.prepareAndPlay(song.audioPath.toUri()) { }
                        Log.d("TopScreen", "Prepare and Play song ID-${song.id-1}")
                        playerViewModel.prepareAndPlay(song.id-1)

                    }
                )
            }
        }
    }
}

@Composable
fun Top50SongItem(
    song: Song,
    rank: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank number
        Text(
            text = "$rank",
            color = Color.Gray,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(32.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Album art
        Image(
            painter = rememberAsyncImagePainter(song.artworkPath?.toUri()),
            contentDescription = song.title,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Song info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
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
        
        // Duration
        Text(
            text = song.duration.let { formatDuration(it) },
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}
