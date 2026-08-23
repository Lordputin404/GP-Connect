package com.gumlapolytechnic.gpconnect.data.repository

import com.gumlapolytechnic.gpconnect.data.model.AdminModule
import com.gumlapolytechnic.gpconnect.data.model.User
import com.gumlapolytechnic.gpconnect.data.model.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Administrator account management. Only SUPER_ADMIN may use these operations
 * (enforced by Firestore security rules, not just hidden UI).
 *
 * BACKEND LIMITATION (documented, not faked): creating a *Firebase
 * Authentication* account for another person cannot be done safely from the
 * signed-in client — createUserWithEmailAndPassword would replace the current
 * session. Real account creation requires the Firebase Admin SDK / a trusted
 * backend (later phase). Until then, new administrator Auth accounts are
 * created manually in the Firebase Console and this repository manages their
 * Firestore profile (role, module, enabled).
 */
interface UserRepository {
    /** All application users; restricted to SUPER_ADMIN by security rules. */
    fun observeUsers(): Flow<List<User>>

    suspend fun setEnabled(uid: String, enabled: Boolean)

    suspend fun setRole(uid: String, role: UserRole, module: AdminModule?)
}
