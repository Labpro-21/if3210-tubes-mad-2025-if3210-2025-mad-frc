package com.example.purrytify.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.purrytify.repository.SongRepository
import com.example.purrytify.utils.SessionManager

class SongViewModelFactory(private val repository: SongRepository, private val userId:Int) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SongViewModel::class.java)) {

            return SongViewModel(repository, userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
