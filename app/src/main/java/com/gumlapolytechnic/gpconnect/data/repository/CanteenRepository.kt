package com.gumlapolytechnic.gpconnect.data.repository

import com.gumlapolytechnic.gpconnect.data.model.CanteenCategory
import com.gumlapolytechnic.gpconnect.data.model.CanteenMenuItem
import kotlinx.coroutines.flow.Flow

/**
 * Canteen menu data contract (student reads + CANTEEN_ADMIN/SUPER_ADMIN CRUD).
 * Authorization is enforced by Firestore security rules — this interface
 * expresses domain operations, not permission checks.
 */
interface CanteenRepository {

    // --- Student / Member read operations ------------------------------------

    /** All enabled categories, ordered by [displayOrder]. */
    fun observeCategories(): Flow<List<CanteenCategory>>

    /** All available menu items, ordered by category then displayOrder. */
    fun observeAvailableMenuItems(): Flow<List<CanteenMenuItem>>

    /** Available menu items in a specific category. */
    fun observeAvailableMenuItemsByCategory(categoryId: String): Flow<List<CanteenMenuItem>>

    // --- Canteen Admin / Super Admin write operations ------------------------

    /**
     * Create a new category.
     * [createdAt] should be current epoch milliseconds.
     */
    suspend fun createCategory(
        name: String,
        description: String?,
        displayOrder: Int,
        enabled: Boolean,
        createdAt: Long
    ): CanteenCategory

    /**
     * Update mutable category fields. [createdAt] is immutable and not included.
     */
    suspend fun updateCategory(
        categoryId: String,
        name: String?,
        description: String?,
        displayOrder: Int?,
        enabled: Boolean?,
        updatedAt: Long
    )

    /**
     * Toggle category enabled state. Convenience for enable/disable.
     */
    suspend fun setCategoryEnabled(categoryId: String, enabled: Boolean)

    /**
     * Create a new menu item.
     * [createdAt] should be current epoch milliseconds.
     */
    suspend fun createMenuItem(
        categoryId: String,
        name: String,
        description: String?,
        pricePaise: Long,
        imageUrl: String?,
        isAvailable: Boolean,
        displayOrder: Int,
        createdAt: Long
    ): CanteenMenuItem

    /**
     * Update mutable menu item fields. [createdAt] and [categoryId] are immutable.
     */
    suspend fun updateMenuItem(
        itemId: String,
        name: String?,
        description: String?,
        pricePaise: Long?,
        imageUrl: String?,
        isAvailable: Boolean?,
        displayOrder: Int?,
        updatedAt: Long
    )

    /**
     * Toggle menu item availability. Convenience for stock management.
     */
    suspend fun setMenuItemAvailable(itemId: String, isAvailable: Boolean)
}