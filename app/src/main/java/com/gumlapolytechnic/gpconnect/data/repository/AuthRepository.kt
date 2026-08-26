package com.gumlapolytechnic.gpconnect.data.repository

import com.gumlapolytechnic.gpconnect.data.model.User
import kotlinx.coroutines.flow.StateFlow

/** Which kind of account the current login form expects. */
enum class LoginExpectation { STUDENT, ADMIN }

/** Outcome of a login attempt after authentication, profile and role resolution. */
sealed interface LoginResult {
    data object Success : LoginResult
    data object InvalidCredentials : LoginResult
    /** Authenticated, but no users/{uid} profile exists — account not set up. */
    data object AccountNotConfigured : LoginResult
    /** Profile exists but enabled == false (or the Auth account is disabled). */
    data object AccountDisabled : LoginResult
    /** Account exists but its role does not match the login form used. */
    data object WrongRole : LoginResult
    /** Network/Firebase unreachable or unknown failure. */
    data object NetworkError : LoginResult
    /** Too many attempts — Firebase rate limiting. */
    data object RateLimited : LoginResult
    /** The Auth provider itself is misconfigured (e.g. Email/Password not enabled). */
    data object ProviderMisconfigured : LoginResult
    /** The Firestore profile read failed (e.g. permission denied — rules missing/wrong). */
    data object ProfileAccessDenied : LoginResult
}

/**
 * Unified authentication contract (student and admin login share one Firebase
 * session; only the expected role differs). Firebase Authentication is the
 * identity source of truth; role/enabled/module come from the Firestore
 * profile. There is deliberately no mock fallback in the production flow.
 */
interface AuthRepository {
    /**
     * The role-resolved, enabled-checked signed-in user, or null when signed
     * out. Restores automatically from a persisted Firebase session.
     */
    val session: StateFlow<User?>

    /**
     * True while startup session restoration is still deciding whether a
     * persisted Firebase user exists and resolves. The root must show a
     * checking state (not the login screen) while this is true, so a stored
     * session never flashes LoginScreen before Home/Admin appears.
     */
    val isResolvingSession: StateFlow<Boolean>

    suspend fun login(email: String, password: String, expectation: LoginExpectation): LoginResult

    suspend fun logout()
}
