package com.gumlapolytechnic.gpconnect.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gumlapolytechnic.gpconnect.data.model.CalendarEvent
import com.gumlapolytechnic.gpconnect.data.repository.CalendarQuery
import com.gumlapolytechnic.gpconnect.data.repository.CalendarRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

data class AdminCalendarUiState(
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val events: List<CalendarEvent> = emptyList(),
    val busyIds: Set<String> = emptySet(),
    val actionFailed: Boolean = false,
)

/**
 * College Calendar management state for the SUPER_ADMIN: the full (published
 * and unpublished) event list plus delete/publish mutations. Writes go
 * through the repository and return [Result]; the Firestore rules reject any
 * non-super-admin caller — this UI simply surfaces that as an error banner.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AdminCalendarViewModel(
    private val calendarRepository: CalendarRepository,
) : ViewModel() {

    private val refresh = MutableStateFlow(0)
    private val busyIds = MutableStateFlow<Set<String>>(emptySet())
    private val actionFailed = MutableStateFlow(false)

    private val events = refresh.flatMapLatest {
        calendarRepository.observeEvents(CalendarQuery(publishedOnly = false))
    }

    val uiState: StateFlow<AdminCalendarUiState> =
        combine(events, busyIds, actionFailed) { result, busy, failed ->
            result.fold(
                onSuccess = { list ->
                    AdminCalendarUiState(
                        isLoading = false,
                        events = list,
                        busyIds = busy,
                        actionFailed = failed,
                    )
                },
                onFailure = {
                    AdminCalendarUiState(isLoading = false, isError = true)
                },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AdminCalendarUiState(),
        )

    fun deleteEvent(event: CalendarEvent) = act(event.id) {
        calendarRepository.deleteEvent(event.id)
    }

    fun setPublished(event: CalendarEvent, published: Boolean) = act(event.id) {
        calendarRepository.setPublished(event.id, published)
    }

    fun dismissActionError() {
        actionFailed.value = false
    }

    fun retry() {
        refresh.value += 1
    }

    private fun act(id: String, action: suspend () -> Result<Unit>) {
        if (id in busyIds.value) return
        busyIds.update { it + id }
        actionFailed.value = false
        viewModelScope.launch {
            val result = action()
            busyIds.update { it - id }
            if (result.isFailure) actionFailed.value = true
        }
    }
}
