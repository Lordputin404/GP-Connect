package com.gumlapolytechnic.gpconnect.data.model

/**
 * Canteen menu item.
 *
 * Prices are stored as integer paise (1 INR = 100 paise) to avoid
 * floating-point precision issues. Use [priceInr] for display formatting.
 *
 * [isAvailable] controls whether the item can be ordered.
 * [displayOrder] defines the sort order within its category.
 */
data class CanteenMenuItem(
    val id: String,
    val categoryId: String,
    val name: String,
    val description: String? = null,
    val pricePaise: Long = 0L,
    val imageUrl: String? = null,
    val isAvailable: Boolean = true,
    val displayOrder: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    /** Price in INR for display purposes (₹). */
    val priceInr: Double
        get() = pricePaise / 100.0

    /** Formatted price string for UI (e.g., "₹30.00"). */
    fun formattedPrice(): String = String.format("₹%.2f", priceInr)
}