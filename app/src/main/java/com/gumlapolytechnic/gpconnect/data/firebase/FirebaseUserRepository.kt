package com.gumlapolytechnic.gpconnect.data.firebase

import com.gumlapolytechnic.gpconnect.data.model.AdminModule
import com.gumlapolytechnic.gpconnect.data.model.User
import com.gumlapolytechnic.gpconnect.data.model.UserRole
import com.gumlapolytechnic.gpconnect.data.repository.UserRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Firestore-backed administrator/user profile management. Only SUPER_ADMIN
 * may observe or mutate users — enforced by the security rules; the client
 * additionally never lets a super admin modify their own account.
 */
class FirebaseUserRepository : UserRepository {

    private val firestore get() = FirebaseServices.firestore

    override fun observeUsers(): Flow<List<User>> = callbackFlow {
        val registration: ListenerRegistration =
            firestore.collection(USERS).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val users = snapshot?.documents
                    ?.mapNotNull { it.toUser() }
                    .orEmpty()
                    .sortedBy { it.name.lowercase() }
                trySend(users)
            }
        awaitClose { registration.remove() }
    }

    override suspend fun setEnabled(uid: String, enabled: Boolean) {
        firestore.collection(USERS).document(uid)
            .update("enabled", enabled, "updatedAt", System.currentTimeMillis())
            .awaitTask()
    }

    override suspend fun setRole(uid: String, role: UserRole, module: AdminModule?) {
        firestore.collection(USERS).document(uid)
            .update(
                "role", role.name,
                "module", module?.name,
                "updatedAt", System.currentTimeMillis(),
            )
            .awaitTask()
    }

    private companion object {
        const val USERS = "users"
    }
}
