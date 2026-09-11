package com.gumlapolytechnic.gpconnect.data.repository

import com.gumlapolytechnic.gpconnect.data.model.Department
import com.gumlapolytechnic.gpconnect.data.model.Faculty
import kotlinx.coroutines.flow.Flow

/** Input for creating a faculty entry. The repository stamps timestamps. */
data class FacultyDraft(
    val name: String,
    val designation: String,
    val roomNumber: String,
    val email: String,
    val phone: String,
)

/**
 * Department faculty directory contract, stored under
 * `departments/{departmentId}/faculty/{facultyId}`.
 *
 * Authority, enforced by firestore.rules:
 *  - Reads: an enabled member of exactly [department], the department's own
 *    HOD, or the SUPER_ADMIN. A student therefore sees only their own
 *    department's faculty — the caller's department is taken from their
 *    profile, never from a screen parameter.
 *  - Writes: the HOD bound to exactly [department], or the SUPER_ADMIN.
 */
interface FacultyRepository {
    /**
     * Streams the faculty of [department]. Result-wrapped so a rules
     * rejection surfaces as an error state instead of an empty list.
     */
    fun observeFaculty(department: Department): Flow<Result<List<Faculty>>>

    /** One-shot fetch of a single faculty document. */
    suspend fun getFaculty(department: Department, facultyId: String): Faculty?

    suspend fun createFaculty(department: Department, draft: FacultyDraft): Result<Unit>

    suspend fun updateFaculty(department: Department, faculty: Faculty): Result<Unit>

    suspend fun deleteFaculty(department: Department, facultyId: String): Result<Unit>
}
