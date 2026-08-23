package com.gumlapolytechnic.gpconnect.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gumlapolytechnic.gpconnect.data.repository.AdminAuthRepository
import com.gumlapolytechnic.gpconnect.data.repository.LoginResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminLoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val usernameError: Boolean = false,
    val passwordError: Boolean = false,
    val credentialsError: Boolean = false,
)

/**
 * Admin sign-in form state machine. On success the root session state flips
 * to AdminActive and the AdminApp replaces this graph — this ViewModel does
 * not navigate. Demo credentials are intentionally not surfaced in the UI.
 */
class AdminLoginViewModel(private val adminAuthRepository: AdminAuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminLoginUiState())
    val uiState: StateFlow<AdminLoginUiState> = _uiState.asStateFlow()

    fun onUsernameChange(value: String) {
        _uiState.update {
            it.copy(username = value, usernameError = false, credentialsError = false)
        }
    }

    fun onPasswordChange(value: String) {
        _uiState.update {
            it.copy(password = value, passwordError = false, credentialsError = false)
        }
    }

    fun login() {
        val current = _uiState.value
        val usernameBlank = current.username.isBlank()
        val passwordBlank = current.password.isBlank()
        if (usernameBlank || passwordBlank) {
            _uiState.update {
                it.copy(
                    usernameError = usernameBlank,
                    passwordError = passwordBlank,
                    credentialsError = false,
                )
            }
            return
        }

        _uiState.update {
            it.copy(isLoading = true, usernameError = false, passwordError = false, credentialsError = false)
        }
        viewModelScope.launch {
            val result = adminAuthRepository.login(current.username, current.password)
            _uiState.update {
                when (result) {
                    LoginResult.Success -> it.copy(isLoading = false)
                    LoginResult.InvalidCredentials -> it.copy(isLoading = false, credentialsError = true)
                }
            }
        }
    }
}
