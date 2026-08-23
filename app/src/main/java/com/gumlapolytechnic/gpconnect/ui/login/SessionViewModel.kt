package com.gumlapolytechnic.gpconnect.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gumlapolytechnic.gpconnect.data.model.User
import com.gumlapolytechnic.gpconnect.data.repository.AuthRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Root session state: forwards the repository's auth state to the navigation
 * root (login vs student app) and handles logout.
 */
class SessionViewModel(private val authRepository: AuthRepository) : ViewModel() {

    val authState: StateFlow<User?> = authRepository.authState

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}
