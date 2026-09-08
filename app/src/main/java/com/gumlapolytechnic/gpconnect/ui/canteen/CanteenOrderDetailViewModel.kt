package com.gumlapolytechnic.gpconnect.ui.canteen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gumlapolytechnic.gpconnect.data.model.CanteenOrder
import com.gumlapolytechnic.gpconnect.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the student order detail screen.
 *
 * Sources:
 * - OrderRepository.observeOrder(orderId) — realtime single order.
 *
 * Exposes cancellation for PENDING orders only.
 * Repository errors during cancel are surfaced in [uiState.cancellationError].
 */
class CanteenOrderDetailViewModel(
    private val orderRepository: OrderRepository,
    orderId: String,
) : ViewModel() {

    data class DetailUiState(
        val isLoading: Boolean = true,
        val isError: Boolean = false,
        val order: CanteenOrder? = null,
        val isCancelling: Boolean = false,
        val cancellationError: String? = null,
    )

    private val _cancelling = MutableStateFlow(false)
    private val _cancellationError = MutableStateFlow<String?>(null)

    private val orderFlow = orderRepository.observeOrder(orderId)
        .map { order ->
            DetailUiState(
                isLoading = false,
                isError = false,
                order = order,
                isCancelling = false,
                cancellationError = null,
            )
        }
        .catch { _ ->
            emit(DetailUiState(isLoading = false, isError = true))
        }

    val uiState: StateFlow<DetailUiState> = combine(
        orderFlow,
        _cancelling,
        _cancellationError,
    ) { orderState, cancelling, cancelError ->
        orderState.copy(
            isCancelling = cancelling,
            cancellationError = cancelError,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DetailUiState(),
    )

    fun cancelOrder(reason: String?) {
        val order = uiState.value.order
        if (order == null || !order.status.isActive || !order.isPending || _cancelling.value) {
            return
        }

        _cancelling.value = true
        _cancellationError.value = null

        viewModelScope.launch {
            val result = orderRepository.cancelOrder(order.id, reason)
            _cancelling.value = false

            result.onFailure { error ->
                _cancellationError.value = userFriendlyErrorMessage(error)
            }
        }
    }

    private fun userFriendlyErrorMessage(e: Throwable): String {
        return when {
            e is SecurityException -> "You don't have permission to cancel this order."
            e is IllegalStateException -> "Order could not be cancelled. It may no longer be pending."
            e is IllegalArgumentException -> "Invalid cancellation request."
            e is java.io.IOException -> "Network error. Please check your connection and retry."
            else -> "Could not cancel the order. Please try again."
        }
    }
}
