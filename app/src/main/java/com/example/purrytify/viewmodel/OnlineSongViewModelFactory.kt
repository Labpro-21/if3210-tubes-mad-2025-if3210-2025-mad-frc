package com.example.purrytify.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.purrytify.network.ApiService
import com.example.purrytify.utils.SessionManager

class OnlineSongViewModelFactory(
    private val api: ApiService,
    private val session: SessionManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OnlineSongViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OnlineSongViewModel(api, session) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}