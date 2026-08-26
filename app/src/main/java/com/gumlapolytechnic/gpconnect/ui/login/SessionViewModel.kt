package com.gumlapolytechnic.gpconnect.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gumlapolytechnic.gpconnect.data.model.User
import com.gumlapolytechnic.gpconnect.data.model.UserRole
import com.gumlapolytechnic.gpconnect.data.repository.AuthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The possible root sessions. [Checking] is the startup state while a
 * persisted Firebase session is being restored — the login screen must never
 * appear during it, so a stored session never flashes LoginScreen.
 * AdminActive carries the resolved role on its [User]; the student shell can
 * never appear while an admin session is active, and vice versa.
 */
sealed interface SessionState {
    data object Checking : SessionState
    data object LoggedOut : SessionState
    data class StudentActive(val user: User) : SessionState
    data class AdminActive(val user: User) : SessionState {
        val role: UserRole get() = user.role
    }
}

/**
 * Root session state: derives the role-resolved Firebase session plus its
 * restoration progress into the shell decision consumed by the navigation
 * root. Logout flips straight to LoggedOut — restoration is already complete
 * by then, so no checking state delays the login screen.
 */
class SessionViewModel(private val authRepository: AuthRepository) : ViewModel() {

    val sessionState: StateFlow<SessionState> = combine(
        authRepository.session,
        authRepository.isResolvingSession,
    ) { user, resolving ->
        when {
            resolving -> SessionState.Checking
            user == null -> SessionState.LoggedOut
            user.role == UserRole.STUDENT -> SessionState.StudentActive(user)
            else -> SessionState.AdminActive(user)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SessionState.Checking,
    )

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}
