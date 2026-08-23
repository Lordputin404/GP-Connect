package com.gumlapolytechnic.gpconnect.data.mock

import com.gumlapolytechnic.gpconnect.data.model.User
import com.gumlapolytechnic.gpconnect.data.repository.AuthRepository
import com.gumlapolytechnic.gpconnect.data.repository.LoginResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Mock authentication: accepts only the fictional demo student. The short
 * artificial delay keeps loading states realistic for the prototype; the
 * Firebase-backed implementation replaces this class in Phase 4.
 */
class MockAuthRepository : AuthRepository {

    private val currentUser = MutableStateFlow<User?>(null)
    override val authState: StateFlow<User?> = currentUser.asStateFlow()

    override suspend fun login(username: String, password: String): LoginResult {
        delay(NETWORK_DELAY_MS)
        val accepted = username.trim().lowercase() in DEMO_USERNAMES && password == DEMO_PASSWORD
        return if (accepted) {
            currentUser.value = DemoStudent
            LoginResult.Success
        } else {
            LoginResult.InvalidCredentials
        }
    }

    override suspend fun logout() {
        currentUser.value = null
    }

    private companion object {
        const val NETWORK_DELAY_MS = 600L
        const val DEMO_PASSWORD = "demo1234"
        val DEMO_USERNAMES = setOf("amar", "amar@gpconnect.demo")
    }
}
