package com.gumlapolytechnic.gpconnect.data.repository

import com.gumlapolytechnic.gpconnect.data.model.User
import kotlinx.coroutines.flow.StateFlow

/** Outcome of a login attempt. */
sealed interface LoginResult {
    data object Success : LoginResult
    data object InvalidCredentials : LoginResult
}

/**
 * Authentication contract. The UI depends only on this interface; the mock
 * implementation backs the prototype and a Firebase implementation replaces it
 * in Phase 4 without UI changes.
 */
interface AuthRepository {
    /** The currently signed-in user, or null when signed out. */
    val authState: StateFlow<User?>

    suspend fun login(username: String, password: String): LoginResult

    suspend fun logout()
}
