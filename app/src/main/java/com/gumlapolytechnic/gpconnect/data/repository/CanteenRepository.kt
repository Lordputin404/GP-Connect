package com.gumlapolytechnic.gpconnect.data.repository

import com.gumlapolytechnic.gpconnect.data.model.CanteenCategory
import com.gumlapolytechnic.gpconnect.data.model.CanteenMenuItem
import kotlinx.coroutines.flow.Flow

/**
 * Query for the canteen menu list.
 *
 * `membersMenuOnly` shapes the Firestore query itself: members read
 * enabled categories + available items only (server-side constraints the
 * rules require), while the CANTEEN_ADMIN/SUPER_ADMIN read everything,
 * including disabled categories and unavailable items.
 */
data class CanteenQuery(
    val membersMenuOnly: Boolean = true,
)

/** Input for creating a category. The repository stamps timestamps. */
data class CanteenCategoryDraft(
    val name: String,
    val displayOrder: Int,
    val enabled: Boolean,
)

/** Input for creating a menu item. The repository stamps timestamps. */
data class CanteenMenuItemDraft(
    val name: String,
    val categoryId: String,
    val price: Int,
    val description: String,
    val isAvailable: Boolean,
    val displayOrder: Int,
)

/**
 * Canteen catalog contract. The canteen is college-wide — one shared
 * catalog, no department scoping anywhere.
 *
 * Authority, enforced by firestore.rules:
 *  - Reads: members (STUDENT/TEACHER) see enabled categories and available
 *    menu items only. CANTEEN_ADMIN and SUPER_ADMIN see everything.
 *  - Writes: CANTEEN_ADMIN and SUPER_ADMIN only. Members are read-only.
 *
 * Write methods return [Result] so a rules rejection surfaces in the admin
 * UI as an error banner instead of a crash.
 */
interface CanteenRepository {
    fun observeCategories(query: CanteenQuery = CanteenQuery()): Flow<Result<List<CanteenCategory>>>

    fun observeMenuItems(query: CanteenQuery = CanteenQuery()): Flow<Result<List<CanteenMenuItem>>>

    /** One-shot fetch of a single category document. */
    suspend fun getCategory(categoryId: String): CanteenCategory?

    /** One-shot fetch of a single menu item document. */
    suspend fun getMenuItem(itemId: String): CanteenMenuItem?

    suspend fun createCategory(draft: CanteenCategoryDraft): Result<Unit>

    /** Replaces the stored category wholesale; the ID identifies the target. */
    suspend fun updateCategory(category: CanteenCategory): Result<Unit>

    suspend fun deleteCategory(categoryId: String): Result<Unit>

    suspend fun createMenuItem(draft: CanteenMenuItemDraft): Result<Unit>

    /** Replaces the stored item wholesale; the ID identifies the target. */
    suspend fun updateMenuItem(item: CanteenMenuItem): Result<Unit>

    /** Availability quick toggle: only [isAvailable] changes. */
    suspend fun setItemAvailable(itemId: String, available: Boolean): Result<Unit>

    /** Category enable/disable quick toggle: only [enabled] changes. */
    suspend fun setCategoryEnabled(categoryId: String, enabled: Boolean): Result<Unit>

    suspend fun deleteMenuItem(itemId: String): Result<Unit>
}

/**
 * Client-side ordering: categories by displayOrder then name; items by
 * displayOrder then name, grouped visually by the caller. Enabled/available
 * filtering happens server-side via the query constraints.
 */
internal fun List<CanteenCategory>.sortedForMenu(): List<CanteenCategory> =
    sortedWith(compareBy({ it.displayOrder }, { it.name.lowercase() }))

internal fun List<CanteenMenuItem>.sortedForMenu(): List<CanteenMenuItem> =
    sortedWith(compareBy({ it.displayOrder }, { it.name.lowercase() }))
