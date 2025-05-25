package com.example.purrytify.ui.components

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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopModalBottomSheet(
    visible: Boolean,
    chartType: String,
    onlineViewModel: OnlineSongViewModel,
    songViewModel: SongViewModel,
    playerViewModel: PlayerViewModel,
    onDismiss: () -> Unit
) {
    if (!visible) return

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    val onlineSongs by onlineViewModel.onlineSongs.collectAsState()

    val title = if (chartType == "global") "Top 50 - Global" else "Top 10 - Indonesia"
    val description = if (chartType == "global")
        "Your daily update of the most played tracks right now - Global"
    else
        "Your daily update of the most played tracks right now - Indonesia"

    LaunchedEffect(chartType) {
        if (chartType == "global") {
            onlineViewModel.loadTopSongs(null)
        } else {
            onlineViewModel.loadTopSongs(chartType)
        }
    }


    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        containerColor = Color(0xFF121212),
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .size(width = 40.dp, height = 4.dp),
                shape = RoundedCornerShape(2.dp),
                color = Color.Gray
            ) {}
        }
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(screenHeight * 0.7f)
                .background(Color(0xFF121212))
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                item {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }


                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Box(
                            modifier = Modifier
                                .size(140.dp)
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
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = if (chartType == "global") "Top 50" else "Top 10",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (chartType == "global") "GLOBAL" else "INDONESIA",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))


                        Text(
                            text = description,
                            color = Color.Gray,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))


                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            OutlinedButton(
                                onClick = { /* Download all */ },
                                modifier = Modifier.size(52.dp),
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
                                        onlineViewModel.sendSongsToMusicService()
                                        songViewModel.setCurrentSong(firstSong)

                                        playerViewModel.prepareAndPlay(0)
                                    }
                                },
                                modifier = Modifier.size(60.dp),
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
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }


                if (onlineSongs.isEmpty()) {
                    item {
                        Text(
                            text = "No songs available for $chartType",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    itemsIndexed(onlineSongs) { index, song ->
                        TopSongItemModal(
                            song = song,
                            rank = index + 1,
                            onClick = {
                                onlineViewModel.sendSongsToMusicService()
                                songViewModel.setCurrentSong(song)

                                Log.d("TopScreen", "Prepare and Play song ID-${index}")
                                playerViewModel.prepareAndPlay(index)
                            }
                        )
                    }
                }


                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
fun TopSongItemModal(
    song: Song,
    rank: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = "$rank",
            color = Color.Gray,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(22.dp)
        )

        Spacer(modifier = Modifier.width(6.dp))


        Image(
            painter = rememberAsyncImagePainter(
                model = if (song.artworkPath?.startsWith("http") == true) {
                    song.artworkPath
                } else {
                    song.artworkPath?.toUri()
                }
            ),
            contentDescription = song.title,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(8.dp))


        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                color = Color.Gray,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }


        Text(
            text = formatDuration(song.duration),
            color = Color.Gray,
            fontSize = 10.sp
        )
    }
}

private fun formatDuration(milliseconds: Long): String {
    val seconds = milliseconds / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%d:%02d", minutes, remainingSeconds)
}