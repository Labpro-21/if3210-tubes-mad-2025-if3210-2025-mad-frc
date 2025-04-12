package com.example.purrytify.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.example.purrytify.utils.NetworkManager

class NetworkViewModel(application: Application) : AndroidViewModel(application) {

    private val networkManager = NetworkManager(application)

    val isConnected: LiveData<Boolean> = networkManager.isConnected.also {
        it.observeForever { value ->
            Log.d("NetworkViewModel", "isConnected updated: $value")
        }
    }
}