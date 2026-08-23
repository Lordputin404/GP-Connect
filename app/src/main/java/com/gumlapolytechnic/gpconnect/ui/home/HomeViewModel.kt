package com.gumlapolytechnic.gpconnect.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gumlapolytechnic.gpconnect.data.mock.CampusEventPreview
import com.gumlapolytechnic.gpconnect.data.model.Notice
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
    val events: List<CampusEventPreview> = emptyList(),
)

/**
 * Home dashboard state holder: pinned/important notices, recent notices with
 * read markers, and the lightweight event previews (Phase 6 replaces the
 * preview list with the real Events module).
 */
class HomeViewModel(
    noticeRepository: NoticeRepository,
    eventPreviews: List<CampusEventPreview>,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        noticeRepository.observeNotices(),
        noticeRepository.observeReadMarkerIds(),
    ) { notices, readIds ->
        HomeUiState(
            isLoading = false,
            isError = false,
            importantNotices = notices.filter { it.isPinned }.take(IMPORTANT_COUNT),
            recentNotices = notices.filter { !it.isPinned }.take(RECENT_COUNT),
            readIds = readIds,
            events = eventPreviews,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(events = eventPreviews),
    )

    private companion object {
        const val IMPORTANT_COUNT = 3
        const val RECENT_COUNT = 4
    }
}
