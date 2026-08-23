package com.gumlapolytechnic.gpconnect.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gumlapolytechnic.gpconnect.data.model.Notice
import com.gumlapolytechnic.gpconnect.data.repository.NoticeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AdminDashboardUiState(
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val totalNotices: Int = 0,
    val pinnedNotices: Int = 0,
    val recentNotices: Int = 0,
    val notices: List<Notice> = emptyList(),
)

/**
 * Admin dashboard state holder: overview counters plus the full management
 * list from the shared notice repository. Deletion and pin toggling go
 * through the repository so every student screen updates automatically.
 */
class AdminDashboardViewModel(private val noticeRepository: NoticeRepository) : ViewModel() {

    private val refresh = MutableStateFlow(0)

    val uiState: StateFlow<AdminDashboardUiState> = combine(
        noticeRepository.observeNotices(),
        refresh,
    ) { notices, _ ->
        val weekAgo = System.currentTimeMillis() - RECENT_WINDOW_MS
        AdminDashboardUiState(
            isLoading = false,
            isError = false,
            totalNotices = notices.size,
            pinnedNotices = notices.count { it.isPinned },
            recentNotices = notices.count { it.createdAt >= weekAgo },
            notices = notices,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AdminDashboardUiState(),
    )

    fun deleteNotice(noticeId: String) {
        viewModelScope.launch { noticeRepository.deleteNotice(noticeId) }
    }

    fun togglePinned(notice: Notice) {
        viewModelScope.launch { noticeRepository.setPinned(notice.id, !notice.isPinned) }
    }

    fun retry() {
        refresh.value += 1
    }

    private companion object {
        const val RECENT_WINDOW_MS = 7 * 86_400_000L
    }
}
