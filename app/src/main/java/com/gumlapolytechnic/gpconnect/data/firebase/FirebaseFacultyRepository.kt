package com.gumlapolytechnic.gpconnect.data.firebase

import android.util.Log
import com.gumlapolytechnic.gpconnect.data.model.Department
import com.gumlapolytechnic.gpconnect.data.model.Faculty
import com.gumlapolytechnic.gpconnect.data.repository.FacultyDraft
import com.gumlapolytechnic.gpconnect.data.repository.FacultyRepository
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Firestore-backed faculty directory. Faculty documents live in the
 * subcollection `departments/{departmentId}/faculty/{facultyId}` — the same
 * departments tree the info documents use, so the department scoping is part
 * of the document's address and the rules can compare the path segment
 * against the caller's own department.
 *
 * Reads are the department's own members/HOD/SUPER_ADMIN; writes are the
 * department's HOD/SUPER_ADMIN — enforced by firestore.rules, not here.
 */
class FirebaseFacultyRepository : FacultyRepository {

    private val firestore get() = FirebaseServices.firestore

    private fun collection(department: Department) =
        firestore.collection(DEPARTMENTS).document(department.id).collection(FACULTY)

    override fun observeFaculty(department: Department): Flow<Result<List<Faculty>>> =
        callbackFlow {
            val registration: ListenerRegistration =
                collection(department)
                    .orderBy("name", Query.Direction.ASCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e(
                                TAG,
                                "observeFaculty(${department.id}) failed: ${error.message}",
                                error,
                            )
                            trySend(Result.failure(error))
                            return@addSnapshotListener
                        }
                        val faculty = snapshot?.documents
                            ?.mapNotNull { it.toFaculty(department) }
                            .orEmpty()
                        trySend(Result.success(faculty))
                    }
            awaitClose { registration.remove() }
        }

    override suspend fun getFaculty(department: Department, facultyId: String): Faculty? =
        collection(department).document(facultyId).get().awaitTask()
            ?.toFaculty(department)

    override suspend fun createFaculty(department: Department, draft: FacultyDraft): Result<Unit> =
        runCatching {
            val now = System.currentTimeMillis()
            collection(department)
                .add(facultyFields(department, draft.name, draft.designation, draft.roomNumber, draft.email, draft.phone, createdAt = now, updatedAt = now))
                .awaitTask()
            Log.i(TAG, "Created faculty '${draft.name}' in ${department.id}")
        }.onFailure { failure ->
            Log.e(TAG, "createFaculty(${department.id}) failed: ${failure.message}", failure)
        }.map { }

    override suspend fun updateFaculty(department: Department, faculty: Faculty): Result<Unit> =
        runCatching {
            collection(department).document(faculty.id)
                .set(
                    facultyFields(
                        department,
                        faculty.name,
                        faculty.designation,
                        faculty.roomNumber,
                        faculty.email,
                        faculty.phone,
                        createdAt = faculty.createdAt,
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
                .awaitTask()
            Log.i(TAG, "Updated faculty '${faculty.name}' (${faculty.id})")
        }.onFailure { failure ->
            Log.e(TAG, "updateFaculty(${faculty.id}) failed: ${failure.message}", failure)
        }.map { }

    override suspend fun deleteFaculty(department: Department, facultyId: String): Result<Unit> =
        runCatching {
            collection(department).document(facultyId).delete().awaitTask()
            Log.i(TAG, "Deleted faculty $facultyId from ${department.id}")
        }.onFailure { failure ->
            Log.e(TAG, "deleteFaculty($facultyId) failed: ${failure.message}", failure)
        }.map { }

    private companion object {
        const val TAG = "GPFirebaseFaculty"
        const val DEPARTMENTS = "departments"
        const val FACULTY = "faculty"
    }
}

/** All fields written to one faculty document. */
internal fun facultyFields(
    department: Department,
    name: String,
    designation: String,
    roomNumber: String,
    email: String,
    phone: String,
    createdAt: Long,
    updatedAt: Long,
): Map<String, Any?> = mapOf(
    "departmentId" to department.id,
    "name" to name.trim(),
    "designation" to designation.trim(),
    "roomNumber" to roomNumber.trim(),
    "email" to email.trim(),
    "phone" to phone.trim(),
    "createdAt" to createdAt,
    "updatedAt" to updatedAt,
)

/** Defensive parse of `departments/{id}/faculty/{fid}`. */
internal fun com.google.firebase.firestore.DocumentSnapshot.toFaculty(
    department: Department,
): Faculty? {
    if (!exists()) return null
    return Faculty(
        id = id,
        departmentId = department.id,
        name = getString("name").orEmpty(),
        designation = getString("designation").orEmpty(),
        roomNumber = getString("roomNumber").orEmpty(),
        email = getString("email").orEmpty(),
        phone = getString("phone").orEmpty(),
        createdAt = getLong("createdAt") ?: 0L,
        updatedAt = getLong("updatedAt") ?: 0L,
    )
}
