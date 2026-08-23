package com.gumlapolytechnic.gpconnect.data.mock

import com.gumlapolytechnic.gpconnect.data.model.Notice
import com.gumlapolytechnic.gpconnect.data.repository.NoticeDraft
import com.gumlapolytechnic.gpconnect.data.repository.NoticeQuery
import com.gumlapolytechnic.gpconnect.data.repository.NoticeRepository
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Single in-memory notice store for the whole prototype: student reads and
 * admin CRUD share this source, so every screen reflects the same state.
 * The first collection simulates a short network fetch so loading states are
 * exercised; later re-queries (search, filter, CRUD) answer instantly and
 * reactively. A Firestore implementation replaces this in Phase 4.
 */
class MockNoticeRepository : NoticeRepository {

    private val notices = MutableStateFlow<List<Notice>>(MockNotices.all)
    private val readIds = MutableStateFlow<Set<String>>(emptySet())

    @Volatile
    private var hasFetchedOnce = false

    override fun observeNotices(query: NoticeQuery): Flow<List<Notice>> = flow {
        if (!hasFetchedOnce) {
            delay(INITIAL_FETCH_DELAY_MS)
            hasFetchedOnce = true
        }
        emitAll(notices.map { list -> list.applyQuery(query) })
    }

    override fun observeNotice(id: String): Flow<Notice?> =
        notices.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun getNotice(id: String): Notice? =
        notices.value.firstOrNull { it.id == id }

    override suspend fun createNotice(draft: NoticeDraft): Notice {
        val created = Notice(
            id = "n-${idCounter.incrementAndGet()}",
            title = draft.title,
            body = draft.body,
            category = draft.category,
            isPinned = draft.isPinned,
            audience = draft.audience,
            attachments = draft.attachments,
            author = draft.author,
            createdAt = draft.createdAt,
        )
        notices.update { list -> listOf(created) + list }
        return created
    }

    override suspend fun updateNotice(notice: Notice) {
        notices.update { list ->
            list.map { existing -> existing.takeIf { it.id != notice.id } ?: notice }
        }
    }

    override suspend fun deleteNotice(noticeId: String) {
        notices.update { list -> list.filterNot { it.id == noticeId } }
        readIds.update { ids -> ids - noticeId }
    }

    override suspend fun setPinned(noticeId: String, pinned: Boolean) {
        notices.update { list ->
            list.map { notice ->
                if (notice.id == noticeId) notice.copy(isPinned = pinned) else notice
            }
        }
    }

    override fun observeReadMarkerIds(): Flow<Set<String>> = readIds.asStateFlow()

    override suspend fun markRead(noticeId: String) {
        readIds.update { it + noticeId }
    }

    private fun List<Notice>.applyQuery(query: NoticeQuery): List<Notice> {
        val term = query.search.trim()
        return asSequence()
            .filter { notice -> query.category == null || notice.category == query.category }
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

    private companion object {
        const val INITIAL_FETCH_DELAY_MS = 600L
        val idCounter = AtomicInteger(1000)
    }
}
