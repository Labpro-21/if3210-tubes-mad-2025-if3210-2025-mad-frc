package com.example.purrytify.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.purrytify.repository.UserRepository
import com.example.purrytify.utils.SessionManager
import com.example.purrytify.utils.TokenManager
import android.content.Context

class ProfileViewModelFactory(
    private val context: Context,
    private val tokenManager: TokenManager,
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(context, tokenManager, userRepository, sessionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}