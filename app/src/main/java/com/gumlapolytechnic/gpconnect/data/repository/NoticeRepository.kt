package com.gumlapolytechnic.gpconnect.data.repository

import com.gumlapolytechnic.gpconnect.data.model.AdminModule
import com.gumlapolytechnic.gpconnect.data.model.Attachment
import com.gumlapolytechnic.gpconnect.data.model.Audience
import com.gumlapolytechnic.gpconnect.data.model.Notice
import com.gumlapolytechnic.gpconnect.data.model.NoticeCategory
import com.gumlapolytechnic.gpconnect.data.model.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Query for the notice list: optional free-text search (title and body),
 * optional category filter and optional module filter. All combine.
 */
data class NoticeQuery(
    val search: String = "",
    val category: NoticeCategory? = null,
    val module: AdminModule? = null,
)

/**
 * Input for creating a notice. The repository stamps createdBy from the
 * authenticated user; ownerRole/module reflect the acting admin's authority
 * (SUPER_ADMIN publishes GLOBAL content, department admins their own module)
 * and Firestore rules reject mismatches.
 */
data class NoticeDraft(
    val title: String,
    val body: String,
    val category: NoticeCategory,
    val audience: Audience,
    val isPinned: Boolean,
    val attachments: List<Attachment>,
    val author: String,
    val createdAt: Long,
    val ownerRole: UserRole,
    val module: AdminModule,
)

/**
 * Notice data contract (student reads + role-aware admin CRUD). Results are
 * ordered pinned first, then newest first. Read tracking stays per-user via
 * marker IDs (users/{uid}/noticeReads), never a readBy array on the notice.
 */
interface NoticeRepository {
    fun observeNotices(query: NoticeQuery = NoticeQuery()): Flow<List<Notice>>

    fun observeNotice(id: String): Flow<Notice?>

    suspend fun getNotice(id: String): Notice?

    suspend fun createNotice(draft: NoticeDraft): Notice

    /** Replaces the stored notice wholesale; the ID identifies the target. */
    suspend fun updateNotice(notice: Notice)

    suspend fun deleteNotice(noticeId: String)

    suspend fun setPinned(noticeId: String, pinned: Boolean)

    fun observeReadMarkerIds(): Flow<Set<String>>

    suspend fun markRead(noticeId: String)
}

/** Shared client-side query evaluation (search + category + module, pinned-first ordering). */
internal fun List<Notice>.applyQuery(query: NoticeQuery): List<Notice> {
    val term = query.search.trim()
    return asSequence()
        .filter { notice -> query.category == null || notice.category == query.category }
        .filter { notice -> query.module == null || notice.module == query.module }
        .filter { notice ->
            term.isEmpty() ||
                notice.title.contains(term, ignoreCase = true) ||
                notice.body.contains(term, ignoreCase = true)
        }
        .sortedWith(
            compareByDescending<Notice> { it.isPinned }
                .thenByDescending { it.createdAt },
        )
        .toList()
}
