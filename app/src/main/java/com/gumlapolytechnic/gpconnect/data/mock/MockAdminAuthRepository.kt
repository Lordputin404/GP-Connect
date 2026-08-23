package com.gumlapolytechnic.gpconnect.data.mock

import com.gumlapolytechnic.gpconnect.data.model.User
import com.gumlapolytechnic.gpconnect.data.model.UserRole
import com.gumlapolytechnic.gpconnect.data.repository.AdminAuthRepository
import com.gumlapolytechnic.gpconnect.data.repository.LoginResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Mock administrator authentication. Demo credentials live only in these
 * constants (isolated mock data — deliberately NOT shown in the UI like the
 * student demo hint). A Firebase-backed implementation replaces this in
 * Phase 4.
 */
class MockAdminAuthRepository : AdminAuthRepository {

    private val currentAdmin = MutableStateFlow<User?>(null)
    override val adminAuthState: StateFlow<User?> = currentAdmin.asStateFlow()

    override suspend fun login(username: String, password: String): LoginResult {
        delay(NETWORK_DELAY_MS)
        val accepted = username.trim().lowercase() == DEMO_USERNAME && password == DEMO_PASSWORD
        return if (accepted) {
            currentAdmin.value = DemoAdmin
            LoginResult.Success
        } else {
            LoginResult.InvalidCredentials
        }
    }

    override suspend fun logout() {
        currentAdmin.value = null
    }

    private companion object {
        const val NETWORK_DELAY_MS = 600L
        const val DEMO_USERNAME = "admin"
        const val DEMO_PASSWORD = "admin1234"
    }
}
