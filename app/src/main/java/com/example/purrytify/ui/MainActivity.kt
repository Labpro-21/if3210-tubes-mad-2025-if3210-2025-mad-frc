package com.example.purrytify

import android.content.pm.PackageManager
import android.os.Bundle
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.example.purrytify.data.AppDatabase
import com.example.purrytify.repository.SongRepository
import com.example.purrytify.ui.navigation.AppNavigation
import com.example.purrytify.ui.theme.PurrytifyTheme
import com.example.purrytify.utils.SessionManager
import com.example.purrytify.viewmodel.OnlineSongViewModel
import com.example.purrytify.viewmodel.OnlineSongViewModelFactory
import com.example.purrytify.viewmodel.PlayerViewModel
import com.example.purrytify.viewmodel.PlayerViewModelFactory
import com.example.purrytify.viewmodel.SongViewModel
import com.example.purrytify.viewmodel.SongViewModelFactory
import com.example.purrytify.network.RetrofitClient
import com.example.purrytify.utils.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.example.purrytify.viewmodel.AudioOutputViewModel
import com.example.purrytify.viewmodel.AudioOutputViewModelFactory
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import com.example.purrytify.utils.MusicServiceManager
import com.example.purrytify.viewmodel.RecommendationViewModel
import com.example.purrytify.viewmodel.RecommendationViewModelFactory
import javax.inject.Inject

class MainActivity : ComponentActivity() {


    private val songViewModel: SongViewModel by viewModels {
        val sm = SessionManager(applicationContext)
        val userIdForFactory = sm.getUserId().let { if (it <= 0) 0 else it } // Default ke 0 jika tidak valid
        Log.d("MainActivity_VM", "MainActivity creating SongViewModel with factory for userId: $userIdForFactory")
        SongViewModelFactory(
            SongRepository(AppDatabase.getDatabase(applicationContext).songDao(), AppDatabase.getDatabase(applicationContext).userDao()),
            userIdForFactory,
            application,
        )
    }
    
    private val recommendationViewModel: RecommendationViewModel by viewModels {
        RecommendationViewModelFactory(application, onlineSongViewModel)
    }
    private val playerViewModel: PlayerViewModel by viewModels {
        PlayerViewModelFactory(application)
    }
    private val onlineSongViewModel: OnlineSongViewModel by viewModels {
        val tm = TokenManager(applicationContext)
        val sm = SessionManager(applicationContext)
        OnlineSongViewModelFactory(RetrofitClient.create(tm), sm,application)
    }

    private val audioOutputViewModel: AudioOutputViewModel by viewModels {
        AudioOutputViewModelFactory(application)
    }

    private lateinit var qrScanLauncher: ActivityResultLauncher<ScanOptions>

    private val audioDeviceChangeReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            Log.d("MainActivityAudioReceiver", "Received action: $action")

            when (action) {
                AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                    Log.d("MainActivityAudioReceiver", "ACTION_AUDIO_BECOMING_NOISY: Headset disconnected.")
                    Toast.makeText(context, "Output disconnected, switching to speaker.", Toast.LENGTH_SHORT).show()
                    playerViewModel.revertToDefaultAudioOutput()
                    audioOutputViewModel.loadAvailableOutputDevices()
                }
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) as BluetoothDevice?
                    }
                    device?.let { connectedBluetoothDevice ->
                        Log.d("MainActivityAudioReceiver", "ACTION_ACL_CONNECTED: ${connectedBluetoothDevice.name}")


                        audioOutputViewModel.loadAvailableOutputDevices()


                        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        val allOutputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                        val correspondingAudioDevice = allOutputDevices.find { audioDevInfo ->

                            (audioDevInfo.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || audioDevInfo.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) &&
                                    (audioDevInfo.address == connectedBluetoothDevice.address || audioDevInfo.productName?.toString()?.equals(connectedBluetoothDevice.name, ignoreCase = true) == true)
                        }

                        if (correspondingAudioDevice != null) {
                            Log.d("MainActivityAudioReceiver", "Found AudioDeviceInfo for connected BT: ${correspondingAudioDevice.productName}. Setting as preferred.")

                            playerViewModel.setPreferredAudioOutput(correspondingAudioDevice)

                        } else {
                            Log.w("MainActivityAudioReceiver", "Could not find matching AudioDeviceInfo for newly connected BT device: ${connectedBluetoothDevice.name}")


                        }
                    }
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) as BluetoothDevice?
                    }
                    device?.let { disconnectedBluetoothDevice ->
                        Log.d("MainActivityAudioReceiver", "ACTION_ACL_DISCONNECTED: ${disconnectedBluetoothDevice.name}")
                        val currentActiveDevice = playerViewModel.activeAudioDevice.value

                        if (currentActiveDevice != null &&
                            (currentActiveDevice.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || currentActiveDevice.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) &&
                            (currentActiveDevice.address == disconnectedBluetoothDevice.address || currentActiveDevice.productName?.toString()?.equals(disconnectedBluetoothDevice.name, ignoreCase = true) == true)
                        ) {
                            Log.d("MainActivityAudioReceiver", "Active Bluetooth device disconnected. Reverting to default.")
                            Toast.makeText(context, "Bluetooth disconnected, switching to speaker.", Toast.LENGTH_SHORT).show()
                            playerViewModel.revertToDefaultAudioOutput()
                        }
                        audioOutputViewModel.loadAvailableOutputDevices()
                    }
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Notifikasi dinonaktifkan", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Log.d("ViewModelInstance", "MainActivity - SongViewModel hash: ${System.identityHashCode(songViewModel)}")
        Log.d("ViewModelInstance", "MainActivity - PlayerViewModel hash: ${System.identityHashCode(playerViewModel)}")
        Log.d("ViewModelInstance", "MainActivity - OnlineSongViewModel hash: ${System.identityHashCode(onlineSongViewModel)}")

        qrScanLauncher = registerForActivityResult(ScanContract()) { result ->
            if (result.contents == null) {
                Log.d("MainActivity_QR", "QR Scan cancelled")
                Toast.makeText(this, "Scan dibatalkan", Toast.LENGTH_SHORT).show()
            } else {
                Log.d("MainActivity_QR", "QR Scan successful: ${result.contents}")
                handleScannedQrCode(result.contents)
            }
        }

        val filter = IntentFilter().apply {
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(audioDeviceChangeReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(audioDeviceChangeReceiver, filter)
        }

        playerViewModel.onPlaybackSecondTick = {



            if (songViewModel.currentSong.value != null && songViewModel.currentSong.value!!.id != 0) {
                Log.d("MainActivity_Ticker", "Playback tick received from PlayerViewModel. Calling SongViewModel.recordPlayTick(). Current song: ${songViewModel.currentSong.value?.title}")
                songViewModel.recordPlayTick()
            } else {
                Log.w("MainActivity_Ticker", "Playback tick received, but SongViewModel.current_song is null or invalid. Skipping recordPlayTick.")
            }
        }

        setContent {
            PurrytifyTheme {
                AppNavigation(
                    songViewModel = this.songViewModel,
                    playerViewModel = this.playerViewModel,
                    onlineSongViewModel = this.onlineSongViewModel,
                    audioOutputViewModel = this.audioOutputViewModel,
                    recommendationViewModel = this.recommendationViewModel,
                    onScanQrClicked = { launchQrScanner() }
                )
                requestNotificationPermission()

            }
        }
        handleIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(audioDeviceChangeReceiver)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d("MainActivity_Lifecycle", "onNewIntent called with intent: $intent")
        intent?.let {
            setIntent(it)
            handleIntent(it)
        }
    }

    fun launchQrScanner() {
        Log.d("MainActivity_QR", "launchQrScanner() CALLED by UI")
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setPrompt("Scan a Purrytify Song QR Code")
        options.setCameraId(0)
        options.setBeepEnabled(true)
        options.setBarcodeImageEnabled(true)
        options.setOrientationLocked(false)
        qrScanLauncher.launch(options)
    }

    private fun handleScannedQrCode(scannedData: String) {
        Log.d("MainActivity_DeepLink", "Scanned QR Data: $scannedData")
        try {
            val scannedUri = scannedData.toUri()
            if (scannedUri.scheme == "purrytify" && scannedUri.host == "song") {
                val intent = Intent(Intent.ACTION_VIEW, scannedUri)
                handleIntent(intent)
            } else {
                Log.w("MainActivity_DeepLink", "Scanned QR is not a valid Purrytify song link: $scannedData")
                Toast.makeText(this, "Invalid Purrytify QR Code", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e("MainActivity_DeepLink", "Error parsing scanned QR data: $scannedData", e)
            Toast.makeText(this, "Error reading QR Code", Toast.LENGTH_LONG).show()
        }
    }

    private fun handleIntent(intent: Intent) {
        val action: String? = intent.action
        val data: Uri? = intent.data
        Log.d("MainActivity_DeepLink", "Handling Intent - Action: $action, Data: $data. Using SongVM hash: ${System.identityHashCode(songViewModel)}")

        if (Intent.ACTION_VIEW == action && data != null) {
            val scheme = data.scheme
            val host = data.host
            val pathSegments = data.pathSegments

            Log.d("MainActivity_DeepLink", "Intent URI - Scheme: $scheme, Host: $host, Segments: $pathSegments")

            if (scheme == "purrytify" && host == "song" && pathSegments.isNotEmpty()) {
                val songIdString = pathSegments.first()
                val songId = songIdString.toIntOrNull()

                if (songId != null) {
                    Log.i("MainActivity_DeepLink", "Processing song ID from deep link/QR: $songId")
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            val songToPlay = onlineSongViewModel.fetchSongById(songId)
                            if (songToPlay != null) {
                                Log.i("MainActivity_DeepLink", "Song fetched: ${songToPlay.title}. Calling setCurrentSong on VM hash: ${System.identityHashCode(songViewModel)}")
                                songViewModel.setCurrentSong(songToPlay)
                                songToPlay.audioPath.let { audioPathString ->
                                    try {
                                        val audioUri = audioPathString.toUri()
                                        if (songToPlay.isExplicitlyAdded == false){
                                            onlineSongViewModel.sendSongsToMusicService()
                                            playerViewModel.prepareAndPlay(onlineSongViewModel.getSongIndex(songToPlay))

                                        }else{
                                            songViewModel.sendSongsToMusicService()
                                            playerViewModel.prepareAndPlay(songViewModel.getSongIndex(songToPlay))

                                        }
                                        // TODO: Secara otomatis tampilkan UI player
                                    } catch (e: Exception) {
                                        Log.e("MainActivity_DeepLink", "Invalid audio path URI: $audioPathString", e)
                                        Toast.makeText(this@MainActivity, "Error playing song: Invalid audio path", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Log.e("MainActivity_DeepLink", "Failed to fetch song with ID: $songId from server.")
                                Toast.makeText(this@MainActivity, "Song not found", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Log.e("MainActivity_DeepLink", "Error processing song ID $songId: ${e.message}", e)
                            Toast.makeText(this@MainActivity, "Error processing song", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Log.e("MainActivity_DeepLink", "Invalid song ID format in deep link/QR: $songIdString")
                    Toast.makeText(this, "Invalid song ID in QR", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.w("MainActivity_DeepLink", "Intent data not a Purrytify song link. Scheme: $scheme, Host: $host")
            }
        } else {
             Log.d("MainActivity_DeepLink", "Intent not for VIEW action or no data. Action: $action")
        }
    }
}
