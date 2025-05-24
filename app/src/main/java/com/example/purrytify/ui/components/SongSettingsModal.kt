package com.example.purrytify.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.purrytify.R
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.purrytify.model.Song
import com.example.purrytify.utils.ImageSharer
import com.example.purrytify.utils.QrCodeGenerator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongSettingsModal(
    song: Song?, // Tambahkan parameter song
    visible: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShareUrl: () -> Unit, // Untuk share URL biasa
    // onShareQr: () -> Unit, // Kita akan handle langsung di sini
    isOnlineSong: Boolean = false
) {
    if (!visible || song == null) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Opsi Edit
            if (!isOnlineSong && song.isExplicitlyAdded) { // Edit hanya untuk lagu lokal eksplisit
                ListItem(
                    headlineContent = { Text("Edit Song") },
                    leadingContent = { Icon(Icons.Default.Edit, contentDescription = "Edit Song") },
                    modifier = Modifier.clickable {
                        onEdit()
                        onDismiss()
                    }
                )
            }

            // Opsi Share URL (hanya untuk lagu server)
            if (song.serverId != null) { // Cek serverId
                ListItem(
                    headlineContent = { Text("Download Song") },
                    leadingContent = {
                        Icon(Icons.Default.Download, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        onDismiss()
                    }
                )

                ListItem(
                    headlineContent = { Text("Share via URL") },
                    leadingContent = { Icon(Icons.Default.Share, contentDescription = "Share via URL") },
                    modifier = Modifier.clickable {
                        onShareUrl()
                        onDismiss()
                    }
                )

                // Opsi Share QR (hanya untuk lagu server)
                ListItem(
                    headlineContent = { Text("Share via QR Code") },
                    leadingContent = { Icon(Icons.Filled.QrCode2, contentDescription = "Share via QR Code") },
                    modifier = Modifier.clickable {
                        scope.launch { // Jalankan di coroutine karena ada operasi file
                            val deepLink = "purrytify://song/${song.serverId}"
                            val qrBitmap = QrCodeGenerator.generateQrBitmap(deepLink)
                            if (qrBitmap != null) {
                                val imageUri = ImageSharer.saveBitmapToCache(context, qrBitmap, "song_${song.serverId}_qr.png")
                                if (imageUri != null) {
                                    ImageSharer.shareImageUri(context, imageUri, "Share ${song.title} QR")
                                }
                            }
                            onDismiss()
                        }
                    }
                )
            }


            // Opsi Delete (hanya untuk lagu lokal eksplisit)
            if (!isOnlineSong && song.isExplicitlyAdded) {
                ListItem(
                    headlineContent = { Text("Delete Song") },
                    leadingContent = { Icon(Icons.Default.Delete, contentDescription = "Delete Song") },
                    modifier = Modifier.clickable {
                        onDelete()
                        onDismiss()
                    }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}