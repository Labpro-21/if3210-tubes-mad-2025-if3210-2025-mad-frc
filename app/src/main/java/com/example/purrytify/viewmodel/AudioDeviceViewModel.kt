package com.example.purrytify.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AudioOutputDevice(
    val id: Int, // Bisa jadi AudioDeviceInfo.id atau hash code untuk BluetoothDevice
    val name: String,
    val type: Int, // Gunakan konstanta dari AudioDeviceInfo (TYPE_BLUETOOTH_A2DP, TYPE_BUILTIN_SPEAKER, dll.)
    val isConnected: Boolean, // Untuk Bluetooth
    val underlyingDevice: Any // Bisa BluetoothDevice atau AudioDeviceInfo
)

class AudioDeviceViewModel(application: Application) : AndroidViewModel(application) {

    private val _availableDevices = MutableStateFlow<List<AudioOutputDevice>>(emptyList())
    val availableDevices: StateFlow<List<AudioOutputDevice>> = _availableDevices.asStateFlow()

    private val _currentOutputDevice = MutableStateFlow<AudioOutputDevice?>(null)
    val currentOutputDevice: StateFlow<AudioOutputDevice?> = _currentOutputDevice.asStateFlow()

    private val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val bluetoothManager = application.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission") // Izin akan diperiksa sebelum mendaftarkan receiver
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED,
                BluetoothDevice.ACTION_ACL_DISCONNECTED,
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    Log.d("AudioDeviceVM", "Bluetooth state changed, refreshing devices.")
                    refreshAudioDevices()
                }
            }
        }
    }

    init {
        Log.d("AudioDeviceVM", "Initializing AudioDeviceViewModel")
        refreshAudioDevices()
        registerBluetoothStateReceiver()
    }

    @SuppressLint("MissingPermission")
    fun refreshAudioDevices() {
        viewModelScope.launch {
            val devices = mutableListOf<AudioOutputDevice>()
            val context = getApplication<Application>().applicationContext

            // 1. Perangkat Output Saat Ini (Built-in Speaker, Earpiece, Wired Headset, A2DP)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val outputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                var activeDeviceFoundByAudioManager = false
                outputDevices.forEach { deviceInfo ->
                    val name = deviceInfo.productName?.toString() ?: "Device ${deviceInfo.id}"
                    // Prioritaskan A2DP (Bluetooth audio berkualitas tinggi)
                    if (deviceInfo.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP) {
                        // Cek apakah ini adalah perangkat Bluetooth yang aktif
                        if (audioManager.isBluetoothA2dpOn) {
                             val device = AudioOutputDevice(
                                id = deviceInfo.id,
                                name = "BT: $name", // Tambahkan prefix untuk kejelasan
                                type = deviceInfo.type,
                                isConnected = true,
                                underlyingDevice = deviceInfo
                            )
                            devices.add(device)
                            _currentOutputDevice.value = device // Asumsikan A2DP yang aktif adalah output utama
                            activeDeviceFoundByAudioManager = true
                            Log.d("AudioDeviceVM", "Found active A2DP device: $name")
                        }
                    } else if (deviceInfo.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER && !activeDeviceFoundByAudioManager) {
                        // Tambahkan speaker internal sebagai default jika tidak ada A2DP aktif
                        val device = AudioOutputDevice(
                            id = deviceInfo.id,
                            name = name,
                            type = deviceInfo.type,
                            isConnected = true, // Speaker internal selalu terhubung
                            underlyingDevice = deviceInfo
                        )
                        devices.add(device)
                        if (_currentOutputDevice.value == null || _currentOutputDevice.value?.type != AudioDeviceInfo.TYPE_BLUETOOTH_A2DP) {
                           _currentOutputDevice.value = device
                        }
                        Log.d("AudioDeviceVM", "Found built-in speaker: $name")
                    }
                    // Bisa tambahkan tipe lain seperti TYPE_WIRED_HEADSET, TYPE_USB_DEVICE, dll.
                }
            }

            // 2. Perangkat Bluetooth yang Terhubung (Paired & Connected)
            // Ini mungkin tumpang tindih dengan A2DP di atas, tapi bisa memberi info lebih
            if (hasBluetoothPermissions()) {
                bluetoothAdapter?.bondedDevices?.forEach { btDevice ->
                    // Cek apakah perangkat ini sudah ada di list dari AudioManager (via A2DP)
                    val alreadyAdded = devices.any {
                        (it.underlyingDevice is AudioDeviceInfo && it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP && getBluetoothDeviceName(it.underlyingDevice as AudioDeviceInfo) == btDevice.name) ||
                        (it.underlyingDevice is BluetoothDevice && it.underlyingDevice.address == btDevice.address)
                    }

                    if (!alreadyAdded) {
                        // Cek apakah terhubung (ini mungkin perlu cara yang lebih baik)
                        val isConnected = BluetoothAdapter.getDefaultAdapter().getProfileConnectionState(android.bluetooth.BluetoothProfile.A2DP) == BluetoothAdapter.STATE_CONNECTED
                        // Atau cek btDevice.isConnected() jika API memungkinkan dan reliable

                        devices.add(
                            AudioOutputDevice(
                                id = btDevice.address.hashCode(), // Gunakan hash dari MAC address sebagai ID unik
                                name = "BT: ${btDevice.name ?: "Unknown Bluetooth Device"}",
                                type = AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, // Asumsikan A2DP jika bonded
                                isConnected = isConnected, // Ini perlu diverifikasi lebih lanjut
                                underlyingDevice = btDevice
                            )
                        )
                        Log.d("AudioDeviceVM", "Found bonded Bluetooth device: ${btDevice.name}")
                    }
                }
            } else {
                Log.w("AudioDeviceVM", "Bluetooth permissions not granted. Cannot list bonded BT devices.")
            }


            // Pastikan speaker internal selalu ada sebagai fallback jika tidak ada perangkat output lain yang aktif
            if (devices.none { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }) {
                 devices.add(
                    AudioOutputDevice(
                        id = Int.MAX_VALUE -1, // ID unik placeholder
                        name = "Device Speaker",
                        type = AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
                        isConnected = true,
                        underlyingDevice = "BuiltInSpeakerPlaceholder"
                    )
                )
            }


            // Urutkan: perangkat terhubung di atas, lalu berdasarkan nama
            _availableDevices.value = devices.sortedWith(compareByDescending<AudioOutputDevice> { it.isConnected }.thenBy { it.name })

            // Update current output device jika belum ada atau jika yang sekarang tidak terhubung lagi
            if (_currentOutputDevice.value == null || _availableDevices.value.none { it.id == _currentOutputDevice.value!!.id && it.isConnected }) {
                _currentOutputDevice.value = _availableDevices.value.firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP && it.isConnected }
                    ?: _availableDevices.value.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
            }
            Log.d("AudioDeviceVM", "Refreshed devices. Count: ${devices.size}. Current Output: ${_currentOutputDevice.value?.name}")

            val currentOutputSnapshot = _currentOutputDevice.value
            if (currentOutputSnapshot == null || _availableDevices.value.none { it.id == currentOutputSnapshot.id && it.isConnected }) {
                val newCurrent = _availableDevices.value.firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP && it.isConnected }
                    ?: _availableDevices.value.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                if (_currentOutputDevice.value?.id != newCurrent?.id) { // Hanya update jika berbeda
                    _currentOutputDevice.value = newCurrent
                }
            }
            Log.d("AudioDeviceVM", "Refreshed devices. Count: ${_availableDevices.value.size}. Current Output: ${_currentOutputDevice.value?.name}")
        }
    }

    @SuppressLint("NewApi", "MissingPermission") // Izin sudah dicek di hasBluetoothPermissions
    private fun getBluetoothDeviceName(deviceInfo: AudioDeviceInfo): String? {
        // Mencoba mendapatkan nama dari BluetoothDevice jika tersedia (membutuhkan beberapa refleksi atau cara lain jika tidak langsung)
        // Untuk A2DP, productName biasanya sudah cukup.
        return deviceInfo.productName?.toString()
    }


    // Fungsi untuk memilih perangkat output (ini lebih kompleks untuk ExoPlayer)
    // ExoPlayer secara default akan mengikuti routing audio sistem.
    // Untuk secara manual memilih output device di ExoPlayer (misal, spesifik BluetoothDevice),
    // Anda mungkin perlu menggunakan setAudioDeviceInfo (API 31+) atau mengelola koneksi Bluetooth secara manual.
    // Untuk saat ini, kita akan mengandalkan sistem Android untuk routing otomatis saat BT terhubung.
    // Fungsi ini bisa digunakan untuk memberi tahu PlayerViewModel atau sistem.
    fun selectOutputDevice(device: AudioOutputDevice, playerViewModel: PlayerViewModel) {
        Log.d("AudioDeviceVM", "Attempting to select output device: ${device.name}")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && device.underlyingDevice is AudioDeviceInfo) {
            // Untuk API 31+, ExoPlayer bisa diarahkan ke AudioDeviceInfo tertentu
            // playerViewModel.setAudioOutputDevice(device.underlyingDevice) // Anda perlu implementasi ini di PlayerViewModel
            Log.i("AudioDeviceVM", "Device selection for API 31+ might require PlayerViewModel.setAudioOutputDevice()")
            // Untuk sekarang, kita update state saja dan biarkan sistem menangani jika BT device
             _currentOutputDevice.value = device
        } else if (device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP && device.underlyingDevice is BluetoothDevice) {
            // Untuk API < 31, jika ini Bluetooth, sistem biasanya otomatis switch jika terhubung.
            // Kita hanya update state di sini.
            _currentOutputDevice.value = device
            Log.i("AudioDeviceVM", "Bluetooth device selected. System should handle audio routing if connected.")
        } else if (device.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
            // Jika memilih speaker internal, mungkin perlu disconnect dari BT A2DP jika ingin paksa
            // Ini bisa dilakukan dengan AudioManager.setBluetoothA2dpOn(false) - hati-hati, ini global
            _currentOutputDevice.value = device
             // AudioManager bisa digunakan untuk mencoba mengalihkan fokus audio, tapi ExoPlayer biasanya mengikuti default.
            // audioManager.isSpeakerphoneOn = true // Ini untuk mode speakerphone telepon, bukan output media umum
            Log.i("AudioDeviceVM", "Built-in speaker selected.")
        }
        _currentOutputDevice.value = device
        Log.i("AudioDeviceVM", "Selected output device state updated to: ${device.name}")
        refreshAudioDevices()
    }

    private fun hasBluetoothPermissions(): Boolean {
        val context = getApplication<Application>().applicationContext
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
        }
    }

    @SuppressLint("MissingPermission")
    private fun registerBluetoothStateReceiver() {
        if (!hasBluetoothPermissions()) {
            Log.w("AudioDeviceVM", "Cannot register Bluetooth state receiver, permissions missing.")
            return
        }
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED) // Untuk on/off Bluetooth
        }
        getApplication<Application>().registerReceiver(bluetoothStateReceiver, filter)
        Log.d("AudioDeviceVM", "BluetoothStateReceiver registered.")
    }

    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<Application>().unregisterReceiver(bluetoothStateReceiver)
            Log.d("AudioDeviceVM", "BluetoothStateReceiver unregistered.")
        } catch (e: Exception) {
            Log.w("AudioDeviceVM", "Error unregistering BluetoothStateReceiver: ${e.message}")
        }
    }
}

// Factory untuk AudioDeviceViewModel
class AudioDeviceViewModelFactory(private val application: Application) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AudioDeviceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AudioDeviceViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}