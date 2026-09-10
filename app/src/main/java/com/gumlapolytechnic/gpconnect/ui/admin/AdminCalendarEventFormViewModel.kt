package com.gumlapolytechnic.gpconnect.ui.admin

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gumlapolytechnic.gpconnect.data.model.CalendarEventStatus
import com.gumlapolytechnic.gpconnect.data.model.CalendarEventType
import com.gumlapolytechnic.gpconnect.data.model.EventAttachment
import com.gumlapolytechnic.gpconnect.data.model.EventAttachmentType
import com.gumlapolytechnic.gpconnect.data.repository.CalendarEventDraft
import com.gumlapolytechnic.gpconnect.data.repository.CalendarRepository
import com.gumlapolytechnic.gpconnect.data.repository.PendingAttachment
import com.gumlapolytechnic.gpconnect.util.Dates
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CalendarEventFormUiState(
    val isLoading: Boolean = false,
    val notFound: Boolean = false,
    val isEditMode: Boolean = false,
    val title: String = "",
    val description: String = "",
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long = System.currentTimeMillis(),
    val type: CalendarEventType = CalendarEventType.ACTIVITY,
    val isAllDay: Boolean = true,
    val status: CalendarEventStatus = CalendarEventStatus.CONFIRMED,
    val isPublished: Boolean = false,
    /** Attachments already stored with the event (edit mode). */
    val existingAttachments: List<EventAttachment> = emptyList(),
    /** Files picked this session, uploaded when the event is saved. */
    val pendingAttachments: List<PendingAttachment> = emptyList(),
    /** True when an unsupported file was picked; cleared on the next pick. */
    val unsupportedAttachment: Boolean = false,
    val titleError: Boolean = false,
    val dateError: Boolean = false,
    val isSaving: Boolean = false,
    val saveError: Boolean = false,
    val saved: Boolean = false,
)

/**
 * Calendar event create/edit form state (the AdminNoticeFormViewModel pattern).
 * Create mode files a [CalendarEventDraft]; edit mode preloads by ID,
 * preserves id/createdAt, and updates in place. The Firestore rules allow
 * only the SUPER_ADMIN to write calendarEvents — a rejected save surfaces as
 * saveError, never a crash.
 *
 * Attachments: picked files are held locally and uploaded to Storage only
 * when the event is saved (create: after the event ID is allocated; edit:
 * before the document update). Removing an already-stored attachment marks
 * it for Storage deletion in the same save.
 */
class AdminCalendarEventFormViewModel(
    private val calendarRepository: CalendarRepository,
    private val editEventId: String?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CalendarEventFormUiState(
            isLoading = editEventId != null,
            isEditMode = editEventId != null,
        ),
    )
    val uiState: StateFlow<CalendarEventFormUiState> = _uiState.asStateFlow()

    init {
        val id = editEventId
        if (id != null) {
            viewModelScope.launch {
                val event = calendarRepository.getEvent(id)
                _uiState.update { state ->
                    if (event == null) {
                        state.copy(isLoading = false, notFound = true)
                    } else {
                        state.copy(
                            isLoading = false,
                            title = event.title,
                            description = event.description,
                            startDate = event.startDate,
                            endDate = event.endDate,
                            type = event.type,
                            isAllDay = event.isAllDay,
                            status = event.status,
                            isPublished = event.isPublished,
                            existingAttachments = event.attachments,
                        )
                    }
                }
            }
        }
    }

    fun onTitleChange(value: String) {
        _uiState.update { it.copy(title = value, titleError = false) }
    }

    fun onDescriptionChange(value: String) {
        _uiState.update { it.copy(description = value) }
    }

    fun onStartDateChange(timestampMs: Long) {
        _uiState.update { state ->
            // Keep the range valid: pulling the start past the end drags the
            // end along (mirrored in onEndDateChange).
            val end = if (timestampMs > state.endDate) timestampMs else state.endDate
            state.copy(startDate = timestampMs, endDate = end, dateError = false)
        }
    }

    fun onEndDateChange(timestampMs: Long) {
        _uiState.update { state ->
            val start = if (timestampMs < state.startDate) timestampMs else state.startDate
            state.copy(startDate = start, endDate = timestampMs, dateError = false)
        }
    }

    fun onTypeChange(value: CalendarEventType) {
        _uiState.update { it.copy(type = value) }
    }

    fun onAllDayChange(value: Boolean) {
        _uiState.update { it.copy(isAllDay = value) }
    }

    fun onStatusChange(value: CalendarEventStatus) {
        _uiState.update { it.copy(status = value) }
    }

    fun onPublishedChange(value: Boolean) {
        _uiState.update { it.copy(isPublished = value) }
    }

    /**
     * Adds a picked file to the pending set. Only PDF/DOC/DOCX/JPG/JPEG/PNG/
     * WebP are accepted; an unsupported pick sets [CalendarEventFormUiState.unsupportedAttachment]
     * so the form can show a message instead of failing at save time.
     */
    fun onAttachmentPicked(uri: Uri, name: String, size: Long) {
        if (EventAttachmentType.fromFileName(name) == null) {
            _uiState.update { it.copy(unsupportedAttachment = true) }
            return
        }
        _uiState.update { state ->
            state.copy(
                pendingAttachments = state.pendingAttachments + PendingAttachment(uri, name, size),
                unsupportedAttachment = false,
            )
        }
    }

    /** Drops a not-yet-uploaded pick; nothing in Storage to clean up. */
    fun removePendingAttachment(pending: PendingAttachment) {
        _uiState.update { state ->
            state.copy(pendingAttachments = state.pendingAttachments - pending)
        }
    }

    /**
     * Marks an already-stored attachment for deletion. The Firestore metadata
     * disappears when the save succeeds and the Storage file is deleted with
     * it; the entry is dropped from the form immediately.
     */
    fun removeExistingAttachment(attachment: EventAttachment) {
        _uiState.update { state ->
            state.copy(existingAttachments = state.existingAttachments - attachment)
        }
    }

    fun save() {
        val state = _uiState.value
        val titleError = state.title.isBlank()
        val dateError = state.endDate < Dates.startOfDay(state.startDate)
        if (titleError || dateError) {
            _uiState.update { it.copy(titleError = titleError, dateError = dateError) }
            return
        }

        _uiState.update { it.copy(isSaving = true, titleError = false, dateError = false, saveError = false) }
        viewModelScope.launch {
            val succeeded = if (editEventId == null) {
                calendarRepository.createEvent(
                    CalendarEventDraft(
                        title = state.title.trim(),
                        description = state.description.trim(),
                        startDate = state.startDate,
                        endDate = state.endDate,
                        type = state.type,
                        isAllDay = state.isAllDay,
                        status = state.status,
                        isPublished = state.isPublished,
                        pendingUploads = state.pendingAttachments,
                    ),
                ).isSuccess
            } else {
                val existing = calendarRepository.getEvent(editEventId)
                if (existing != null) {
                    val kept = existing.attachments.filter { it in state.existingAttachments }
                    val removed = existing.attachments.filter { it !in state.existingAttachments }
                    calendarRepository.updateEvent(
                        existing.copy(
                            title = state.title.trim(),
                            description = state.description.trim(),
                            startDate = state.startDate,
                            endDate = state.endDate,
                            type = state.type,
                            isAllDay = state.isAllDay,
                            status = state.status,
                            isPublished = state.isPublished,
                            attachments = kept,
                        ),
                        pendingUploads = state.pendingAttachments,
                        removedAttachments = removed,
                    ).isSuccess
                } else {
                    false
                }
            }
            _uiState.update {
                if (succeeded) {
                    it.copy(isSaving = false, saved = true)
                } else {
                    it.copy(isSaving = false, saveError = true)
                }
            }
        }
    }
}
