package com.gumlapolytechnic.gpconnect.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gumlapolytechnic.gpconnect.data.model.CalendarEvent
import com.gumlapolytechnic.gpconnect.data.model.CalendarEventType
import com.gumlapolytechnic.gpconnect.data.repository.CalendarQuery
import com.gumlapolytechnic.gpconnect.data.repository.CalendarRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class CalendarUiState(
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val type: CalendarEventType? = null,
    val events: List<CalendarEvent> = emptyList(),
)

/**
 * College Calendar list state: a type filter chip flow over the repository's
 * published-event stream (the NoticesViewModel pattern). Members read
 * published events only — enforced by the Firestore rules, not just this
 * query. Result-wrapped so a rules rejection shows as an error state rather
 * than an empty calendar.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(private val calendarRepository: CalendarRepository) : ViewModel() {

    private val type = MutableStateFlow<CalendarEventType?>(null)
    private val refresh = MutableStateFlow(0)

    val uiState: StateFlow<CalendarUiState> =
        combine(type, refresh) { filter, _ -> filter }
            .flatMapLatest { filter ->
                calendarRepository.observeEvents(
                    CalendarQuery(type = filter, publishedOnly = true),
                )
            }
            .combine(type) { result, filter ->
                result.fold(
                    onSuccess = { events ->
                        CalendarUiState(
                            isLoading = false,
                            isError = false,
                            type = filter,
                            events = events,
                        )
                    },
                    onFailure = {
                        CalendarUiState(isLoading = false, isError = true, type = filter)
                    },
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = CalendarUiState(),
            )

    fun onTypeChange(value: CalendarEventType?) {
        type.value = value
    }

    fun retry() {
        refresh.value += 1
    }
}
