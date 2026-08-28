package com.gumlapolytechnic.gpconnect.data.model

/**
 * Application roles (Phase 4C hierarchy):
 *
 *  SUPER_ADMIN — global administrator management and all content.
 *  CANTEEN_ADMIN / LIBRARY_ADMIN / FACILITY_ADMIN —
 *      independently manage content belonging to their own module only.
 *  FACULTY_ADMIN — the Head of Department (HOD). Manages the FACULTY module's
 *      content plus the teachers and student signup requests of the **single**
 *      department recorded in [User.department].
 *  TEACHER — staff member belonging to exactly one department. Read-only
 *      content access; no administrative portal.
 *  STUDENT — read-only access to published content.
 *
 * Enum names are persisted verbatim in `users.role` and mirrored by the
 * `adminRoles()` / `memberRoles()` allow-lists in `firestore.rules`.
 */
enum class UserRole {
    SUPER_ADMIN,
    CANTEEN_ADMIN,
    LIBRARY_ADMIN,
    FACULTY_ADMIN,
    FACILITY_ADMIN,
    TEACHER,
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

/**
 * Roles that may enter the administrator portal. TEACHER and STUDENT are
 * *members* of a department, not administrators, and are deliberately absent.
 */
val ADMIN_ROLES: Set<UserRole> = setOf(
    UserRole.SUPER_ADMIN,
    UserRole.CANTEEN_ADMIN,
    UserRole.LIBRARY_ADMIN,
    UserRole.FACULTY_ADMIN,
    UserRole.FACILITY_ADMIN,
)

/** Roles a HOD may assign inside their own department. */
val MEMBER_ROLES: Set<UserRole> = setOf(UserRole.TEACHER, UserRole.STUDENT)

/**
 * True for roles that belong in the admin portal. Derived from [ADMIN_ROLES]
 * rather than `!= STUDENT` so that every future non-admin role (TEACHER was the
 * first) is excluded by default instead of leaking into the admin shell.
 */
val UserRole.isAdmin: Boolean get() = this in ADMIN_ROLES

/**
 * The single content module a role is permitted to manage, or null when the
 * role has no module scope (SUPER_ADMIN is global; TEACHER/STUDENT have none).
 *
 * This is the ONLY authority for module scope in Kotlin. The stored
 * `users.module` field is display metadata: it may be stale or missing, so
 * never derive permissions or queries from it.
 */
val UserRole.departmentModule: AdminModule?
    get() = when (this) {
        UserRole.LIBRARY_ADMIN -> AdminModule.LIBRARY
        UserRole.CANTEEN_ADMIN -> AdminModule.CANTEEN
        UserRole.FACULTY_ADMIN -> AdminModule.FACULTY
        UserRole.FACILITY_ADMIN -> AdminModule.FACILITY
        UserRole.SUPER_ADMIN, UserRole.TEACHER, UserRole.STUDENT -> null
    }

/**
 * Application user resolved from Firebase Authentication (identity) plus the
 * Firestore profile at users/{uid} (role, module, enabled). Student detail
 * fields are nullable because administrator accounts do not have them.
 *
 * [department] stores a canonical [Department] id. Legacy documents may still
 * hold a display label; read it through [departmentOrNull], never raw.
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
    val isAdmin: Boolean get() = role.isAdmin

    /** Resolved department, tolerating legacy display-label values. */
    val departmentOrNull: Department? get() = Department.resolveOrNull(department)

    /** Resolved course, tolerating legacy display-label values. */
    val courseOrNull: Course? get() = Course.resolveOrNull(course)

    /**
     * True only for a FACULTY_ADMIN that is actually bound to one real
     * department. A FACULTY_ADMIN without a resolvable department has no
     * department authority at all — Firestore rules reject its scoped reads.
     */
    val isHod: Boolean get() = role == UserRole.FACULTY_ADMIN && departmentOrNull != null
}
