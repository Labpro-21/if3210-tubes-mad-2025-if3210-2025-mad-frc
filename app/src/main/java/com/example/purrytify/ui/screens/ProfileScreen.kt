package com.example.purrytify.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.purrytify.R
import com.example.purrytify.viewmodel.ProfileViewModel

/**
 * Fungsi composable yang mengintegrasikan ViewModel dengan ProfileScreen.
 * State Profile diambil dari ProfileViewModel.
 */
@Composable
fun ProfileScreenWithViewModel(onBack: () -> Unit) {
    // Mendapatkan instance ProfileViewModel melalui viewModel() composition
    val profileViewModel: ProfileViewModel = viewModel()
    // Amati state Profile dari ViewModel
    val uiState = profileViewModel.uiState.value

    // Jika resource photo belum di-set, gunakan placeholder sebagai default
    val profilePhotoRes = uiState.profilePhoto ?: R.drawable.profile_placeholder

    // Panggil ProfileScreen dengan data dari uiState
    ProfileScreen(
        username = uiState.username,
        email = uiState.email,
        profilePhoto = profilePhotoRes,
        songsAdded = uiState.songsAdded,
        likedSongs = uiState.likedSongs,
        listenedSongs = uiState.listenedSongs,
        onBack = onBack
    )
}

/**
 * ProfileScreen yang menampilkan informasi profil pengguna.
 */
@Composable
fun ProfileScreen(
    username: String,
    email: String,
    profilePhoto: Int, // Drawable resource untuk foto profil
    songsAdded: Int,
    likedSongs: Int,
    listenedSongs: Int,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header: Profile Image & User Data
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = profilePhoto),
                contentDescription = "Profile Photo",
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = username,
                    style = MaterialTheme.typography.h5
                )
                Text(
                    text = email,
                    style = MaterialTheme.typography.body1
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Statistics Row: Songs Added, Liked Songs, and Listened Songs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$songsAdded",
                    style = MaterialTheme.typography.h6
                )
                Text(
                    text = "Songs Added",
                    style = MaterialTheme.typography.body2
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$likedSongs",
                    style = MaterialTheme.typography.h6
                )
                Text(
                    text = "Liked Songs",
                    style = MaterialTheme.typography.body2
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$listenedSongs",
                    style = MaterialTheme.typography.h6
                )
                Text(
                    text = "Listened Songs",
                    style = MaterialTheme.typography.body2
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Tombol Back
        Button(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(text = "Back")
        }
    }
}

@Preview(showBackground = true, name = "Profile Screen Preview")
@Composable
fun ProfileScreenPreview() {
    ProfileScreen(
        username = "John Doe",
        email = "john.doe@example.com",
        profilePhoto = R.drawable.profile_placeholder, // Pastikan resource ini ada di res/drawable
        songsAdded = 25,
        likedSongs = 40,
        listenedSongs = 120,
        onBack = {}
    )
}