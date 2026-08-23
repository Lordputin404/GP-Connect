package com.gumlapolytechnic.gpconnect.data.repository

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
 * Notice data contract. The UI depends only on this interface; a Firestore
 * implementation replaces the mock in Phase 4. Results are ordered pinned
 * first, then newest first.
 */
interface NoticeRepository {
    fun observeNotices(query: NoticeQuery = NoticeQuery()): Flow<List<Notice>>

    fun observeNotice(id: String): Flow<Notice?>

    /**
     * IDs of notices the current user has read. Mock implementation keeps an
     * in-memory set; Phase 4 maps this onto users/{uid}/noticeReads/{noticeId}
     * (no readBy array on the notice itself, per architecture decision).
     */
    fun observeReadMarkerIds(): Flow<Set<String>>

    suspend fun markRead(noticeId: String)
}
