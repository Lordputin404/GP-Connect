package com.gumlapolytechnic.gpconnect.data.model

/**
 * One canteen menu category, stored at `canteenCategories/{categoryId}`.
 *
 * Categories are real Firestore data managed by the CANTEEN_ADMIN — the
 * app never hardcodes a category list. [displayOrder] lets the admin keep
 * a stable serving order (Food before Snacks before Drinks, say); a
 * disabled category is hidden from members but its items keep their
 * documents so re-enabling loses nothing.
 *
 * The canteen is college-wide: no department field exists by design.
 */
data class CanteenCategory(
    val id: String = "",
    val name: String = "",
    val displayOrder: Int = 0,
    val enabled: Boolean = true,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

/**
 * One canteen menu item, stored at `canteenMenuItems/{itemId}`.
 *
 * [categoryId] references a [CanteenCategory] document id. [isAvailable]
 * is the admin's availability toggle (sold out for today, say) — the
 * student UI shows Unavailable and refuses to add such an item to the
 * cart. [price] is in whole rupees.
 *
 * Catalog data only: no orders, no inventory, no department scoping —
 * those belong to later phases.
 */
data class CanteenMenuItem(
    val id: String = "",
    val name: String = "",
    val categoryId: String = "",
    val price: Int = 0,
    val description: String = "",
    val isAvailable: Boolean = true,
    val displayOrder: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

/**
 * One line of the in-memory cart: the snapshotted item plus a quantity.
 * The cart is local/session state only — never written to Firestore — so
 * it clears naturally when the process or session is recreated.
 */
data class CartLine(
    val item: CanteenMenuItem,
    val quantity: Int,
) {
    /** price × quantity for this line. */
    val lineTotal: Int get() = item.price * quantity
}
