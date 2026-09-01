package com.gumlapolytechnic.gpconnect.data.firebase

import android.util.Log
import com.gumlapolytechnic.gpconnect.data.model.Department
import com.gumlapolytechnic.gpconnect.data.model.MEMBER_ROLES
import com.gumlapolytechnic.gpconnect.data.model.SignupRequest
import com.gumlapolytechnic.gpconnect.data.model.SignupRequestStatus
import com.gumlapolytechnic.gpconnect.data.model.UserRole
import com.gumlapolytechnic.gpconnect.data.model.sortedForInbox
import com.gumlapolytechnic.gpconnect.data.repository.SignupRequestRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Firestore-backed signup request inbox.
 *
 * Ordering is applied on the client ([sortedForInbox]) rather than with
 * `orderBy`, so the department-filtered query needs no composite index and the
 * project keeps working without a deployed index configuration.
 */
class FirebaseSignupRequestRepository : SignupRequestRepository {

    private val firestore get() = FirebaseServices.firestore

    override fun observeAllRequests(): Flow<Result<List<SignupRequest>>> =
        observe(label = "all") { firestore.collection(SIGNUP_REQUESTS) }

    override fun observeDepartmentRequests(
        department: Department,
    ): Flow<Result<List<SignupRequest>>> = observe(label = department.id) {
        // Required by the rules, not just an optimisation: the `list` rule matches
        // on resource.data.department, so Firestore rejects any query that does
        // not carry this exact equality constraint. A HOD therefore cannot read
        // another department's requests even by editing the client.
        firestore.collection(SIGNUP_REQUESTS).whereEqualTo("department", department.id)
    }

    private fun observe(
        label: String,
        query: () -> com.google.firebase.firestore.Query,
    ): Flow<Result<List<SignupRequest>>> = callbackFlow {
        val registration: ListenerRegistration = query().addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(
                    TAG,
                    "observe($label) failed: code=${error.code} message=${error.message}",
                    error,
                )
                trySend(Result.failure(error))
                return@addSnapshotListener
            }
            val requests = snapshot?.documents
                ?.mapNotNull { it.toSignupRequest() }
                .orEmpty()
                .sortedForInbox()
            trySend(Result.success(requests))
        }
        awaitClose { registration.remove() }
    }

    override suspend fun approve(
        request: SignupRequest,
        decidedBy: String,
        department: Department?,
    ): Result<Unit> {
        val grantedRole = request.requestedRole
        val hodApproval = grantedRole == UserRole.FACULTY_ADMIN
        if (grantedRole !in MEMBER_ROLES && !hodApproval) {
            // Defence in depth: the rules reject this too, but a tampered request
            // document must never be handed to a write.
            val message = "Refusing to approve uid=${request.uid}: illegal requested role $grantedRole"
            Log.e(TAG, message)
            return Result.failure(IllegalArgumentException(message))
        }
        if (hodApproval && department == null) {
            // Requirement 6: a HOD is only complete with exactly one department.
            val message = "Refusing to approve HOD uid=${request.uid}: no department assigned"
            Log.e(TAG, message)
            return Result.failure(IllegalArgumentException(message))
        }
        return runCatching {
            val now = System.currentTimeMillis()
            val batch = firestore.batch()
            batch.update(
                firestore.collection(SIGNUP_REQUESTS).document(request.uid),
                signupDecisionFields(SignupRequestStatus.APPROVED, now, decidedBy, note = null),
            )
            // Same batch as the decision, so an APPROVED request can never exist
            // alongside a still-disabled account. Member approvals write no
            // `module` and no `department`: a member role grants no module, and
            // the profile already carries the applicant's department.
            batch.update(
                firestore.collection(USERS).document(request.uid),
                if (hodApproval) {
                    hodProfileFields(department!!, now)
                } else {
                    mapOf(
                        "role" to grantedRole.name,
                        "enabled" to true,
                        "updatedAt" to now,
                    )
                },
            )
            batch.commit().awaitTask()
            Log.i(TAG, "Approved uid=${request.uid} as $grantedRole by $decidedBy")
        }.onFailure { failure ->
            Log.e(TAG, "approve(uid=${request.uid}) failed: ${failure.message}", failure)
        }.map { }
    }

    override suspend fun reject(
        request: SignupRequest,
        decidedBy: String,
        note: String?,
    ): Result<Unit> = runCatching {
        // The account stays disabled; only the request document changes so the
        // decision (and who made it) remains on record.
        firestore.collection(SIGNUP_REQUESTS).document(request.uid)
            .update(
                signupDecisionFields(
                    status = SignupRequestStatus.REJECTED,
                    decidedAt = System.currentTimeMillis(),
                    decidedBy = decidedBy,
                    note = note,
                ),
            )
            .awaitTask()
        Log.i(TAG, "Rejected uid=${request.uid} by $decidedBy")
    }.onFailure { failure ->
        Log.e(TAG, "reject(uid=${request.uid}) failed: ${failure.message}", failure)
    }.map { }

    private companion object {
        const val TAG = "GPFirebaseSignup"
        const val SIGNUP_REQUESTS = "signupRequests"
        const val USERS = "users"
    }
}
