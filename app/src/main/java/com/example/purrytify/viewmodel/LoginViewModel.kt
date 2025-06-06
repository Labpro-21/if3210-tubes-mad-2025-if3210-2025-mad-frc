package com.example.purrytify.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.model.LoginUiState
import com.example.purrytify.model.LoginResponse
import com.example.purrytify.repository.LoginRepository
import com.example.purrytify.repository.UserRepository
import com.example.purrytify.utils.SessionManager
import kotlinx.coroutines.launch

class LoginViewModel(
    application: Application,
    private val repository: LoginRepository = LoginRepository(),
) : AndroidViewModel(application) {

    private val _uiState = mutableStateOf(LoginUiState())
    val uiState: State<LoginUiState> get() = _uiState

    var isLoading = mutableStateOf(false)
        private set

    var loginResult = mutableStateOf<Result<LoginResponse>?>(null)
        private set

    private val sessionManager = SessionManager(application)

    fun updateEmail(email: String) {
        _uiState.value = _uiState.value.copy(email = email)
    }

    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }

    fun login() {
        viewModelScope.launch {
            isLoading.value = true
            val result = repository.login(_uiState.value.email, _uiState.value.password)
            loginResult.value = result
            isLoading.value = false
        }
    }

    fun clearLoginResult() {
        loginResult.value = null
    }
}