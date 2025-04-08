package com.example.purrytify.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.purrytify.R

class HomeScreen : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HomeScreenContent()
        }
    }
}

@Composable
fun HomeScreenContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // New Songs Section
        Text(
            text = "New songs",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow {
            items(newSongs) { song ->
                Column(
                    modifier = Modifier
                        .padding(end = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = song.imageRes),
                        contentDescription = song.title,
                        modifier = Modifier
                            .size(100.dp)
                    )
                    Text(
                        text = song.title,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recently Played Section
        Text(
            text = "Recently played",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Column {
            recentlyPlayed.forEach { song ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = song.imageRes),
                        contentDescription = song.title,
                        modifier = Modifier
                            .size(50.dp)
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

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom Player Section
        BottomPlayerSection()
    }
}

@Composable
fun BottomPlayerSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.DarkGray)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.song_cover),
            contentDescription = "Currently Playing",
            modifier = Modifier.size(50.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Starboy",
                color = Color.White,
                fontSize = 14.sp
            )
            Text(
                text = "The Weeknd",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
        IconButton(onClick = { /* Play/Pause Action */ }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_play),
                contentDescription = "Play",
                tint = Color.White
            )
        }
        IconButton(onClick = { /* More Options */ }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_more),
                contentDescription = "More",
                tint = Color.White
            )
        }
    }
}

// Sample Data buat sementara
data class Song(val title: String, val artist: String, val imageRes: Int)

val newSongs = listOf(
    Song("Starboy", "The Weeknd", R.drawable.album_cover),
    Song("Here Comes The Sun", "The Beatles", R.drawable.album_cover),
    Song("Midnight Pretenders", "Tomoko Aran", R.drawable.album_cover)
)

val recentlyPlayed = listOf(
    Song("Jazz is for ordinary people", "berlioz", R.drawable.song_cover),
    Song("Loose", "Daniel Caesar", R.drawable.song_cover),
    Song("Nights", "Frank Ocean", R.drawable.song_cover),
    Song("Kiss of Life", "Sade", R.drawable.song_cover),
    Song("BEST INTEREST", "Tyler, The Creator", R.drawable.song_cover)
)