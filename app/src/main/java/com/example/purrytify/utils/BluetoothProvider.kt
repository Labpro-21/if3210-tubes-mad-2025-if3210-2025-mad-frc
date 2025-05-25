package com.example.purrytify.utils

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.flow.MutableStateFlow

class BluetoothProvider(
    private val context: Context,
    private val bluetoothManager: BluetoothManager
) {
    val isHeadsetConnected = MutableStateFlow(getHeadsetState())
    init {

        bluetoothManager.adapter.getProfileProxy(
            context,
            object : BluetoothProfile.ServiceListener {

                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {

                    if (profile == BluetoothProfile.HEADSET)

                        isHeadsetConnected.value = true
                }


                override fun onServiceDisconnected(profile: Int) {
                    if (profile == BluetoothProfile.HEADSET)
                        isHeadsetConnected.value = false
                }

            }

            ,
            BluetoothProfile.HEADSET)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun getHeadsetState(): Boolean {
        val adapter = bluetoothManager.adapter

        return adapter?.getProfileConnectionState(BluetoothProfile.HEADSET) == BluetoothAdapter.STATE_CONNECTED
    }
    }