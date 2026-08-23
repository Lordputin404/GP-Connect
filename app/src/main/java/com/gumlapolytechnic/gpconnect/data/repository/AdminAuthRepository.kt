package com.gumlapolytechnic.gpconnect.data.repository

import com.gumlapolytechnic.gpconnect.data.model.User
import kotlinx.coroutines.flow.StateFlow

/**
 * Administrator authentication contract, deliberately separate from student
 * authentication: the two sessions are never interchangeable and root routing
 * distinguishes them. Mock implementation backs the prototype; Firebase
 * replaces it in Phase 4. This is demo authentication, not production
 * security.
 */
interface AdminAuthRepository {
    /** The currently signed-in administrator, or null when signed out. */
    val adminAuthState: StateFlow<User?>

    suspend fun login(username: String, password: String): LoginResult

    suspend fun logout()
}
