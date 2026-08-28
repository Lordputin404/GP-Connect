package com.gumlapolytechnic.gpconnect.data.firebase

import android.util.Log
import com.gumlapolytechnic.gpconnect.data.model.SignupSubmission
import com.gumlapolytechnic.gpconnect.data.model.User
import com.gumlapolytechnic.gpconnect.data.model.isAdmin
import com.gumlapolytechnic.gpconnect.data.repository.AuthRepository
import com.gumlapolytechnic.gpconnect.data.repository.LoginExpectation
import com.gumlapolytechnic.gpconnect.data.repository.LoginResult
import com.gumlapolytechnic.gpconnect.data.repository.RegistrationResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
 *
 * Every failure path logs the real exception class, Firebase error code and
 * message under the GPFirebaseAuth tag so device-side root causes are visible
 * in Logcat — failures are never collapsed into a generic error.
 */
class FirebaseAuthRepository : AuthRepository {

    private val auth: FirebaseAuth get() = FirebaseServices.auth
    private val firestore: FirebaseFirestore get() = FirebaseServices.firestore

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _session = MutableStateFlow<User?>(null)
    override val session: StateFlow<User?> = _session.asStateFlow()

    private val _isResolvingSession = MutableStateFlow(true)
    override val isResolvingSession: StateFlow<Boolean> = _isResolvingSession.asStateFlow()

    /**
     * Set for the duration of [register] only. Self-service signup necessarily
     * creates an Auth account *before* users/{uid} exists, which fires the auth
     * state listener below; without this guard the restore path would find no
     * profile, sign the applicant out mid-write, and the profile/request batch
     * would then fail with permission denied. Volatile because the listener runs
     * on the main thread while [register] runs on Dispatchers.IO.
     */
    @Volatile
    private var registrationInProgress = false

    init {
        // Restores a persisted Firebase session at app start (and observes
        // sign-out). Profile problems end the session rather than guessing a
        // role. Every resolution path clears isResolvingSession so the root
        // never waits on a stale checking state.
        auth.addAuthStateListener { firebaseAuth ->
            if (registrationInProgress) {
                // Deliberately touches neither flow: the applicant stays on the
                // signup screen (session == null, resolving == false) and is
                // signed out again as soon as the signup writes complete.
                Log.i(TAG, "Auth state change ignored — signup in progress")
                return@addAuthStateListener
            }
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser == null) {
                _isResolvingSession.value = false
                _session.value = null
            } else {
                scope.launch {
                    val profile = try {
                        fetchProfile(firebaseUser.uid)
                    } catch (e: Exception) {
                        Log.e(
                            TAG,
                            "Session restore: profile fetch failed for uid=${firebaseUser.uid}",
                            e,
                        )
                        null
                    }
                    when {
                        profile == null -> {
                            Log.w(TAG, "Session restore: no usable profile — signing out")
                            auth.signOut()
                        }
                        !profile.enabled -> {
                            Log.w(TAG, "Session restore: profile disabled — signing out")
                            auth.signOut()
                        }
                        else -> {
                            Log.i(TAG, "Session restored: uid=${profile.id} role=${profile.role}")
                            _session.value = profile
                        }
                    }
                    _isResolvingSession.value = false
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
            Log.e(
                TAG,
                "signInWithEmailAndPassword failed: code=${e.errorCode} " +
                    "exception=${e.javaClass.simpleName} message=${e.message}",
                e,
            )
            return mapAuthError(e)
        } catch (e: Exception) {
            Log.e(
                TAG,
                "signInWithEmailAndPassword failed (non-Auth exception): " +
                    "${e.javaClass.simpleName}: ${e.message}",
                e,
            )
            return mapUnknownError(e)
        }
        val firebaseUser = result.user
        if (firebaseUser == null) {
            Log.e(TAG, "signInWithEmailAndPassword returned no user")
            return LoginResult.InvalidCredentials
        }
        Log.i(TAG, "Firebase authentication succeeded: uid=${firebaseUser.uid}")

        val profile = try {
            fetchProfile(firebaseUser.uid)
        } catch (e: FirebaseFirestoreException) {
            Log.e(
                TAG,
                "Profile fetch failed (Firestore): code=${e.code} " +
                    "exception=${e.javaClass.simpleName} message=${e.message}",
                e,
            )
            auth.signOut()
            return mapFirestoreError(e)
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Profile fetch failed: ${e.javaClass.simpleName}: ${e.message}",
                e,
            )
            auth.signOut()
            return mapUnknownError(e)
        }

        if (profile == null) {
            Log.w(TAG, "Profile users/${firebaseUser.uid} does not exist — not configured")
            auth.signOut()
            return LoginResult.AccountNotConfigured
        }

        return when {
            !profile.enabled -> {
                Log.w(TAG, "Profile disabled for uid=${profile.id}")
                auth.signOut()
                LoginResult.AccountDisabled
            }
            expectation == LoginExpectation.MEMBER && profile.role.isAdmin -> {
                Log.w(TAG, "Role mismatch: expected a student/teacher account, got ${profile.role}")
                auth.signOut()
                LoginResult.WrongRole
            }
            expectation == LoginExpectation.ADMIN && !profile.role.isAdmin -> {
                Log.w(TAG, "Role mismatch: expected an admin role, got ${profile.role}")
                auth.signOut()
                LoginResult.WrongRole
            }
            else -> {
                Log.i(TAG, "Login resolved: uid=${profile.id} role=${profile.role} module=${profile.module}")
                _session.value = profile
                LoginResult.Success
            }
        }
    }

    override suspend fun register(
        submission: SignupSubmission,
        password: String,
    ): RegistrationResult {
        val typedEmail = submission.email.trim()
        registrationInProgress = true
        var createdUid: String? = null
        try {
            val created = try {
                auth.createUserWithEmailAndPassword(typedEmail, password).awaitTask()
            } catch (e: FirebaseAuthException) {
                Log.e(
                    TAG,
                    "createUserWithEmailAndPassword failed: code=${e.errorCode} " +
                        "exception=${e.javaClass.simpleName} message=${e.message}",
                    e,
                )
                return mapRegistrationAuthError(e)
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "createUserWithEmailAndPassword failed (non-Auth exception): " +
                        "${e.javaClass.simpleName}: ${e.message}",
                    e,
                )
                return RegistrationResult.NetworkError
            }
            val firebaseUser = created.user
            if (firebaseUser == null) {
                Log.e(TAG, "createUserWithEmailAndPassword returned no user")
                return RegistrationResult.UnknownFailure
            }
            val uid = firebaseUser.uid
            createdUid = uid
            // Use the email Firebase itself recorded: security rules compare the
            // stored email against request.auth.token.email exactly.
            val stored = submission.copy(email = firebaseUser.email ?: typedEmail)
            val now = System.currentTimeMillis()

            try {
                val batch = firestore.batch()
                batch.set(
                    firestore.collection(USERS).document(uid),
                    pendingMemberProfileFields(stored, now),
                )
                batch.set(
                    firestore.collection(SIGNUP_REQUESTS).document(uid),
                    signupRequestFields(uid, stored, now),
                )
                batch.commit().awaitTask()
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "Signup write failed for uid=$uid (${e.javaClass.simpleName}: ${e.message}) " +
                        "— rolling back the Auth account",
                    e,
                )
                // Without this the applicant would own an Auth account that has
                // no profile and can never be signed in or retried.
                runCatching { firebaseUser.delete().awaitTask() }.onFailure { failure ->
                    Log.e(TAG, "Rollback failed — orphan Auth account uid=$uid has no profile", failure)
                }
                return when {
                    e is FirebaseFirestoreException &&
                        e.code == FirebaseFirestoreException.Code.UNAVAILABLE -> RegistrationResult.NetworkError
                    e is FirebaseFirestoreException &&
                        e.code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> RegistrationResult.NetworkError
                    e is FirebaseFirestoreException -> RegistrationResult.RequestRejected
                    else -> RegistrationResult.NetworkError
                }
            }

            Log.i(
                TAG,
                "Signup request created: uid=$uid department=${stored.department.id} " +
                    "requestedRole=${stored.requestedRole}",
            )
            return RegistrationResult.Success
        } finally {
            registrationInProgress = false
            // A pending account must never hold a live session. Only sign out
            // when this call actually created an account, so a failed attempt
            // cannot terminate an unrelated session.
            if (createdUid != null) {
                auth.signOut()
            }
        }
    }

    override suspend fun logout() {
        auth.signOut()
        _session.value = null
    }

    private suspend fun fetchProfile(uid: String): User? {
        val snapshot = firestore.collection(USERS).document(uid).get().awaitTask()
        if (snapshot == null) {
            Log.w(TAG, "Profile snapshot null for uid=$uid")
            return null
        }
        if (!snapshot.exists()) {
            return null
        }
        val user = snapshot.toUser()
        if (user == null) {
            Log.e(TAG, "Profile conversion returned null for uid=$uid (malformed document?)")
        }
        return user
    }

    private fun mapAuthError(e: FirebaseAuthException): LoginResult = when (e.errorCode) {
        "ERROR_NETWORK_REQUEST_FAILED" -> LoginResult.NetworkError
        "ERROR_TOO_MANY_REQUESTS", "ERROR_TOO_MANY_ATTEMPTS_TRY_LATER" -> LoginResult.RateLimited
        "ERROR_USER_DISABLED" -> LoginResult.AccountDisabled
        "ERROR_OPERATION_NOT_ALLOWED", "ERROR_INVALID_CONFIGURATION",
        "ERROR_PROJECT_NOT_FOUND", "ERROR_API_UNAVAILABLE",
        -> LoginResult.ProviderMisconfigured
        else -> LoginResult.InvalidCredentials
    }

    private fun mapRegistrationAuthError(e: FirebaseAuthException): RegistrationResult =
        when (e.errorCode) {
            "ERROR_EMAIL_ALREADY_IN_USE",
            "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL",
            -> RegistrationResult.EmailAlreadyInUse
            "ERROR_INVALID_EMAIL" -> RegistrationResult.InvalidEmail
            "ERROR_WEAK_PASSWORD" -> RegistrationResult.WeakPassword
            "ERROR_NETWORK_REQUEST_FAILED" -> RegistrationResult.NetworkError
            "ERROR_TOO_MANY_REQUESTS", "ERROR_TOO_MANY_ATTEMPTS_TRY_LATER" -> RegistrationResult.RateLimited
            "ERROR_OPERATION_NOT_ALLOWED", "ERROR_INVALID_CONFIGURATION",
            "ERROR_PROJECT_NOT_FOUND", "ERROR_API_UNAVAILABLE",
            -> RegistrationResult.ProviderMisconfigured
            else -> RegistrationResult.UnknownFailure
        }

    private fun mapFirestoreError(e: FirebaseFirestoreException): LoginResult = when (e.code) {
        FirebaseFirestoreException.Code.UNAVAILABLE,
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
        -> LoginResult.NetworkError
        // PERMISSION_DENIED and every other Firestore failure is a profile
        // access problem (missing/wrong rules), not a connectivity problem.
        else -> LoginResult.ProfileAccessDenied
    }

    /** Non-Auth, non-Firestore failures: connectivity or unknown transport problems. */
    private fun mapUnknownError(e: Exception): LoginResult = LoginResult.NetworkError

    private companion object {
        const val TAG = "GPFirebaseAuth"
        const val USERS = "users"
        const val SIGNUP_REQUESTS = "signupRequests"
    }
}
