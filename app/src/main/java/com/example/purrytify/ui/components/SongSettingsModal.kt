package com.example.purrytify.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongSettingsModal(
    visible: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    isOnlineSong: Boolean = false
) {
    if (!visible) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Edit option (only for local songs)
            if (!isOnlineSong) {
                ListItem(
                    headlineContent = { Text("Edit Song") },
                    leadingContent = { 
                        Icon(Icons.Default.Edit, contentDescription = null) 
                    },
                    modifier = Modifier.clickable { 
                        onEdit()
                        onDismiss()
                    }
                )
            }

            // Share option (only for online songs)
            if (isOnlineSong) {
                ListItem(
                    headlineContent = { Text("Share") },
                    leadingContent = { 
                        Icon(Icons.Default.Share, contentDescription = null) 
                    },
                    modifier = Modifier.clickable { 
                        onShare()
                        onDismiss()
                    }
                )
            }

            // Delete option (only for local songs)
            if (!isOnlineSong) {
                ListItem(
                    headlineContent = { Text("Delete Song") },
                    leadingContent = { 
                        Icon(Icons.Default.Delete, contentDescription = null) 
                    },
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