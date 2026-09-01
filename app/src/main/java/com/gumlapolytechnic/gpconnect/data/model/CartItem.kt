package com.gumlapolytechnic.gpconnect.data.model

/**
 * Local-only cart item representing a selected menu item and its quantity.
 *
 * This is strictly a client-side transient UI model. It is never stored directly
 * as a Firestore document. During checkout, cart items are validated against
 * trusted server menu data.
 */
data class CartItem(
    val menuItem: CanteenMenuItem,
    val quantity: Int = 1,
) {
    init {
        require(quantity > 0) { "Cart item quantity must be positive, got $quantity" }
    }

    /** Subtotal in paise (`menuItem.pricePaise * quantity`). */
    val subtotalPaise: Long
        get() = menuItem.pricePaise * quantity

    /** Subtotal in INR for display. */
    val subtotalInr: Double
        get() = subtotalPaise / 100.0

    fun formattedSubtotal(): String = String.format("₹%.2f", subtotalInr)
}