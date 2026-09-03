package com.gumlapolytechnic.gpconnect.ui.login

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

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val emailError: Boolean = false,
    val passwordError: Boolean = false,
    val error: LoginResult? = null,
    /** True after LoginResult.EmailNotVerified — shows the verify/resend UI. */
    val isEmailUnverified: Boolean = false,
    /** True while the verification email resend is in flight. */
    val isResending: Boolean = false,
    /** Null until a resend attempt ends; cleared on the next action. */
    val resendResult: Boolean? = null,
)

/**
 * Student sign-in form state machine: blank-field validation here, Firebase
 * authentication + role resolution delegated to the repository. On success
 * the root session state flips and this ViewModel is replaced — it never
 * navigates.
 */
class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update {
            it.copy(
                email = value,
                emailError = false,
                error = null,
                isEmailUnverified = false,
                resendResult = null,
            )
        }
    }

    fun onPasswordChange(value: String) {
        _uiState.update {
            it.copy(
                password = value,
                passwordError = false,
                error = null,
                isEmailUnverified = false,
                resendResult = null,
            )
        }
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
                expectation = LoginExpectation.MEMBER,
            )
            _uiState.update {
                when (result) {
                    LoginResult.Success -> it.copy(isLoading = false)
                    // The unverified state is a distinct, actionable UI: the
                    // standard error line is suppressed in favour of the verify
                    // prompt + resend action below the form.
                    LoginResult.EmailNotVerified -> it.copy(
                        isLoading = false,
                        error = null,
                        isEmailUnverified = true,
                        resendResult = null,
                    )
                    else -> it.copy(isLoading = false, error = result)
                }
            }
        }
    }

    /**
     * Re-sends the verification email using the credentials still held in the
     * form (the user just used them for the login attempt). The repository
     * signs in only to send and signs straight back out.
     */
    fun resendVerificationEmail() {
        val current = _uiState.value
        if (current.isResending) return
        _uiState.update { it.copy(isResending = true, resendResult = null) }
        viewModelScope.launch {
            val result = authRepository.resendVerificationEmail(
                email = current.email,
                password = current.password,
            )
            _uiState.update {
                it.copy(isResending = false, resendResult = result.isSuccess)
            }
        }
    }
}
