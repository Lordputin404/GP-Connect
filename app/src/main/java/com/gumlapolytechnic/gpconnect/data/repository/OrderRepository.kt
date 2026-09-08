package com.gumlapolytechnic.gpconnect.data.repository

import com.gumlapolytechnic.gpconnect.data.model.CanteenOrder
import com.gumlapolytechnic.gpconnect.data.model.CheckoutRequest
import com.gumlapolytechnic.gpconnect.data.model.OrderStatus
import kotlinx.coroutines.flow.Flow

/**
 * Canteen order data contract.
 *
 * The Android client submits a [CheckoutRequest] (item IDs + quantities + idempotency key).
 * Authoritative pricing, availability, and snapshot creation are performed by
 * the trusted checkout backend (Cloud Function). This repository does NOT expose
 * a generic "createOrder(CanteenOrder)" or "updateOrder(CanteenOrder)" to
 * prevent clients from treating authoritative fields as client-owned.
 */
interface OrderRepository {

    // --- Student operations ---------------------------------------------------

    /**
     * Submit an order request. The trusted backend will:
     * 1. Fetch authoritative menu items for each [OrderItemRequest.menuItemId]
     * 2. Verify availability and positive quantities
     * 3. Calculate subtotals and total from live prices
     * 4. Create immutable [OrderItemSnapshot] records
     * 5. Persist the [CanteenOrder] with status PENDING
     *
     * The [CheckoutRequest.idempotencyKey] MUST be the same across retries of the
     * same logical checkout operation to prevent duplicate orders.
     */
    suspend fun submitOrder(request: CheckoutRequest): Result<CanteenOrder>

    /**
     * The student's single active order (status in PENDING, CONFIRMED, PREPARING, READY),
     * or null if none.
     */
    fun observeActiveOrder(customerId: String): Flow<CanteenOrder?>

    /** All orders for the given customer, newest first. */
    fun observeOrderHistory(customerId: String): Flow<List<CanteenOrder>>

    /** Observe a specific order by ID. */
    fun observeOrder(orderId: String): Flow<CanteenOrder?>

    /**
     * Cancel the student's own order while it is still PENDING.
     * Firestore rules enforce ownership and status constraint.
     */
    suspend fun cancelOrder(orderId: String, reason: String?): Result<Unit>

    // --- Canteen Admin / Super Admin operations -------------------------------

    /** All active orders (queue), ordered by creation time ascending. */
    fun observeActiveOrderQueue(): Flow<Result<List<CanteenOrder>>>

    /**
     * Transition order status following the state machine.
     * Only status, updatedAt, decidedBy, and cancellationReason are modified.
     * Firestore rules validate the transition.
     */
    suspend fun updateOrderStatus(
        orderId: String,
        newStatus: OrderStatus,
        cancellationReason: String?
    ): Result<Unit>
}