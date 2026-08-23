package com.gumlapolytechnic.gpconnect.data.model

/**
 * Application roles (Phase 4B hierarchy):
 *
 *  SUPER_ADMIN — global administrator management and all content.
 *  CANTEEN_ADMIN / LIBRARY_ADMIN / FACULTY_ADMIN / FACILITY_ADMIN —
 *      independently manage content belonging to their own module only.
 *  STUDENT — read-only access to published content.
 */
enum class UserRole {
    SUPER_ADMIN,
    CANTEEN_ADMIN,
    LIBRARY_ADMIN,
    FACULTY_ADMIN,
    FACILITY_ADMIN,
    STUDENT,
}

/** Content modules a notice (or department admin) belongs to. */
enum class AdminModule {
    LIBRARY,
    CANTEEN,
    FACULTY,
    FACILITY,
    GLOBAL,
}

/** The content module a role is permitted to manage, or null for STUDENT/SUPER_ADMIN. */
val UserRole.departmentModule: AdminModule?
    get() = when (this) {
        UserRole.LIBRARY_ADMIN -> AdminModule.LIBRARY
        UserRole.CANTEEN_ADMIN -> AdminModule.CANTEEN
        UserRole.FACULTY_ADMIN -> AdminModule.FACULTY
        UserRole.FACILITY_ADMIN -> AdminModule.FACILITY
        UserRole.SUPER_ADMIN, UserRole.STUDENT -> null
    }

/**
 * Application user resolved from Firebase Authentication (identity) plus the
 * Firestore profile at users/{uid} (role, module, enabled). Student detail
 * fields are nullable because administrator accounts do not have them.
 */
data class User(
    val id: String,
    val email: String,
    val name: String,
    val role: UserRole,
    val enabled: Boolean = true,
    val module: AdminModule? = null,
    val rollNo: String? = null,
    val course: String? = null,
    val semester: Int? = null,
    val department: String? = null,
) {
    val isAdmin: Boolean get() = role != UserRole.STUDENT
}
