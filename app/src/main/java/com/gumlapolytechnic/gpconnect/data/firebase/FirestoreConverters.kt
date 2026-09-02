package com.gumlapolytechnic.gpconnect.data.firebase

import com.gumlapolytechnic.gpconnect.data.model.AdminModule
import com.gumlapolytechnic.gpconnect.data.model.Attachment
import com.gumlapolytechnic.gpconnect.data.model.Audience
import com.gumlapolytechnic.gpconnect.data.model.CanteenOrder
import com.gumlapolytechnic.gpconnect.data.model.Department
import com.gumlapolytechnic.gpconnect.data.model.Notice
import com.gumlapolytechnic.gpconnect.data.model.NoticeCategory
import com.gumlapolytechnic.gpconnect.data.model.OrderItemSnapshot
import com.gumlapolytechnic.gpconnect.data.model.OrderStatus
import com.gumlapolytechnic.gpconnect.data.model.SignupRequest
import com.gumlapolytechnic.gpconnect.data.model.SignupRequestStatus
import com.gumlapolytechnic.gpconnect.data.model.SignupSubmission
import com.gumlapolytechnic.gpconnect.data.model.User
import com.gumlapolytechnic.gpconnect.data.model.UserRole
import com.google.firebase.firestore.DocumentSnapshot

/**
 * Firestore document ↔ domain model converters. Parsing is defensive: unknown
 * role/module strings or malformed shapes degrade to safe defaults rather
 * than crashing client screens.
 */

internal fun DocumentSnapshot.toUser(): User? {
    val data = data ?: return null
    return User(
        id = id,
        email = data.string("email"),
        name = data.string("displayName").ifBlank { data.string("email") },
        role = data.string("role").toRole(),
        enabled = data["enabled"] as? Boolean ?: true,
        module = data.stringOrNull("module").toModuleOrNull(),
        rollNo = data.stringOrNull("rollNo"),
        course = data.stringOrNull("course"),
        semester = (data["semester"] as? Long)?.toInt(),
        department = data.stringOrNull("department"),
    )
}

internal fun DocumentSnapshot.toSignupRequest(): SignupRequest? {
    val data = data ?: return null
    return SignupRequest(
        uid = data.stringOrNull("uid") ?: id,
        email = data.string("email"),
        name = data.string("displayName").ifBlank { data.string("email") },
        requestedRole = data.string("requestedRole").toRole(),
        department = data.string("department"),
        course = data.stringOrNull("course"),
        semester = (data["semester"] as? Long)?.toInt(),
        rollNo = data.stringOrNull("rollNo"),
        status = data.stringOrNull("status").toRequestStatus(),
        createdAt = data.long("createdAt"),
        decidedAt = (data["decidedAt"] as? Long),
        decidedBy = data.stringOrNull("decidedBy"),
        decisionNote = data.stringOrNull("decisionNote"),
    )
}

/**
 * The `users/{uid}` document written at signup. Always a **disabled STUDENT**,
 * whatever role was requested: the security rules only ever permit a
 * self-created profile in that shape, so self-registration cannot mint a
 * teacher or an administrator. The requested role lives on the signup request
 * and is applied by the HOD on approval.
 *
 * Optional keys are omitted entirely (not written as null) because the rules
 * validate them with key-presence checks.
 */
internal fun pendingMemberProfileFields(
    submission: SignupSubmission,
    createdAt: Long,
): Map<String, Any?> = buildMap {
    put("email", submission.email)
    put("displayName", submission.name)
    put("role", UserRole.STUDENT.name)
    put("enabled", false)
    put("department", submission.department.id)
    put("createdAt", createdAt)
    submission.course?.let { put("course", it.id) }
    submission.semester?.let { put("semester", it.toLong()) }
    submission.rollNo?.takeIf { it.isNotBlank() }?.let { put("rollNo", it) }
}

/** The PENDING `signupRequests/{uid}` document written at signup. */
internal fun signupRequestFields(
    uid: String,
    submission: SignupSubmission,
    createdAt: Long,
): Map<String, Any?> = buildMap {
    put("uid", uid)
    put("email", submission.email)
    put("displayName", submission.name)
    put("requestedRole", submission.requestedRole.name)
    put("department", submission.department.id)
    put("status", SignupRequestStatus.PENDING.name)
    put("createdAt", createdAt)
    submission.course?.let { put("course", it.id) }
    submission.semester?.let { put("semester", it.toLong()) }
    submission.rollNo?.takeIf { it.isNotBlank() }?.let { put("rollNo", it) }
}

/**
 * The HOD's decision patch. Only these four keys may change on an existing
 * request (enforced by the rules), and the deciding uid is recorded so an
 * approval can always be attributed.
 */
internal fun signupDecisionFields(
    status: SignupRequestStatus,
    decidedAt: Long,
    decidedBy: String,
    note: String?,
): Map<String, Any?> = buildMap {
    put("status", status.name)
    put("decidedAt", decidedAt)
    put("decidedBy", decidedBy)
    note?.takeIf { it.isNotBlank() }?.let { put("decisionNote", it) }
}

/**
 * The `users/{uid}` patch a SUPER_ADMIN writes when approving a HOD signup
 * request: role = FACULTY_ADMIN, enabled = true, bound to exactly the one
 * [department] confirmed during approval. `module` is FACULTY display metadata
 * derived from the role, matching what Admin Management writes.
 */
internal fun hodProfileFields(
    department: Department,
    updatedAt: Long,
): Map<String, Any?> = mapOf(
    "role" to UserRole.FACULTY_ADMIN.name,
    "enabled" to true,
    "module" to AdminModule.FACULTY.name,
    "department" to department.id,
    "updatedAt" to updatedAt,
)

internal fun DocumentSnapshot.toNotice(): Notice? {
    val data = data ?: return null
    return Notice(
        id = id,
        title = data.string("title"),
        body = data.string("body"),
        category = data.string("category").toCategory(),
        isPinned = data["isPinned"] as? Boolean ?: false,
        audience = audienceFrom(data["audience"]),
        attachments = (data["attachments"] as? List<*>)
            ?.mapNotNull { entry -> (entry as? Map<*, *>)?.string("name")?.let(::Attachment) }
            .orEmpty(),
        author = data.string("author"),
        createdAt = data.long("createdAt"),
        updatedAt = data.long("updatedAt").takeIf { it != 0L } ?: data.long("createdAt"),
        createdBy = data.string("createdBy"),
        ownerRole = data.string("ownerRole").toRole(),
        module = data.stringOrNull("module").toModuleOrNull() ?: AdminModule.GLOBAL,
    )
}

internal fun noticeFields(
    title: String,
    body: String,
    category: NoticeCategory,
    isPinned: Boolean,
    audience: Audience,
    attachments: List<Attachment>,
    author: String,
    createdAt: Long,
    updatedAt: Long,
    createdBy: String,
    ownerRole: UserRole,
    module: AdminModule,
): Map<String, Any?> = mapOf(
    "title" to title,
    "body" to body,
    "category" to category.name,
    "isPinned" to isPinned,
    "audience" to audienceFields(audience),
    "attachments" to attachments.map { mapOf("name" to it.name) },
    "author" to author,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt,
    "createdBy" to createdBy,
    "ownerRole" to ownerRole.name,
    "module" to module.name,
)

private fun audienceFields(audience: Audience): Map<String, Any?> = when (audience) {
    Audience.All -> mapOf("type" to "ALL")
    is Audience.Department -> mapOf("type" to "DEPARTMENT", "value" to audience.department)
    is Audience.Course -> mapOf(
        "type" to "COURSE",
        "course" to audience.course,
        "semester" to audience.semester,
    )
}

private fun audienceFrom(value: Any?): Audience {
    val map = value as? Map<*, *> ?: return Audience.All
    return when (map.string("type")) {
        "DEPARTMENT" -> Audience.Department(map.string("value"))
        "COURSE" -> Audience.Course(
            course = map.string("course"),
            semester = (map["semester"] as? Long)?.toInt(),
        )
        else -> Audience.All
    }
}

private fun String?.toRole(): UserRole =
    runCatching { UserRole.valueOf(this ?: "") }.getOrDefault(UserRole.STUDENT)

private fun String?.toRequestStatus(): SignupRequestStatus =
    runCatching { SignupRequestStatus.valueOf(this ?: "") }
        .getOrDefault(SignupRequestStatus.PENDING)

private fun String?.toOrderStatus(): OrderStatus =
    runCatching { OrderStatus.valueOf(this ?: "") }
        .getOrDefault(OrderStatus.PENDING)

private fun String?.toModuleOrNull(): AdminModule? =
    runCatching { AdminModule.valueOf(this ?: "") }.getOrNull()

private fun String?.toCategory(): NoticeCategory =
    runCatching { NoticeCategory.valueOf(this ?: "") }.getOrDefault(NoticeCategory.GENERAL)

internal fun DocumentSnapshot.toCanteenOrder(): CanteenOrder? {
    val data = data ?: return null
    return CanteenOrder(
        id = id,
        customerId = data.string("customerId"),
        customerName = data.string("customerName"),
        customerEmail = data.string("customerEmail"),
        items = (data["items"] as? List<*>)
            ?.mapNotNull { entry ->
                val m = entry as? Map<*, *> ?: return@mapNotNull null
                OrderItemSnapshot(
                    menuItemId = m.string("menuItemId"),
                    name = m.string("name"),
                    pricePaise = m.long("pricePaise"),
                    quantity = (m["quantity"] as? Long)?.toInt() ?: 0,
                )
            }
            .orEmpty(),
        totalAmountPaise = data.long("totalAmountPaise"),
        status = data.string("status").toOrderStatus(),
        createdAt = data.long("createdAt"),
        updatedAt = data.long("updatedAt"),
        decidedBy = data.stringOrNull("decidedBy"),
        cancellationReason = data.stringOrNull("cancellationReason"),
    )
}


private fun Map<*, *>.string(key: String): String = this[key] as? String ?: ""

private fun Map<*, *>.stringOrNull(key: String): String? =
    (this[key] as? String)?.takeIf { it.isNotBlank() }

private fun Map<*, *>.long(key: String): Long = this[key] as? Long ?: 0L
