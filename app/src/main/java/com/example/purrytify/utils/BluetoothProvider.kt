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
        // Receive the adapter from BluetoothManager and install our ServiceListener
        bluetoothManager.adapter.getProfileProxy(
            context,
            object : BluetoothProfile.ServiceListener {
                // This method will be used when the new device connects
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                    // Checking if it is the headset that's active
                    if (profile == BluetoothProfile.HEADSET)
                    // Update state
                        isHeadsetConnected.value = true
                }

                // This method will be used when the new device disconnects
                override fun onServiceDisconnected(profile: Int) {
                    if (profile == BluetoothProfile.HEADSET)
                        isHeadsetConnected.value = false
                }

            }
            // Enabling ServiceListener for headsets
            ,
            BluetoothProfile.HEADSET)
    }
    // The method of receiving the current state of the bluetooth headset. Only used to initialize the starting state
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun getHeadsetState(): Boolean {
        val adapter = bluetoothManager.adapter
        // Checking if there are active headsets
        return adapter?.getProfileConnectionState(BluetoothProfile.HEADSET) == BluetoothAdapter.STATE_CONNECTED
    }
    }