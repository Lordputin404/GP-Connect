package com.gumlapolytechnic.gpconnect.data.firebase

import android.util.Log
import com.gumlapolytechnic.gpconnect.data.model.CanteenCategory
import com.gumlapolytechnic.gpconnect.data.model.CanteenMenuItem
import com.gumlapolytechnic.gpconnect.data.repository.CanteenCategoryDraft
import com.gumlapolytechnic.gpconnect.data.repository.CanteenMenuItemDraft
import com.gumlapolytechnic.gpconnect.data.repository.CanteenQuery
import com.gumlapolytechnic.gpconnect.data.repository.CanteenRepository
import com.gumlapolytechnic.gpconnect.data.repository.sortedCategoriesForMenu
import com.gumlapolytechnic.gpconnect.data.repository.sortedItemsForMenu
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Firestore-backed canteen catalog, mirroring the library/calendar
 * repositories: a single collection listener per query with client-side
 * ordering. Member reads carry the `enabled == true` / `isAvailable == true`
 * constraints server-side — the rules reject any member query without them
 * — while the CANTEEN_ADMIN/SUPER_ADMIN listen to the whole collections.
 * Writes return [Result] so a rules rejection shows as an admin error
 * state rather than a crash.
 */
class FirebaseCanteenRepository : CanteenRepository {

    private val firestore get() = FirebaseServices.firestore

    override fun observeCategories(query: CanteenQuery): Flow<Result<List<CanteenCategory>>> =
        callbackFlow {
            val collection = if (query.membersMenuOnly) {
                firestore.collection(CANTEEN_CATEGORIES).whereEqualTo("enabled", true)
            } else {
                firestore.collection(CANTEEN_CATEGORIES)
            }
            val registration: ListenerRegistration =
                collection.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(
                            TAG,
                            "observeCategories failed: code=${error.code} message=${error.message}",
                            error,
                        )
                        trySend(Result.failure(error))
                        return@addSnapshotListener
                    }
                    val categories = snapshot?.documents
                        ?.mapNotNull { it.toCanteenCategory() }
                        .orEmpty()
                        .sortedCategoriesForMenu()
                    trySend(Result.success(categories))
                }
            awaitClose { registration.remove() }
        }

    override fun observeMenuItems(query: CanteenQuery): Flow<Result<List<CanteenMenuItem>>> =
        callbackFlow {
            val collection = if (query.membersMenuOnly) {
                firestore.collection(CANTEEN_MENU_ITEMS).whereEqualTo("isAvailable", true)
            } else {
                firestore.collection(CANTEEN_MENU_ITEMS)
            }
            val registration: ListenerRegistration =
                collection.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(
                            TAG,
                            "observeMenuItems failed: code=${error.code} message=${error.message}",
                            error,
                        )
                        trySend(Result.failure(error))
                        return@addSnapshotListener
                    }
                    val items = snapshot?.documents
                        ?.mapNotNull { it.toCanteenMenuItem() }
                        .orEmpty()
                        .sortedItemsForMenu()
                    trySend(Result.success(items))
                }
            awaitClose { registration.remove() }
        }

    override suspend fun getCategory(categoryId: String): CanteenCategory? =
        firestore.collection(CANTEEN_CATEGORIES).document(categoryId).get().awaitTask()
            ?.toCanteenCategory()

    override suspend fun getMenuItem(itemId: String): CanteenMenuItem? =
        firestore.collection(CANTEEN_MENU_ITEMS).document(itemId).get().awaitTask()
            ?.toCanteenMenuItem()

    override suspend fun createCategory(draft: CanteenCategoryDraft): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        firestore.collection(CANTEEN_CATEGORIES)
            .add(
                canteenCategoryFields(
                    name = draft.name,
                    displayOrder = draft.displayOrder,
                    enabled = draft.enabled,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            .awaitTask()
        Log.i(TAG, "Created canteen category '${draft.name}'")
    }.onFailure { failure ->
        Log.e(TAG, "createCategory failed: ${failure.message}", failure)
    }.map { }

    override suspend fun updateCategory(category: CanteenCategory): Result<Unit> = runCatching {
        firestore.collection(CANTEEN_CATEGORIES).document(category.id)
            .set(
                canteenCategoryFields(
                    name = category.name,
                    displayOrder = category.displayOrder,
                    enabled = category.enabled,
                    createdAt = category.createdAt,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            .awaitTask()
        Log.i(TAG, "Updated canteen category '${category.name}' (${category.id})")
    }.onFailure { failure ->
        Log.e(TAG, "updateCategory(${category.id}) failed: ${failure.message}", failure)
    }.map { }

    override suspend fun deleteCategory(categoryId: String): Result<Unit> = runCatching {
        firestore.collection(CANTEEN_CATEGORIES).document(categoryId).delete().awaitTask()
        Log.i(TAG, "Deleted canteen category $categoryId")
    }.onFailure { failure ->
        Log.e(TAG, "deleteCategory($categoryId) failed: ${failure.message}", failure)
    }.map { }

    override suspend fun createMenuItem(draft: CanteenMenuItemDraft): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        firestore.collection(CANTEEN_MENU_ITEMS)
            .add(
                canteenMenuItemFields(
                    name = draft.name,
                    categoryId = draft.categoryId,
                    price = draft.price,
                    description = draft.description,
                    isAvailable = draft.isAvailable,
                    displayOrder = draft.displayOrder,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            .awaitTask()
        Log.i(TAG, "Created canteen menu item '${draft.name}'")
    }.onFailure { failure ->
        Log.e(TAG, "createMenuItem failed: ${failure.message}", failure)
    }.map { }

    override suspend fun updateMenuItem(item: CanteenMenuItem): Result<Unit> = runCatching {
        firestore.collection(CANTEEN_MENU_ITEMS).document(item.id)
            .set(
                canteenMenuItemFields(
                    name = item.name,
                    categoryId = item.categoryId,
                    price = item.price,
                    description = item.description,
                    isAvailable = item.isAvailable,
                    displayOrder = item.displayOrder,
                    createdAt = item.createdAt,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            .awaitTask()
        Log.i(TAG, "Updated canteen menu item '${item.name}' (${item.id})")
    }.onFailure { failure ->
        Log.e(TAG, "updateMenuItem(${item.id}) failed: ${failure.message}", failure)
    }.map { }

    override suspend fun setItemAvailable(itemId: String, available: Boolean): Result<Unit> =
        runCatching {
            firestore.collection(CANTEEN_MENU_ITEMS).document(itemId)
                .update("isAvailable", available, "updatedAt", System.currentTimeMillis())
                .awaitTask()
            Log.i(TAG, "Set isAvailable=$available on canteen menu item $itemId")
        }.onFailure { failure ->
            Log.e(TAG, "setItemAvailable($itemId) failed: ${failure.message}", failure)
        }.map { }

    override suspend fun setCategoryEnabled(categoryId: String, enabled: Boolean): Result<Unit> =
        runCatching {
            firestore.collection(CANTEEN_CATEGORIES).document(categoryId)
                .update("enabled", enabled, "updatedAt", System.currentTimeMillis())
                .awaitTask()
            Log.i(TAG, "Set enabled=$enabled on canteen category $categoryId")
        }.onFailure { failure ->
            Log.e(TAG, "setCategoryEnabled($categoryId) failed: ${failure.message}", failure)
        }.map { }

    override suspend fun deleteMenuItem(itemId: String): Result<Unit> = runCatching {
        firestore.collection(CANTEEN_MENU_ITEMS).document(itemId).delete().awaitTask()
        Log.i(TAG, "Deleted canteen menu item $itemId")
    }.onFailure { failure ->
        Log.e(TAG, "deleteMenuItem($itemId) failed: ${failure.message}", failure)
    }.map { }

    private companion object {
        const val TAG = "GPFirebaseCanteen"
        const val CANTEEN_CATEGORIES = "canteenCategories"
        const val CANTEEN_MENU_ITEMS = "canteenMenuItems"
    }
}

/**
 * All fields written to one canteen category document. Timestamps are
 * epoch-millisecond longs, matching the Notice/Calendar/Library pattern.
 */
internal fun canteenCategoryFields(
    name: String,
    displayOrder: Int,
    enabled: Boolean,
    createdAt: Long,
    updatedAt: Long,
): Map<String, Any?> = mapOf(
    "name" to name.trim(),
    "displayOrder" to displayOrder,
    "enabled" to enabled,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt,
)

/**
 * All fields written to one canteen menu item document. `categoryId`
 * references a canteenCategories/{id} document; `price` is whole rupees.
 */
internal fun canteenMenuItemFields(
    name: String,
    categoryId: String,
    price: Int,
    description: String,
    isAvailable: Boolean,
    displayOrder: Int,
    createdAt: Long,
    updatedAt: Long,
): Map<String, Any?> = mapOf(
    "name" to name.trim(),
    "categoryId" to categoryId,
    "price" to price,
    "description" to description.trim(),
    "isAvailable" to isAvailable,
    "displayOrder" to displayOrder,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt,
)

/** Defensive parse of `canteenCategories/{categoryId}`. */
internal fun com.google.firebase.firestore.DocumentSnapshot.toCanteenCategory(): CanteenCategory? {
    if (!exists()) return null
    return CanteenCategory(
        id = id,
        name = getString("name").orEmpty(),
        displayOrder = getLong("displayOrder")?.toInt() ?: 0,
        enabled = getBoolean("enabled") ?: true,
        createdAt = getLong("createdAt") ?: 0L,
        updatedAt = getLong("updatedAt") ?: 0L,
    )
}

/** Defensive parse of `canteenMenuItems/{itemId}`. */
internal fun com.google.firebase.firestore.DocumentSnapshot.toCanteenMenuItem(): CanteenMenuItem? {
    if (!exists()) return null
    return CanteenMenuItem(
        id = id,
        name = getString("name").orEmpty(),
        categoryId = getString("categoryId").orEmpty(),
        price = getLong("price")?.toInt() ?: 0,
        description = getString("description").orEmpty(),
        isAvailable = getBoolean("isAvailable") ?: true,
        displayOrder = getLong("displayOrder")?.toInt() ?: 0,
        createdAt = getLong("createdAt") ?: 0L,
        updatedAt = getLong("updatedAt") ?: 0L,
    )
}
