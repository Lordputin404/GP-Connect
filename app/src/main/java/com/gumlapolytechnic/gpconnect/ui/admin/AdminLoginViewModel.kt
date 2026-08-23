package com.gumlapolytechnic.gpconnect.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gumlapolytechnic.gpconnect.data.repository.AuthRepository
import com.gumlapolytechnic.gpconnect.data.repository.LoginExpectation
import com.gumlapolytechnic.gpconnect.data.repository.LoginResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminLoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val emailError: Boolean = false,
    val passwordError: Boolean = false,
    val error: LoginResult? = null,
)

/**
 * Admin sign-in form state machine. On success the root session state flips
 * to AdminActive (with the resolved role) and the AdminApp replaces this
 * graph — this ViewModel does not navigate.
 */
class AdminLoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminLoginUiState())
    val uiState: StateFlow<AdminLoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = false, error = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, passwordError = false, error = null) }
    }

    fun login() {
        val current = _uiState.value
        val emailBlank = current.email.isBlank()
        val passwordBlank = current.password.isBlank()
        if (emailBlank || passwordBlank) {
            _uiState.update {
                it.copy(emailError = emailBlank, passwordError = passwordBlank, error = null)
            }
            return
        }

        _uiState.update {
            it.copy(isLoading = true, emailError = false, passwordError = false, error = null)
        }
        viewModelScope.launch {
            val result = authRepository.login(
                email = current.email,
                password = current.password,
                expectation = LoginExpectation.ADMIN,
            )
            _uiState.update {
                when (result) {
                    LoginResult.Success -> it.copy(isLoading = false)
                    else -> it.copy(isLoading = false, error = result)
                }
            }
        }
    }
}
