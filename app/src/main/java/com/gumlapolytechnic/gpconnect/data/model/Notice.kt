package com.gumlapolytechnic.gpconnect.data.model

/** Official notice categories. */
enum class NoticeCategory {
    EXAM,
    GENERAL,
    EVENT,
    HOLIDAY,
    LIBRARY,
    ASSIGNMENT,
}

/** Who a notice is addressed to. */
sealed interface Audience {
    data object All : Audience
    data class Department(val department: String) : Audience
    data class Course(val course: String, val semester: Int? = null) : Audience
}

/** Demo attachment reference — name only until Firebase Storage exists (Phase 4+). */
data class Attachment(val name: String)

/**
 * Official college notice. Timestamps are epoch milliseconds. Ownership
 * metadata (createdBy uid, ownerRole, module) drives module-scoped
 * authorization for department admins; Firestore security rules enforce it.
 */
data class Notice(
    val id: String,
    val title: String,
    val body: String,
    val category: NoticeCategory,
    val isPinned: Boolean,
    val audience: Audience,
    val attachments: List<Attachment> = emptyList(),
    val author: String,
    val createdAt: Long,
    val updatedAt: Long = createdAt,
    val createdBy: String = "",
    val ownerRole: UserRole = UserRole.SUPER_ADMIN,
    val module: AdminModule = AdminModule.GLOBAL,
)
