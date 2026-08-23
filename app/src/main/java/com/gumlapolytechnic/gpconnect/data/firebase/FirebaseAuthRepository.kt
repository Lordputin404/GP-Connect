package com.gumlapolytechnic.gpconnect.data.firebase

import com.gumlapolytechnic.gpconnect.data.model.User
import com.gumlapolytechnic.gpconnect.data.model.UserRole
import com.gumlapolytechnic.gpconnect.data.repository.AuthRepository
import com.gumlapolytechnic.gpconnect.data.repository.LoginExpectation
import com.gumlapolytechnic.gpconnect.data.repository.LoginResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Firebase Authentication + Firestore profile resolution.
 *
 * Login flow: authenticate with Email/Password → read users/{uid} → verify the
 * profile exists and is enabled → verify the role matches the login form's
 * expectation → publish the resolved [User] on [session]. Any failure signs
 * the Firebase user out again; there is no mock fallback.
 */
class FirebaseAuthRepository : AuthRepository {

    private val auth: FirebaseAuth get() = FirebaseServices.auth
    private val firestore: FirebaseFirestore get() = FirebaseServices.firestore

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _session = MutableStateFlow<User?>(null)
    override val session: StateFlow<User?> = _session.asStateFlow()

    init {
        // Restores a persisted Firebase session at app start (and observes
        // sign-out). Profile problems end the session rather than guessing a
        // role.
        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser == null) {
                _session.value = null
            } else {
                scope.launch {
                    val profile = runCatching { fetchProfile(firebaseUser.uid) }.getOrNull()
                    if (profile == null || !profile.enabled) {
                        auth.signOut()
                    } else {
                        _session.value = profile
                    }
                }
            }
        }
    }

    override suspend fun login(
        email: String,
        password: String,
        expectation: LoginExpectation,
    ): LoginResult {
        val result = try {
            auth.signInWithEmailAndPassword(email.trim(), password).awaitTask()
        } catch (e: FirebaseAuthException) {
            return mapAuthError(e)
        } catch (e: Exception) {
            return LoginResult.NetworkError
        }
        val firebaseUser = result.user ?: return LoginResult.InvalidCredentials

        val profile = try {
            fetchProfile(firebaseUser.uid)
        } catch (e: Exception) {
            auth.signOut()
            return LoginResult.NetworkError
        }

        return when {
            profile == null -> {
                auth.signOut()
                LoginResult.AccountNotConfigured
            }
            !profile.enabled -> {
                auth.signOut()
                LoginResult.AccountDisabled
            }
            expectation == LoginExpectation.STUDENT && profile.role != UserRole.STUDENT -> {
                auth.signOut()
                LoginResult.WrongRole
            }
            expectation == LoginExpectation.ADMIN && profile.role == UserRole.STUDENT -> {
                auth.signOut()
                LoginResult.WrongRole
            }
            else -> {
                _session.value = profile
                LoginResult.Success
            }
        }
    }

    override suspend fun logout() {
        auth.signOut()
        _session.value = null
    }

    private suspend fun fetchProfile(uid: String): User? =
        firestore.collection(USERS).document(uid).get().awaitTask()?.toUser()

    private fun mapAuthError(e: FirebaseAuthException): LoginResult = when (e.errorCode) {
        "ERROR_NETWORK_REQUEST_FAILED" -> LoginResult.NetworkError
        "ERROR_TOO_MANY_ATTEMPTS_TRY_LATER", "ERROR_OPERATION_NOT_ALLOWED" -> LoginResult.RateLimited
        "ERROR_USER_DISABLED" -> LoginResult.AccountDisabled
        else -> LoginResult.InvalidCredentials
    }

    private companion object {
        const val USERS = "users"
    }
}
