package com.gumlapolytechnic.gpconnect.data.mock

import com.gumlapolytechnic.gpconnect.data.model.Notice
import com.gumlapolytechnic.gpconnect.data.repository.NoticeDraft
import com.gumlapolytechnic.gpconnect.data.repository.NoticeQuery
import com.gumlapolytechnic.gpconnect.data.repository.NoticeRepository
import com.gumlapolytechnic.gpconnect.data.repository.applyQuery
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
 * Retained mock notice source (migration/testing reference — NOT wired into
 * the production AppContainer since Phase 4B). Single in-memory reactive
 * store shared by student reads and admin CRUD.
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
            createdBy = "mock",
            ownerRole = draft.ownerRole,
            module = draft.module,
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

    private companion object {
        const val INITIAL_FETCH_DELAY_MS = 600L
        val idCounter = AtomicInteger(1000)
    }
}
