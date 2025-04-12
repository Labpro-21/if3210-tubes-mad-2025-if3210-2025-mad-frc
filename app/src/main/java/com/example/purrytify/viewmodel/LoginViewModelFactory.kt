package com.example.purrytify.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.purrytify.repository.LoginRepository
import com.example.purrytify.repository.UserRepository

class LoginViewModelFactory(
    private val application: Application,
    private val loginRepository: LoginRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(application, loginRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}