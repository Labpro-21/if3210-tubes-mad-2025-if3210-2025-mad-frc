package com.example.purrytify.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.purrytify.repository.SongRepository
import com.example.purrytify.utils.SessionManager

class SongViewModelFactory(
    private val repository: SongRepository,
    private val userId: Int,
    private val context: Context // Tambahkan context di sini
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SongViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SongViewModel(repository, userId, context.applicationContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

