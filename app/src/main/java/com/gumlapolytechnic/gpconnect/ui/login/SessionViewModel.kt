package com.gumlapolytechnic.gpconnect.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gumlapolytechnic.gpconnect.data.model.User
import com.gumlapolytechnic.gpconnect.data.repository.AdminAuthRepository
import com.gumlapolytechnic.gpconnect.data.repository.AuthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The three possible root sessions. Admin takes priority in the (impossible
 * in the mock, defensive) case of both being active — the student shell can
 * never appear while an administrator session exists.
 */
sealed interface SessionState {
    data object LoggedOut : SessionState
    data class StudentActive(val user: User) : SessionState
    data class AdminActive(val user: User) : SessionState
}

/**
 * Root session state: combines the separate student and admin authentication
 * states into the single decision consumed by the navigation root.
 */
class SessionViewModel(
    private val authRepository: AuthRepository,
    private val adminAuthRepository: AdminAuthRepository,
) : ViewModel() {

    val sessionState: StateFlow<SessionState> = combine(
        authRepository.authState,
        adminAuthRepository.adminAuthState,
    ) { student, admin ->
        when {
            admin != null -> SessionState.AdminActive(admin)
            student != null -> SessionState.StudentActive(student)
            else -> SessionState.LoggedOut
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SessionState.LoggedOut,
    )

    fun logoutStudent() {
        viewModelScope.launch { authRepository.logout() }
    }

    fun logoutAdmin() {
        viewModelScope.launch { adminAuthRepository.logout() }
    }
}
