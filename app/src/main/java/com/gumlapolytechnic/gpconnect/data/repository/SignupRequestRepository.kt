package com.gumlapolytechnic.gpconnect.data.repository

import com.gumlapolytechnic.gpconnect.data.model.Department
import com.gumlapolytechnic.gpconnect.data.model.SignupRequest
import kotlinx.coroutines.flow.Flow

/**
 * Signup request inbox. Reads are constrained by Firestore security rules:
 * SUPER_ADMIN sees every request, a HOD sees only their own department's, and an
 * applicant may read only their own document.
 *
 * Flows emit a [Result] rather than a bare list so that a rules rejection (the
 * most likely misconfiguration on this new surface) surfaces as an error state
 * instead of an indistinguishable empty inbox.
 */
interface SignupRequestRepository {
    /** Every request, across all departments. SUPER_ADMIN only. */
    fun observeAllRequests(): Flow<Result<List<SignupRequest>>>

    /**
     * Requests for one department. The query carries
     * `whereEqualTo("department", …)`, which the `list` rule requires — a HOD
     * cannot widen it to another department because Firestore evaluates the
     * query's constraints against the rule before returning any document.
     */
    fun observeDepartmentRequests(department: Department): Flow<Result<List<SignupRequest>>>

    /**
     * Approve [request]: atomically stamp the request APPROVED and flip
     * `users/{uid}` to `enabled = true` with the requested role. One
     * [com.google.firebase.firestore.WriteBatch], so an approved request can
     * never exist alongside a still-disabled account.
     *
     * A HOD (FACULTY_ADMIN) request must be approved by a SUPER_ADMIN, which
     * assigns the HOD's department here: exactly one [Department] is required
     * for it and the profile is written with role = FACULTY_ADMIN, the
     * department, enabled = true and module = FACULTY. Member approvals leave
     * [department] null and behave exactly as before.
     */
    suspend fun approve(request: SignupRequest, decidedBy: String, department: Department? = null): Result<Unit>

    /**
     * Reject [request]. The account stays disabled; only the request document
     * changes, so the record of the decision (and who made it) is preserved.
     */
    suspend fun reject(request: SignupRequest, decidedBy: String, note: String?): Result<Unit>
}
