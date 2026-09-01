package com.gumlapolytechnic.gpconnect.data.model

/**
 * Immutable historical snapshot of a single item inside a placed order.
 *
 * This representation is completely frozen at the time of checkout.
 * It copies the item name and unit price (in paise) so that any subsequent
 * menu edits (e.g. price hikes, description edits, or deletion) do not corrupt
 * or alter historical order summaries.
 */
data class OrderItemSnapshot(
    val menuItemId: String,
    val name: String,
    val pricePaise: Long,
    val quantity: Int,
) {
    init {
        require(quantity > 0) { "Ordered quantity must be positive, got $quantity" }
        require(pricePaise >= 0L) { "Item price cannot be negative, got $pricePaise" }
    }

    /** Subtotal of this item snapshot in paise. */
    val subtotalPaise: Long
        get() = pricePaise * quantity

    /** Subtotal in INR for display. */
    val subtotalInr: Double
        get() = subtotalPaise / 100.0

    fun formattedSubtotal(): String = String.format("₹%.2f", subtotalInr)
}