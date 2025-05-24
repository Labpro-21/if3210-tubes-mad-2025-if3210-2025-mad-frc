package com.example.purrytify.viewmodel
//
//import android.content.Context
//import android.media.AudioDeviceInfo
//import android.media.AudioManager
//import android.util.Log
//import androidx.core.content.ContextCompat.getSystemService
//
//class SoundRouteViewModel(context: Context) {
//    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
//    val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
//
//    val externalDevices = devices.filter {
//        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
//                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
//                it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
//                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET
//    }
//
//}