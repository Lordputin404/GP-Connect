package com.gumlapolytechnic.gpconnect.ui.canteen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gumlapolytechnic.gpconnect.data.model.CanteenOrder
import com.gumlapolytechnic.gpconnect.data.model.OrderStatus
import com.gumlapolytechnic.gpconnect.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for the student order history screen.
 *
 * Sources:
 * - OrderRepository.observeOrderHistory(currentUid) — realtime order list.
 *
 * UI state separates "active" (non-terminal) orders from terminal history so
 * the screen can highlight what the student should track.
 *
 * Active statuses: PENDING, CONFIRMED, PREPARING, READY.
 * Terminal statuses: COMPLETED, CANCELLED.
 *
 * Repository errors are surfaced via [isError] — we do not silently collapse
 * failures into an empty list.
 */
class CanteenOrderHistoryViewModel(
    private val orderRepository: OrderRepository,
    currentUid: String?,
) : ViewModel() {

    data class HistoryUiState(
        val isLoading: Boolean = true,
        val isError: Boolean = false,
        val activeOrders: List<CanteenOrder> = emptyList(),
        val historicalOrders: List<CanteenOrder> = emptyList(),
    )

    private val _isError = MutableStateFlow(false)

    val uiState: StateFlow<HistoryUiState> = if (currentUid == null) {
        flowOf(HistoryUiState(isLoading = false)).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryUiState(isLoading = false),
        )
    } else {
        val ordersFlow = orderRepository.observeOrderHistory(currentUid)
            .map { it.sortedByDescending { order -> order.createdAt } }
            .map { orders -> orders.partition { it.status.isActive } }
            .map { (active, historical) ->
                HistoryUiState(
                    isLoading = false,
                    isError = false,
                    activeOrders = active,
                    historicalOrders = historical,
                )
            }
            .onStart { _isError.value = false }
            .catch { _ ->
                _isError.value = true
                emit(HistoryUiState(isLoading = false, isError = true))
            }

        combine(ordersFlow, _isError) { state, errorFlag ->
            if (errorFlag) state.copy(isError = true) else state
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryUiState(),
        )
    }
}