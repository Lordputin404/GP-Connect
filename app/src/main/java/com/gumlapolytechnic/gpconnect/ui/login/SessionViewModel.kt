package com.gumlapolytechnic.gpconnect.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gumlapolytechnic.gpconnect.data.model.User
import com.gumlapolytechnic.gpconnect.data.model.UserRole
import com.gumlapolytechnic.gpconnect.data.repository.AuthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The three possible root sessions. AdminActive carries the resolved role on
 * its [User]; the student shell can never appear while an admin session is
 * active, and vice versa.
 */
sealed interface SessionState {
    data object LoggedOut : SessionState
    data class StudentActive(val user: User) : SessionState
    data class AdminActive(val user: User) : SessionState {
        val role: UserRole get() = user.role
    }
}

/**
 * Root session state: derives the single role-resolved Firebase session into
 * the shell decision consumed by the navigation root.
 */
class SessionViewModel(private val authRepository: AuthRepository) : ViewModel() {

    val sessionState: StateFlow<SessionState> = authRepository.session
        .map { user ->
            when {
                user == null -> SessionState.LoggedOut
                user.role == UserRole.STUDENT -> SessionState.StudentActive(user)
                else -> SessionState.AdminActive(user)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SessionState.LoggedOut,
        )

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}
