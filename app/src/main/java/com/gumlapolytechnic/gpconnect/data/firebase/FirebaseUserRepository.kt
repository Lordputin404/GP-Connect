package com.gumlapolytechnic.gpconnect.data.firebase

import android.util.Log
import com.gumlapolytechnic.gpconnect.data.model.Department
import com.gumlapolytechnic.gpconnect.data.model.MEMBER_ROLES
import com.gumlapolytechnic.gpconnect.data.model.User
import com.gumlapolytechnic.gpconnect.data.model.UserRole
import com.gumlapolytechnic.gpconnect.data.model.departmentModule
import com.gumlapolytechnic.gpconnect.data.repository.UserRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Firestore-backed user profile management.
 *
 * Authority split, mirrored by the security rules:
 *  - [observeUsers] and [setAdminRole] require SUPER_ADMIN.
 *  - [observeDepartmentMembers] and [setMemberRole] are limited to the caller's
 *    own department (HOD). The department filter is part of the *query*, which
 *    the `list` rule requires, so it cannot be widened from the client.
 *
 * The client additionally never lets a super admin modify their own account.
 */
class FirebaseUserRepository : UserRepository {

    private val firestore get() = FirebaseServices.firestore

    override fun observeUsers(): Flow<List<User>> = callbackFlow {
        val registration: ListenerRegistration =
            firestore.collection(USERS).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observeUsers failed: code=${error.code} message=${error.message}", error)
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

    override fun observeDepartmentMembers(
        department: Department,
    ): Flow<Result<List<User>>> = callbackFlow {
        val registration: ListenerRegistration = firestore.collection(USERS)
            // Required by the rules, not just an optimisation: the `list` rule
            // matches on resource.data.department, so Firestore rejects any query
            // that does not carry this exact equality constraint.
            .whereEqualTo("department", department.id)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(
                        TAG,
                        "observeDepartmentMembers(${department.id}) failed: " +
                            "code=${error.code} message=${error.message}",
                        error,
                    )
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val members = snapshot?.documents
                    ?.mapNotNull { it.toUser() }
                    .orEmpty()
                    // Administrators are never "members" of a department roster.
                    .filter { it.role in MEMBER_ROLES }
                    .sortedBy { it.name.lowercase() }
                trySend(Result.success(members))
            }
        awaitClose { registration.remove() }
    }

    override suspend fun setEnabled(uid: String, enabled: Boolean) {
        firestore.collection(USERS).document(uid)
            .update("enabled", enabled, "updatedAt", System.currentTimeMillis())
            .awaitTask()
    }

    override suspend fun setAdminRole(uid: String, role: UserRole, department: Department?) {
        // A HOD belongs to exactly one department; every other administrator role
        // has none, so a stale department is cleared on demotion/reassignment.
        val resolvedDepartment =
            if (role == UserRole.FACULTY_ADMIN) department?.id else null
        firestore.collection(USERS).document(uid)
            .update(
                "role", role.name,
                "module", role.departmentModule?.name,
                "department", resolvedDepartment,
                "updatedAt", System.currentTimeMillis(),
            )
            .awaitTask()
    }

    override suspend fun setMemberRole(uid: String, role: UserRole) {
        require(role in MEMBER_ROLES) { "setMemberRole accepts only $MEMBER_ROLES, got $role" }
        // `department` is deliberately absent: a HOD must not be able to move a
        // member into (or out of) another department. `module` is absent for the
        // same reason — it is display metadata derived from the role, a member
        // role grants no module at all, and it is not HOD-writable in the rules.
        firestore.collection(USERS).document(uid)
            .update(
                "role", role.name,
                "updatedAt", System.currentTimeMillis(),
            )
            .awaitTask()
    }

    private companion object {
        const val TAG = "GPFirebaseUsers"
        const val USERS = "users"
    }
}
