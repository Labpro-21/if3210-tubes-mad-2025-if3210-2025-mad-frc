package com.example.purrytify.ui

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.purrytify.model.Song
import com.example.purrytify.viewmodel.SongViewModel
import com.example.purrytify.data.UserDao
import com.example.purrytify.repository.UserRepository
import java.util.Date

import com.example.purrytify.utils.SessionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsertSongPopUp(
    songViewModel: SongViewModel,
    song: Song? = null,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState()
    var showSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(if (song == null) "Upload Song" else "Edit Song", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))

                var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
                var selectedAudioUri by remember { mutableStateOf<Uri?>(null) }

                var title by rememberSaveable { mutableStateOf(song?.title ?: "") }
                var artist by rememberSaveable { mutableStateOf(song?.artist ?: "") }
                var duration by rememberSaveable { mutableStateOf(song?.duration ?: 0L) }

                LaunchedEffect(selectedAudioUri) {
                    selectedAudioUri?.let { uri ->
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(context, uri)

                        val metaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: ""
                        val metaArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: ""
                        val metaduration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L

                        if (title.isBlank()) title = metaTitle
                        if (artist.isBlank()) artist = metaArtist
                        duration = metaduration

                        retriever.release()
                    }
                }

                // Autofill the selected URIs if song is provided
                LaunchedEffect(song) {
                    song?.let {
                        selectedAudioUri = Uri.parse(it.audioPath)
                        selectedPhotoUri = Uri.parse(it.artworkPath)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    UploadBoxWithButton(
                        selectedUri = selectedPhotoUri,
                        onFileSelected = { selectedPhotoUri = it },
                        mimeType = "image/*",
                        text = "Upload Photo"
                    )
                    UploadBoxWithButton(
                        selectedUri = selectedAudioUri,
                        onFileSelected = { selectedAudioUri = it },
                        mimeType = "audio/*",
                        text = "Upload File"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Duration: ${duration / 1000} seconds",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Title", style = MaterialTheme.typography.titleSmall, modifier = Modifier.align(Alignment.Start))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Artist", style = MaterialTheme.typography.titleSmall, modifier = Modifier.align(Alignment.Start))
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(onClick = { showSheet = false }, modifier = Modifier.width(150.dp)) {
                        Text("Cancel", style = MaterialTheme.typography.titleMedium)
                    }
                    Button(onClick = {
                        handleSaveSong(
                            context = context,
                            selectedAudioUri = selectedAudioUri,
                            selectedPhotoUri = selectedPhotoUri,
                            title = title,
                            artist = artist,
                            duration = duration,
                            songViewModel = songViewModel,
                            song = song, // Passing the song object to handle edit or add
                            onComplete = { showSheet = false }
                        )
                    }, modifier = Modifier.width(150.dp)) {
                        Text("Save", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }

    if (song == null){
        Box(modifier = Modifier.fillMaxSize()) {
            FloatingActionButton(
                onClick = { showSheet = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Show Popup")
            }
        }
    }else{
//        IconButton(onClick = { showSheet = true }) {
//            Icon(Icons.Default.Edit, contentDescription = "Edit")
//        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showSheet=true }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Edit, contentDescription = "Edit")
            Spacer(modifier = Modifier.width(16.dp))
            Text("Edit Song", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

fun handleSaveSong(
    context: Context,
    selectedAudioUri: Uri?,
    selectedPhotoUri: Uri?,
    title: String,
    artist: String,
    duration: Long,
    songViewModel: SongViewModel,
    song: Song? = null, // Parameter song untuk edit
    onComplete: () -> Unit
) {
    if (selectedAudioUri != null) {
        val sessionManager = SessionManager(context)
        val currentUserId = sessionManager.getUserId()
        val songToSave = Song(
            id = song?.id ?: 0, // Gunakan ID dari song yang ada atau 0 untuk lagu baru
            title = title,
            artist = artist,
            duration = duration,
            artworkPath = selectedPhotoUri.toString(),
            audioPath = selectedAudioUri.toString(),
            lastPlayed = Date(),
            userId = currentUserId,
            )

        if (song != null) {
            songViewModel.updateSong(songToSave) // Jika song ada, update
        } else {
            songViewModel.addSong(songToSave) // Jika song null, tambahkan baru
        }
    }

    onComplete() // misalnya untuk menutup sheet atau update UI
}


@Composable
fun UploadBoxDisplay(fileUri: Uri?, text: String, mimeType: String) {
    val context = LocalContext.current
    val isImage = mimeType.startsWith("image")

    Box(
        modifier = Modifier
            .width(150.dp)
            .height(150.dp)
            .padding(16.dp)
            .border(BorderStroke(2.dp, Color.Gray), shape = RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        when {
            fileUri != null && isImage -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(fileUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Selected Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            fileUri != null -> {
                val fileName = getFileNameFromUri(context,fileUri)
                Text(
                    text = "File selected:\n${fileName}",
                    color = Color.DarkGray,
                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .fillMaxWidth()
                )

            }

            else -> {
                Text(text, color = Color.Gray)
            }
        }
    }
}

@Composable
fun UploadBoxWithButton(
    selectedUri: Uri?,
    onFileSelected: (Uri) -> Unit,
    mimeType: String,
    text: String
) {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(it, takeFlags)
            } catch (e: SecurityException) {
                e.printStackTrace()
            }

            onFileSelected(it)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        UploadBoxDisplay(fileUri = selectedUri, text = text, mimeType = mimeType)
        Spacer(modifier = Modifier.height(4.dp))
        Button(onClick = { launcher.launch(arrayOf(mimeType)) }) { // array karena OpenDocument
            Text("Choose File")
        }
    }
}


fun handleSaveSong(
    context: Context,
    selectedAudioUri: Uri?,
    selectedPhotoUri: Uri?,
    title: String,
    artist: String,
    duration: Long,
    songViewModel: SongViewModel,
    onComplete: () -> Unit,
    song: Song? = null, // Parameter song untuk edit

) {
    if (selectedAudioUri != null) {
        val sessionManager = SessionManager(context)
        val currentUserId = sessionManager.getUserId()
        val songToSave = Song(
            id = song?.id ?: 0, // Gunakan ID dari song yang ada atau 0 untuk lagu baru
            title = if (title.isBlank()) "Unnamed Song" else title,
            artist = if (artist.isBlank()) "Unnamed Artist" else artist,
            duration = duration,
            artworkPath = selectedPhotoUri.toString(),
            audioPath = selectedAudioUri.toString(),
            addedDate = Date(),
            lastPlayed = null,
            userId = currentUserId,
        )

        if (song != null) {
            songViewModel.updateSong(songToSave) // Jika song ada, update
        } else {
            songViewModel.addSong(songToSave) // Jika song null, tambahkan baru
        }
    }

    onComplete() // misalnya untuk menutup sheet atau update UI
}

fun getFileNameFromUri(context: Context, uri: Uri): String {
    val returnCursor = context.contentResolver.query(uri, null, null, null, null)
    returnCursor?.use {
        val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        it.moveToFirst()
        return it.getString(nameIndex)
    }
    return "Unknown file"
}


