package com.gumlapolytechnic.gpconnect.ui.notices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gumlapolytechnic.gpconnect.data.model.Notice
import com.gumlapolytechnic.gpconnect.data.repository.NoticeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NoticeDetailUiState(
    val isLoading: Boolean = true,
    val notice: Notice? = null,
)

/**
 * Notice detail state holder: streams the single notice and marks it read.
 * Read markers live per user in the repository (users/{uid}/noticeReads in
 * Phase 4) — never a readBy array on the notice itself.
 */
class NoticeDetailViewModel(
    private val noticeRepository: NoticeRepository,
    private val noticeId: String,
) : ViewModel() {

    val uiState: StateFlow<NoticeDetailUiState> = noticeRepository.observeNotice(noticeId)
        .map { notice -> NoticeDetailUiState(isLoading = false, notice = notice) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NoticeDetailUiState(),
        )

    fun markAsRead() {
        viewModelScope.launch { noticeRepository.markRead(noticeId) }
    }
}
