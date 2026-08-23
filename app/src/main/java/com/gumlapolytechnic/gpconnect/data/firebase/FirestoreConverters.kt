package com.gumlapolytechnic.gpconnect.data.firebase

import com.gumlapolytechnic.gpconnect.data.model.AdminModule
import com.gumlapolytechnic.gpconnect.data.model.Attachment
import com.gumlapolytechnic.gpconnect.data.model.Audience
import com.gumlapolytechnic.gpconnect.data.model.Notice
import com.gumlapolytechnic.gpconnect.data.model.NoticeCategory
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

private fun String?.toModuleOrNull(): AdminModule? =
    runCatching { AdminModule.valueOf(this ?: "") }.getOrNull()

private fun String?.toCategory(): NoticeCategory =
    runCatching { NoticeCategory.valueOf(this ?: "") }.getOrDefault(NoticeCategory.GENERAL)

private fun Map<*, *>.string(key: String): String = this[key] as? String ?: ""

private fun Map<*, *>.stringOrNull(key: String): String? =
    (this[key] as? String)?.takeIf { it.isNotBlank() }

private fun Map<*, *>.long(key: String): Long = this[key] as? Long ?: 0L
