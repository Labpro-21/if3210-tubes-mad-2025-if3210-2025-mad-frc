package com.example.purrytify.viewmodel

import android.annotation.SuppressLint
import android.app.Application



import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager

import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.util.Log

class AudioOutputViewModel(application: Application) : AndroidViewModel(application) {

    private val audioManager =
        application.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _availableOutputDevices = MutableStateFlow<List<AudioDeviceInfo>>(emptyList())
    val availableOutputDevices: StateFlow<List<AudioDeviceInfo>> = _availableOutputDevices.asStateFlow()

    init {
        loadAvailableOutputDevices()
    }

    @SuppressLint("MissingPermission")
    fun loadAvailableOutputDevices() {
        val allOutputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val relevantDevices = mutableListOf<AudioDeviceInfo>()


        allOutputDevices.find { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER && it.isSink }?.let {
            relevantDevices.add(it)
        }


        val bluetoothDevicesA2DP = allOutputDevices.filter {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP && it.isSink
        }


        val distinctBluetoothNames = bluetoothDevicesA2DP.mapNotNull { it.productName?.toString() }.distinct()
        distinctBluetoothNames.forEach { name ->
            bluetoothDevicesA2DP.firstOrNull { it.productName?.toString() == name }?.let {
                relevantDevices.add(it)
            }
        }





        val wiredHeadphones = allOutputDevices.filter {
            (it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET) && it.isSink
        }
        relevantDevices.addAll(wiredHeadphones)









        _availableOutputDevices.value = relevantDevices.distinctBy { it.id }
        Log.d("AudioOutputVM", "Filtered devices: ${_availableOutputDevices.value.joinToString { (it.productName?.toString() ?: "ID: ${it.id}") + " Type: " + getDeviceTypeString(it) }}")
    }

    fun getDeviceName(deviceInfo: AudioDeviceInfo): String {
        return when (deviceInfo.type) {
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Device Speaker"
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> deviceInfo.productName?.toString() ?: "Bluetooth Device"
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired Headphones"
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired Headset"

            else -> deviceInfo.productName?.toString() ?: "Unknown Device ${deviceInfo.id}"
        }
    }

    fun getDeviceTypeString(deviceInfo: AudioDeviceInfo): String {
        return when (deviceInfo.type) {
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "BT A2DP"
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "BT SCO"
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "Earpiece"
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Speaker"
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired HP"
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired HS"
            AudioDeviceInfo.TYPE_USB_DEVICE -> "USB Device"
            AudioDeviceInfo.TYPE_USB_HEADSET -> "USB Headset"
            AudioDeviceInfo.TYPE_HDMI -> "HDMI"
            else -> "Other (${deviceInfo.type})"
        }
    }
}