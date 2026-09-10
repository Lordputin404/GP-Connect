package com.gumlapolytechnic.gpconnect.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gumlapolytechnic.gpconnect.data.model.CalendarEventStatus
import com.gumlapolytechnic.gpconnect.data.model.CalendarEventType
import com.gumlapolytechnic.gpconnect.data.repository.CalendarEventDraft
import com.gumlapolytechnic.gpconnect.data.repository.CalendarRepository
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
                    ),
                ).isSuccess
            } else {
                val existing = calendarRepository.getEvent(editEventId)
                if (existing != null) {
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
                        ),
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
