package com.example.purrytify.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class AudioOutputViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AudioOutputViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AudioOutputViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}