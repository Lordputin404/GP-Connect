package com.gumlapolytechnic.gpconnect.data.repository

import com.gumlapolytechnic.gpconnect.data.model.Attachment
import com.gumlapolytechnic.gpconnect.data.model.Audience
import com.gumlapolytechnic.gpconnect.data.model.Notice
import com.gumlapolytechnic.gpconnect.data.model.NoticeCategory
import kotlinx.coroutines.flow.Flow

/**
 * Query for the notice list: optional free-text search (matches title and
 * body) combined with an optional category filter. Both work together.
 */
data class NoticeQuery(
    val search: String = "",
    val category: NoticeCategory? = null,
)

/**
 * Input for creating a notice. The repository owns ID generation; the draft
 * maps onto a Firestore document in Phase 4.
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
)

/**
 * Notice data contract (student reads + admin CRUD). The UI depends only on
 * this interface; a Firestore implementation replaces the mock in Phase 4.
 * List results are ordered pinned first, then newest first. Read tracking
 * stays per-user via marker IDs — never a readBy array on the notice.
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

    /**
     * IDs of notices the current user has read. Mock implementation keeps an
     * in-memory set; Phase 4 maps this onto users/{uid}/noticeReads/{noticeId}.
     */
    fun observeReadMarkerIds(): Flow<Set<String>>

    suspend fun markRead(noticeId: String)
}
