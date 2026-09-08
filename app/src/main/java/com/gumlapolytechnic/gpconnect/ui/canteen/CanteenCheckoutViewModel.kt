package com.gumlapolytechnic.gpconnect.ui.canteen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gumlapolytechnic.gpconnect.data.model.CheckoutRequest
import com.gumlapolytechnic.gpconnect.data.model.OrderItemRequest
import com.gumlapolytechnic.gpconnect.data.repository.OrderRepository
import com.gumlapolytechnic.gpconnect.ui.login.CartState
import com.gumlapolytechnic.gpconnect.ui.login.SessionViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * ViewModel for the canteen checkout screen.
 *
 * Handles:
 * - Reading the current cart state from SessionViewModel
 * - Converting CartItems to CheckoutRequest
 * - Submitting the order via OrderRepository
 * - Handling idempotency key for retries
 * - Managing submitting/success/error states
 */
class CanteenCheckoutViewModel(
    private val sessionViewModel: SessionViewModel,
    private val orderRepository: OrderRepository,
) : ViewModel() {

    data class CheckoutUiState(
        val isSubmitting: Boolean = false,
        val errorMessage: String? = null,
        val orderPlaced: Boolean = false,
        val cartIsEmpty: Boolean = true,
        val orderId: String? = null,
    )

    private val _checkoutRequest = MutableStateFlow<CheckoutRequest?>(null)
    private val _checkoutAttemptId = MutableStateFlow<Int>(0)

    // Internal mutable state
    private val _isSubmitting = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _orderPlaced = MutableStateFlow(false)
    private val _orderId = MutableStateFlow<String?>(null)
    private val _cartIsEmpty = MutableStateFlow(true)

    // Single source of truth for UI state
    val uiState: StateFlow<CheckoutUiState> = combine(
        _cartIsEmpty,
        _isSubmitting,
        _errorMessage,
        _orderPlaced,
        _orderId,
    ) { cartIsEmpty, isSubmitting, errorMessage, orderPlaced, orderId ->
        CheckoutUiState(
            isSubmitting = isSubmitting,
            errorMessage = errorMessage,
            orderPlaced = orderPlaced,
            cartIsEmpty = cartIsEmpty,
            orderId = orderId,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CheckoutUiState(),
    )

    fun placeOrder() {
        // Don't place duplicate orders
        if (_isSubmitting.value) return

        val cartState = sessionViewModel.cartState.value
        if (cartState.isEmpty) {
            _cartIsEmpty.value = true
            return
        }

        // Build the checkout request
        val items = cartState.items.map { cartItem ->
            OrderItemRequest(
                menuItemId = cartItem.menuItem.id,
                quantity = cartItem.quantity,
            )
        }

        // Create or reuse checkout request with idempotency key
        val currentRequest = _checkoutRequest.value
        val attemptId = _checkoutAttemptId.value
        val checkoutRequest = if (currentRequest != null && attemptId == 0) {
            // First attempt - use the existing key
            currentRequest
        } else if (currentRequest != null && attemptId > 0) {
            // Retry - preserve the same idempotency key
            CheckoutRequest.retry(currentRequest)
        } else {
            // Fresh checkout - generate new key via CheckoutRequest.create
            CheckoutRequest.create(items)
        }

        _checkoutRequest.value = checkoutRequest
        _checkoutAttemptId.value = attemptId + 1
        _orderId.value = null // Clear previous order ID for new attempt

        _isSubmitting.value = true
        _errorMessage.value = null
        _cartIsEmpty.value = false

        viewModelScope.launch {
            val result = orderRepository.submitOrder(checkoutRequest)
            _isSubmitting.value = false

            result.onSuccess { order ->
                // Success - clear cart and mark order placed
                sessionViewModel.clearCart()
                _orderPlaced.value = true
                _orderId.value = order.id // Store order ID for success screen
                _checkoutRequest.value = null // Reset for next checkout
                _checkoutAttemptId.value = 0
            }.onFailure { error ->
                // Failure - keep cart intact, show error, allow retry
                _errorMessage.value = userFriendlyErrorMessage(error)
            }
        }
    }

    fun retry() {
        if (_isSubmitting.value) return
        _errorMessage.value = null
        placeOrder()
    }

    private fun userFriendlyErrorMessage(e: Throwable): String {
        return when {
            e is SecurityException -> "Authentication required. Please sign in again."
            e is IllegalArgumentException -> "Invalid order. Please check your cart and try again."
            e is IllegalStateException -> "Order could not be placed. Please try again."
            e is IOException -> "Network error. Please check your connection and retry."
            e is SecurityException && e.message?.contains("Permission") == true -> "Permission denied. You may not have access to the canteen."
            else -> "Something went wrong. Please try again."
        }
    }

    // Synchronize _cartIsEmpty with session cart
    init {
        viewModelScope.launch {
            sessionViewModel.cartState.collect { cartState ->
                _cartIsEmpty.value = cartState.isEmpty
            }
        }
    }
}