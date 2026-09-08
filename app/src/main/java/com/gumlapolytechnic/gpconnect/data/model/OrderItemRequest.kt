package com.gumlapolytechnic.gpconnect.data.model

/**
 * Client intent submitted during checkout.
 *
 * Contains strictly the student's requested item IDs and desired quantities.
 * The client NEVER supplies prices or order totals — the authoritative order creation
 * flow looks up live menu items to determine prices, availability, and total amount.
 */
data class OrderItemRequest(
    val menuItemId: String,
    val quantity: Int,
) {
    init {
        require(quantity > 0) { "Requested quantity must be positive, got $quantity" }
    }
}