package com.gumlapolytechnic.gpconnect.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gumlapolytechnic.gpconnect.data.model.CalendarEvent
import com.gumlapolytechnic.gpconnect.data.repository.CalendarRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class CalendarEventDetailUiState(
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val event: CalendarEvent? = null,
)

/**
 * Calendar event detail state: streams the single event. The rules `get`
 * grants members published events and the SUPER_ADMIN everything, so an
 * unpublished event simply shows as "not found" for a member.
 */
class CalendarEventDetailViewModel(
    calendarRepository: CalendarRepository,
    eventId: String,
) : ViewModel() {

    val uiState: StateFlow<CalendarEventDetailUiState> =
        calendarRepository.observeEvent(eventId)
            .map { result ->
                result.fold(
                    onSuccess = { event ->
                        CalendarEventDetailUiState(isLoading = false, isError = false, event = event)
                    },
                    onFailure = {
                        CalendarEventDetailUiState(isLoading = false, isError = true)
                    },
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = CalendarEventDetailUiState(),
            )
}
