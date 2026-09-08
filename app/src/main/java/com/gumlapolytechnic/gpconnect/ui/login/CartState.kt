package com.gumlapolytechnic.gpconnect.ui.login

import com.gumlapolytechnic.gpconnect.data.model.CanteenMenuItem
import com.gumlapolytechnic.gpconnect.data.model.CartItem

/** Maximum quantity per cart line, aligned with the trusted backend limit. */
const val CART_MAX_QUANTITY_PER_ITEM: Int = 99

/**
 * Local-only transient cart state for the student canteen.
 *
 * This state is never persisted, never written to Firestore, and is reset on
 * logout. Cart totals are display-only; the trusted checkout backend
 * revalidates every menu item and computes the authoritative total.
 */
data class CartState(
    val items: List<CartItem> = emptyList(),
) {
    /** True when there are no items in the cart. */
    val isEmpty: Boolean get() = items.isEmpty()

    /** Total quantity across all cart lines. */
    val totalQuantity: Int get() = items.sumOf { it.quantity }

    /** Distinct menu items in the cart. */
    val itemCount: Int get() = items.size

    /** Display-only subtotal in paise. */
    val subtotalPaise: Long
        get() = items.sumOf { it.subtotalPaise }

    /** Adds [menuItem] or increments its quantity if already present, capped at [CART_MAX_QUANTITY_PER_ITEM]. */
    fun addItem(menuItem: CanteenMenuItem): CartState {
        val existingIndex = items.indexOfFirst { it.menuItem.id == menuItem.id }
        return if (existingIndex >= 0) {
            val existing = items[existingIndex]
            val nextQuantity = (existing.quantity + 1).coerceAtMost(CART_MAX_QUANTITY_PER_ITEM)
            if (nextQuantity == existing.quantity) {
                this
            } else {
                copy(items = items.toMutableList().apply {
                    this[existingIndex] = existing.copy(quantity = nextQuantity)
                })
            }
        } else {
            copy(items = items + CartItem(menuItem = menuItem, quantity = 1))
        }
    }

    /** Increments the quantity of [menuItemId], capped at [CART_MAX_QUANTITY_PER_ITEM]. */
    fun increment(menuItemId: String): CartState = updateQuantity(menuItemId) { (it + 1).coerceAtMost(CART_MAX_QUANTITY_PER_ITEM) }

    /** Decrements the quantity of [menuItemId]; removes the line entirely if it would reach zero. */
    fun decrement(menuItemId: String): CartState = updateQuantity(menuItemId) { next -> if (next <= 1) 0 else next - 1 }

    /** Removes [menuItemId] from the cart completely. */
    fun remove(menuItemId: String): CartState =
        if (items.any { it.menuItem.id == menuItemId }) {
            copy(items = items.filterNot { it.menuItem.id == menuItemId })
        } else {
            this
        }

    /** Returns the line for [menuItemId] or null when not present. */
    fun findItem(menuItemId: String): CartItem? = items.firstOrNull { it.menuItem.id == menuItemId }

    /** Quantity of [menuItemId] in the cart, or 0 when not present. */
    fun quantityOf(menuItemId: String): Int = findItem(menuItemId)?.quantity ?: 0

    private fun updateQuantity(menuItemId: String, transform: (Int) -> Int): CartState {
        val index = items.indexOfFirst { it.menuItem.id == menuItemId }
        if (index < 0) return this
        val existing = items[index]
        val next = transform(existing.quantity)
        val updated = items.toMutableList()
        return if (next <= 0) {
            updated.removeAt(index)
            copy(items = updated)
        } else if (next == existing.quantity) {
            this
        } else {
            updated[index] = existing.copy(quantity = next)
            copy(items = updated)
        }
    }
}
