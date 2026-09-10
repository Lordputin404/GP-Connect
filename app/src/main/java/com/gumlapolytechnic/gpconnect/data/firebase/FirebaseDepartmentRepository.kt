package com.gumlapolytechnic.gpconnect.data.firebase

import android.util.Log
import com.gumlapolytechnic.gpconnect.data.model.Department
import com.gumlapolytechnic.gpconnect.data.model.DepartmentInfo
import com.gumlapolytechnic.gpconnect.data.repository.DepartmentRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Firestore-backed department info. The collection `departments` holds one
 * optional document per department, keyed by the canonical [Department] id —
 * the same registry the HOD assignment system uses, never a duplicate one.
 *
 * Reads are any enabled member (rules); writes are the department's own HOD
 * only, and the HOD stamps their own display name on the document (hodName)
 * so students can see who heads the department without needing users-collection
 * access, which the rules do not grant them.
 */
class FirebaseDepartmentRepository : DepartmentRepository {

    private val firestore get() = FirebaseServices.firestore

    override fun observeDepartmentInfo(department: Department): Flow<Result<DepartmentInfo?>> =
        callbackFlow {
            val registration: ListenerRegistration =
                firestore.collection(DEPARTMENTS).document(department.id)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e(
                                TAG,
                                "observeDepartmentInfo(${department.id}) failed: ${error.message}",
                                error,
                            )
                            trySend(Result.failure(error))
                            return@addSnapshotListener
                        }
                        trySend(Result.success(snapshot?.toDepartmentInfo(department)))
                    }
            awaitClose { registration.remove() }
        }

    override suspend fun saveDepartmentInfo(
        department: Department,
        info: DepartmentInfo,
        hodName: String,
    ): Result<Unit> = runCatching {
        firestore.collection(DEPARTMENTS).document(department.id)
            .set(
                mapOf(
                    "departmentId" to department.id,
                    "hodName" to hodName.trim(),
                    "about" to info.about.trim(),
                    "officeRoom" to info.officeRoom.trim(),
                    "contact" to info.contact.trim(),
                    "updatedAt" to System.currentTimeMillis(),
                ),
            )
            .awaitTask()
        Log.i(TAG, "Saved info for department ${department.id}")
    }.onFailure { failure ->
        Log.e(TAG, "saveDepartmentInfo(${department.id}) failed: ${failure.message}", failure)
    }.map { }

    private companion object {
        const val TAG = "GPFirebaseDepartments"
        const val DEPARTMENTS = "departments"
    }
}

/** Parses `departments/{id}`; null when the document does not exist (yet). */
private fun com.google.firebase.firestore.DocumentSnapshot.toDepartmentInfo(
    department: Department,
): DepartmentInfo? {
    if (!exists()) return null
    return DepartmentInfo(
        departmentId = department.id,
        hodName = getString("hodName")?.trim().orEmpty(),
        about = getString("about")?.trim().orEmpty(),
        officeRoom = getString("officeRoom")?.trim().orEmpty(),
        contact = getString("contact")?.trim().orEmpty(),
        updatedAt = getLong("updatedAt") ?: 0L,
    )
}
