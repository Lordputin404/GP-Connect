package com.gumlapolytechnic.gpconnect.data.model

import java.util.UUID

/**
 * Complete checkout request submitted by the client.
 *
 * [items] - The list of menu items and quantities.
 * [idempotencyKey] - Optional UUID v4 generated ONCE per checkout session by the caller.
 *                    If omitted, the repository will generate one (for one-shot checkouts).
 *                    For retries, the SAME key MUST be reused to prevent duplicate orders.
 */
data class CheckoutRequest(
    val items: List<OrderItemRequest>,
    val idempotencyKey: String = UUID.randomUUID().toString(),
) {
    init {
        require(items.isNotEmpty()) { "Checkout requires at least one item" }
        require(idempotencyKey.isNotBlank()) { "idempotencyKey must be non-empty" }
    }

    companion object {
        /** Creates a new checkout request with a fresh idempotency key (for one-shot checkouts). */
        fun create(items: List<OrderItemRequest>): CheckoutRequest = CheckoutRequest(items)

        /** Creates a retry request reusing the same idempotency key. */
        fun retry(request: CheckoutRequest): CheckoutRequest = request.copy()
    }
}