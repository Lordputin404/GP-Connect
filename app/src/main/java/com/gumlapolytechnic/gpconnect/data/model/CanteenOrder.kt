package com.gumlapolytechnic.gpconnect.data.model

/**
 * Persisted canteen order stored at `canteenOrders/{orderId}`.
 *
 * [customerId] links the order to the student's Firebase Auth UID (ownership authority).
 * [items] stores immutable [OrderItemSnapshot] records to preserve historical pricing.
 * [totalAmountPaise] represents the authoritative total in integer paise.
 * [status] reflects the current [OrderStatus] in the deterministic state machine.
 */
data class CanteenOrder(
    val id: String,
    val customerId: String,
    val customerName: String,
    val customerEmail: String,
    val items: List<OrderItemSnapshot>,
    val totalAmountPaise: Long,
    val status: OrderStatus = OrderStatus.PENDING,
    val createdAt: Long = 0L,
    val updatedAt: Long = createdAt,
    val decidedBy: String? = null,
    val cancellationReason: String? = null,
) {
    /** Total amount in INR for display. */
    val totalAmountInr: Double
        get() = totalAmountPaise / 100.0

    fun formattedTotal(): String = String.format("₹%.2f", totalAmountInr)

    val isPending: Boolean get() = status == OrderStatus.PENDING
    val isActive: Boolean get() = status.isActive
    val isTerminal: Boolean get() = status.isTerminal
}