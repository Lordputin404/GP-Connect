package com.gumlapolytechnic.gpconnect.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gumlapolytechnic.gpconnect.data.model.CalendarEvent
import com.gumlapolytechnic.gpconnect.data.model.EventAttachment
import com.gumlapolytechnic.gpconnect.data.repository.CalendarRepository
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * A user-visible message about an attachment that failed to open, with the
 * reason so the screen can show the right text (download failure vs. no
 * installed viewer).
 */
enum class AttachmentOpenMessage { DOWNLOAD_FAILED, NO_VIEWER }

data class CalendarEventDetailUiState(
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val event: CalendarEvent? = null,
    /** Attachment currently being downloaded for opening; null when idle. */
    val openingAttachment: EventAttachment? = null,
    /** One-shot message shown after a failed open attempt; consumed by the UI. */
    val openMessage: AttachmentOpenMessage? = null,
)

/**
 * Calendar event detail state: streams the single event. The rules `get`
 * grants members published events and the SUPER_ADMIN everything, so an
 * unpublished event simply shows as "not found" for a member.
 *
 * Attachments: tapping one downloads its Storage object into the private
 * cache via the repository (auth-governed) and hands the cached file to the
 * UI, which opens it externally through the FileProvider.
 */
class CalendarEventDetailViewModel(
    private val calendarRepository: CalendarRepository,
    private val eventId: String,
) : ViewModel() {

    private val _openingAttachment = MutableStateFlow<EventAttachment?>(null)
    private val _openMessage = MutableStateFlow<AttachmentOpenMessage?>(null)

    val uiState: StateFlow<CalendarEventDetailUiState> =
        combine(
            calendarRepository.observeEvent(eventId),
            _openingAttachment,
            _openMessage,
        ) { result, opening, message ->
            result.fold(
                onSuccess = { event ->
                    CalendarEventDetailUiState(
                        isLoading = false,
                        isError = false,
                        event = event,
                        openingAttachment = opening,
                        openMessage = message,
                    )
                },
                onFailure = {
                    CalendarEventDetailUiState(
                        isLoading = false,
                        isError = true,
                        openingAttachment = opening,
                        openMessage = message,
                    )
                },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CalendarEventDetailUiState(),
        )

    /**
     * Downloads [attachment] into the cache and reports the file back through
     * [onDownloaded]; the screen performs the ACTION_VIEW because only it
     * owns the window context. Failures surface as [AttachmentOpenMessage].
     */
    fun openAttachment(
        attachment: EventAttachment,
        cacheDirectory: File,
        onDownloaded: (File) -> Unit,
    ) {
        if (_openingAttachment.value != null) return
        _openMessage.value = null
        _openingAttachment.value = attachment
        viewModelScope.launch {
            val file = calendarRepository
                .downloadAttachment(attachment, cacheDirectory)
                .getOrNull()
            _openingAttachment.value = null
            if (file != null) {
                onDownloaded(file)
            } else {
                _openMessage.value = AttachmentOpenMessage.DOWNLOAD_FAILED
            }
        }
    }

    /** Called by the screen after ACTION_VIEW fails to find a viewer app. */
    fun onNoViewerApp() {
        _openMessage.value = AttachmentOpenMessage.NO_VIEWER
    }

    fun consumeOpenMessage() {
        _openMessage.value = null
    }
}
