package com.gumlapolytechnic.gpconnect.data.repository

import com.gumlapolytechnic.gpconnect.data.model.Department
import com.gumlapolytechnic.gpconnect.data.model.DepartmentInfo
import kotlinx.coroutines.flow.Flow

/**
 * Public department information contract. The department *registry* itself is
 * the [Department] enum (mirrored by firestore.rules); this repository covers
 * only the editable per-department info documents at `departments/{id}`.
 *
 * Authority, enforced by firestore.rules:
 *  - Reads of `departments/{id}`: any enabled member.
 *  - Writes to `departments/{id}`: only the HOD bound to exactly that
 *    department (role FACULTY_ADMIN + matching `users.department`). The
 *    SUPER_ADMIN keeps total authority, as everywhere else.
 *
 * HOD identity on the student screen comes from the `hodName` field the HOD
 * stamps on the info document when saving. Students cannot read the users
 * collection (its `list` rule is SUPER_ADMIN/HOD-only), so the HOD's name is
 * carried in this document rather than queried from users — the HOD writes
 * only their own profile's display name, which involves no privilege
 * escalation.
 */
interface DepartmentRepository {
    /**
     * Streams one department's info document. Emits null when the document
     * does not exist yet (never filled in) — the UI treats that as "not
     * available" rather than an error. Result-wrapped so a rules rejection
     * surfaces as an error state instead of an empty screen.
     */
    fun observeDepartmentInfo(department: Department): Flow<Result<DepartmentInfo?>>

    /**
     * Creates or replaces the info document of [department], stamped with the
     * saving HOD's [hodName]. HOD-only for the caller's own department
     * (server-enforced); returns [Result] so a rules rejection shows as an
     * admin error state instead of a crash.
     */
    suspend fun saveDepartmentInfo(
        department: Department,
        info: DepartmentInfo,
        hodName: String,
    ): Result<Unit>
}
