package com.gumlapolytechnic.gpconnect.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gumlapolytechnic.gpconnect.data.repository.AuthRepository
import com.gumlapolytechnic.gpconnect.data.repository.LoginResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val usernameError: Boolean = false,
    val passwordError: Boolean = false,
    val credentialsError: Boolean = false,
)

/**
 * Login form state machine: blank-field validation happens here, credential
 * verification is delegated to the repository. Successful login is observed
 * through [SessionViewModel] — this ViewModel does not navigate.
 */
class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

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
            val result = authRepository.login(current.username, current.password)
            _uiState.update {
                when (result) {
                    LoginResult.Success -> it.copy(isLoading = false)
                    LoginResult.InvalidCredentials -> it.copy(isLoading = false, credentialsError = true)
                }
            }
        }
    }
}
