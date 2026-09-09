package com.gumlapolytechnic.gpconnect.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gumlapolytechnic.gpconnect.data.model.CalendarEvent
import com.gumlapolytechnic.gpconnect.data.model.Notice
import com.gumlapolytechnic.gpconnect.data.repository.CalendarQuery
import com.gumlapolytechnic.gpconnect.data.repository.CalendarRepository
import com.gumlapolytechnic.gpconnect.data.repository.NoticeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val importantNotices: List<Notice> = emptyList(),
    val recentNotices: List<Notice> = emptyList(),
    val readIds: Set<String> = emptySet(),
    val events: List<CalendarEvent> = emptyList(),
)

/**
 * Home dashboard state holder: pinned/important notices, recent notices with
 * read markers, and upcoming published calendar events (max three, soonest
 * first — the repository's upcoming-first ordering). Notice/calendar streams
 * combine independently: one failing must not blank the whole dashboard.
 */
class HomeViewModel(
    noticeRepository: NoticeRepository,
    calendarRepository: CalendarRepository,
) : ViewModel() {

    private val notices = noticeRepository.observeNotices()
    private val readMarkers = noticeRepository.observeReadMarkerIds()
    private val events = calendarRepository.observeEvents(
        CalendarQuery(publishedOnly = true, upcomingOnly = true),
    )

    val uiState: StateFlow<HomeUiState> = combine(
        notices,
        readMarkers,
        events,
    ) { noticeList, readIds, eventResult ->
        HomeUiState(
            isLoading = false,
            isError = false,
            importantNotices = noticeList.filter { it.isPinned }.take(IMPORTANT_COUNT),
            recentNotices = noticeList.filter { !it.isPinned }.take(RECENT_COUNT),
            readIds = readIds,
            events = eventResult.getOrDefault(emptyList()).take(UPCOMING_COUNT),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    private companion object {
        const val IMPORTANT_COUNT = 3
        const val RECENT_COUNT = 4
        const val UPCOMING_COUNT = 3
    }
}
