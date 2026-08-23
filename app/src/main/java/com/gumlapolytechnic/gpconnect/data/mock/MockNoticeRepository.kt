package com.gumlapolytechnic.gpconnect.data.mock

import com.gumlapolytechnic.gpconnect.data.repository.NoticeQuery
import com.gumlapolytechnic.gpconnect.data.repository.NoticeRepository
import com.gumlapolytechnic.gpconnect.data.model.Notice
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.flow

/**
 * In-memory notice source. The first collection simulates a short network
 * fetch so loading states are exercised; later re-queries (search, filter) are
 * answered instantly. A Firestore implementation replaces this in Phase 4.
 */
class MockNoticeRepository : NoticeRepository {

    private val readIds = MutableStateFlow<Set<String>>(emptySet())

    @Volatile
    private var hasFetchedOnce = false

    override fun observeNotices(query: NoticeQuery): Flow<List<Notice>> = flow {
        if (!hasFetchedOnce) {
            delay(INITIAL_FETCH_DELAY_MS)
            hasFetchedOnce = true
        }
        val term = query.search.trim()
        emit(
            MockNotices.all.asSequence()
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
                .toList(),
        )
    }

    override fun observeNotice(id: String): Flow<Notice?> =
        observeNotices().map { notices -> notices.firstOrNull { it.id == id } }

    override fun observeReadMarkerIds(): Flow<Set<String>> = readIds.asStateFlow()

    override suspend fun markRead(noticeId: String) {
        readIds.update { it + noticeId }
    }

    private companion object {
        const val INITIAL_FETCH_DELAY_MS = 600L
    }
}
