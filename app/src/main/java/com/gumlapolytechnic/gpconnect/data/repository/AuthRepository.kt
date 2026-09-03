package com.gumlapolytechnic.gpconnect.data.repository

import com.gumlapolytechnic.gpconnect.data.model.SignupSubmission
import com.gumlapolytechnic.gpconnect.data.model.User
import kotlinx.coroutines.flow.StateFlow

/**
 * Which kind of account the current login form expects. MEMBER covers both
 * STUDENT and TEACHER — they share the same non-administrative shell.
 */
enum class LoginExpectation { MEMBER, ADMIN }

/** Outcome of a login attempt after authentication, profile and role resolution. */
sealed interface LoginResult {
    data object Success : LoginResult
    data object InvalidCredentials : LoginResult
    /** Authenticated, but no users/{uid} profile exists — account not set up. */
    data object AccountNotConfigured : LoginResult
    /** Profile exists but enabled == false (or the Auth account is disabled). */
    data object AccountDisabled : LoginResult
    /** Authenticated, but the Firebase email has not been verified yet. */
    data object EmailNotVerified : LoginResult
    /** Not enabled, but the signup request is still PENDING — awaiting the department HOD. */
    data object AccountPendingApproval : LoginResult
    /** Not enabled HOD applicant — awaiting the college Super Admin's approval. */
    data object HodAccountPendingApproval : LoginResult
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

/** Outcome of a self-service signup attempt. */
sealed interface RegistrationResult {
    /** Auth account created, profile + signup request written, session signed out. */
    data object Success : RegistrationResult
    data object EmailAlreadyInUse : RegistrationResult
    data object InvalidEmail : RegistrationResult
    data object WeakPassword : RegistrationResult
    data object NetworkError : RegistrationResult
    data object RateLimited : RegistrationResult
    data object ProviderMisconfigured : RegistrationResult
    /** Firestore refused the profile/request write (rules not deployed or wrong). */
    data object RequestRejected : RegistrationResult
    data object UnknownFailure : RegistrationResult
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

    /**
     * Re-sends the Firebase verification email to [email]. Firebase only sends
     * to the signed-in user, so this temporarily signs in with [password]
     * (reusing the credentials of the login attempt that failed with
     * [LoginResult.EmailNotVerified]) and always signs out again — an
     * unverified account never holds a live session. Verification alone grants
     * nothing: the signup-request approval + enabled profile checks still
     * apply at login.
     */
    suspend fun resendVerificationEmail(email: String, password: String): Result<Unit>

    /**
     * Self-service signup. Creates the Firebase Auth account (the password goes
     * only to Firebase Auth — never to Firestore), then atomically writes a
     * **disabled STUDENT** profile at `users/{uid}` plus a PENDING request at
     * `signupRequests/{uid}` for the department HOD to decide on.
     *
     * The applicant is signed out again before this returns: a pending account
     * must never hold a live session. If the Firestore write fails the freshly
     * created Auth account is deleted so the applicant can retry with the same
     * email.
     */
    suspend fun register(submission: SignupSubmission, password: String): RegistrationResult

    suspend fun logout()
}
