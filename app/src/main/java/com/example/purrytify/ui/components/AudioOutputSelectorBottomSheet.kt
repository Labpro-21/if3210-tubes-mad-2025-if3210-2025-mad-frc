package com.example.purrytify.ui.components

import android.media.AudioDeviceInfo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.purrytify.viewmodel.AudioOutputViewModel
import com.example.purrytify.viewmodel.AudioOutputViewModelFactory
import com.example.purrytify.viewmodel.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioOutputSelectorBottomSheet(
    playerViewModel: PlayerViewModel,
    audioOutputViewModel: AudioOutputViewModel = viewModel(
        factory = AudioOutputViewModelFactory(LocalContext.current.applicationContext as android.app.Application)
    ),
    onDismiss: () -> Unit
) {
    val availableDevices by audioOutputViewModel.availableOutputDevices.collectAsState()
    val activeDevice by playerViewModel.activeAudioDevice.collectAsState() // Dari PlayerViewModel

    LaunchedEffect(Unit) {
        audioOutputViewModel.loadAvailableOutputDevices()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true) // Bisa disesuaikan
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Pilih Output Device",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (availableDevices.isEmpty()) {
                Text("Tidak ada perangkat audio eksternal terdeteksi.")
            } else {
                LazyColumn {
                    items(availableDevices) { device ->
                        AudioDeviceItem(
                            deviceInfo = device,
                            deviceName = audioOutputViewModel.getDeviceName(device),
                            deviceTypeString = audioOutputViewModel.getDeviceTypeString(device),
                            isSelected = device.id == activeDevice?.id,
                            onDeviceSelected = {
                                playerViewModel.setPreferredAudioOutput(it)
                                onDismiss() // Tutup sheet setelah memilih
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text("Tutup")
            }
        }
    }
}

@Composable
fun AudioDeviceItem(
    deviceInfo: AudioDeviceInfo,
    deviceName: String,
    deviceTypeString: String,
    isSelected: Boolean,
    onDeviceSelected: (AudioDeviceInfo) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDeviceSelected(deviceInfo) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = getDeviceIcon(deviceInfo.type),
            contentDescription = deviceTypeString,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = deviceName, style = MaterialTheme.typography.bodyLarge)
            Text(text = deviceTypeString, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

fun getDeviceIcon(deviceType: Int): ImageVector {
    return when (deviceType) {
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> Icons.Filled.Bluetooth
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET -> Icons.Filled.Headset
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> Icons.Filled.Speaker // Atau ikon yang lebih spesifik
        else -> Icons.Filled.Speaker // Default
    }
}