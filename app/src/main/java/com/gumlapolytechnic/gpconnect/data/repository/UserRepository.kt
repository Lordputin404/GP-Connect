package com.gumlapolytechnic.gpconnect.data.repository

import com.gumlapolytechnic.gpconnect.data.model.Department
import com.gumlapolytechnic.gpconnect.data.model.User
import com.gumlapolytechnic.gpconnect.data.model.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * User account management. Every operation here is additionally constrained by
 * Firestore security rules, not just by hidden UI:
 *  - [observeUsers] / [setAdminRole] are SUPER_ADMIN only.
 *  - [observeDepartmentMembers] / [setMemberRole] are limited to the caller's
 *    own department (HOD), or unrestricted for SUPER_ADMIN.
 *
 * BACKEND LIMITATION (documented, not faked): creating a *Firebase
 * Authentication* account for another person cannot be done safely from the
 * signed-in client — createUserWithEmailAndPassword would replace the current
 * session. Real account creation on someone else's behalf requires the Firebase
 * Admin SDK / a trusted backend (later phase). Administrator Auth accounts are
 * therefore still created manually in the Firebase Console; students and
 * teachers create their own account through the signup flow
 * ([AuthRepository.register]) and a HOD approves it.
 */
interface UserRepository {
    /** All application users; restricted to SUPER_ADMIN by security rules. */
    fun observeUsers(): Flow<List<User>>

    /**
     * Students and teachers of a single department, for HOD management.
     *
     * The query carries `whereEqualTo("department", …)` because the `list` rule
     * matches on `resource.data.department`; Firestore rejects any query that
     * does not carry that exact constraint, which is what makes the HOD's
     * department isolation server-enforced rather than cosmetic.
     *
     * Unlike [observeUsers] this emits a [Result] so a rules rejection is
     * visible in the UI instead of being silently rendered as "empty".
     */
    fun observeDepartmentMembers(department: Department): Flow<Result<List<User>>>

    suspend fun setEnabled(uid: String, enabled: Boolean)

    /**
     * SUPER_ADMIN: assign an administrator role. [department] is required for
     * FACULTY_ADMIN (a HOD belongs to exactly one department) and is cleared for
     * every other administrator role.
     */
    suspend fun setAdminRole(uid: String, role: UserRole, department: Department?)

    /**
     * HOD: change a member's role within their own department. Only
     * [com.gumlapolytechnic.gpconnect.data.model.MEMBER_ROLES] are accepted and
     * the department field is deliberately left untouched, so this can never
     * move a member between departments or grant administrator access.
     */
    suspend fun setMemberRole(uid: String, role: UserRole)
}
