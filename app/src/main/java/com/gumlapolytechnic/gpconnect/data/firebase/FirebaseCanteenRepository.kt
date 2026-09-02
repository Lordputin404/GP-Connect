package com.gumlapolytechnic.gpconnect.data.firebase

import android.util.Log
import com.gumlapolytechnic.gpconnect.data.model.CanteenCategory
import com.gumlapolytechnic.gpconnect.data.model.CanteenMenuItem
import com.gumlapolytechnic.gpconnect.data.repository.CanteenRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Firebase-backed canteen catalog repository.
 *
 * Student reads are constrained by Firestore Security Rules:
 * - Categories: where enabled == true
 * - Menu items: where isAvailable == true
 * - Menu by category: where categoryId == X && isAvailable == true
 *
 * Admin writes (create/update/enable) are authorized by Rules.
 * This repository does NOT perform local role checks; Rules are authoritative.
 */
class FirebaseCanteenRepository : CanteenRepository {

    private val firestore get() = FirebaseServices.firestore
    private val auth get() = FirebaseServices.auth

    private val TAG = "GPFirebaseCanteen"

    // --- Student / Member read operations ------------------------------------

    override fun observeCategories(): Flow<List<CanteenCategory>> = callbackFlow {
        // Student query MUST explicitly constrain to enabled == true
        // to satisfy the Firestore Security Rules list predicate.
        val registration: ListenerRegistration = firestore.collection(CATEGORIES)
            .whereEqualTo("enabled", true)
            .orderBy("displayOrder")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observeCategories error: ${error.message}", error)
                    // Do not fabricate an empty list on error; close the channel
                    close(error)
                    return@addSnapshotListener
                }
                val categories = snapshot?.documents
                    ?.mapNotNull { it.toCanteenCategory() }
                    .orEmpty()
                trySend(categories)
            }
        awaitClose { registration.remove() }
    }

    override fun observeAvailableMenuItems(): Flow<List<CanteenMenuItem>> = callbackFlow {
        // Student query MUST explicitly constrain to isAvailable == true.
        // No ordering by category then displayOrder is possible without
        // a composite index on (isAvailable ASC, categoryId ASC, displayOrder ASC)
        // which does not currently exist. We order by displayOrder alone.
        // The client can group by categoryId if needed.
        val registration: ListenerRegistration = firestore.collection(MENU_ITEMS)
            .whereEqualTo("isAvailable", true)
            .orderBy("displayOrder")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observeAvailableMenuItems error: ${error.message}", error)
                    close(error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents
                    ?.mapNotNull { it.toCanteenMenuItem() }
                    .orEmpty()
                trySend(items)
            }
        awaitClose { registration.remove() }
    }

    override fun observeAvailableMenuItemsByCategory(categoryId: String): Flow<List<CanteenMenuItem>> = callbackFlow {
        // Query MUST constrain both categoryId and isAvailable to satisfy Rules
        // and use the existing composite index:
        // categoryId ASC, isAvailable ASC, displayOrder ASC
        val registration: ListenerRegistration = firestore.collection(MENU_ITEMS)
            .whereEqualTo("categoryId", categoryId)
            .whereEqualTo("isAvailable", true)
            .orderBy("displayOrder")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observeAvailableMenuItemsByCategory($categoryId) error: ${error.message}", error)
                    close(error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents
                    ?.mapNotNull { it.toCanteenMenuItem() }
                    .orEmpty()
                trySend(items)
            }
        awaitClose { registration.remove() }
    }

    // --- Canteen Admin / Super Admin write operations ------------------------

    override suspend fun createCategory(
        name: String,
        description: String?,
        displayOrder: Int,
        enabled: Boolean,
        createdAt: Long
    ): CanteenCategory {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Cannot create category while signed out")

        val fields = categoryFields(name, description, displayOrder, enabled, createdAt)
        val reference = firestore.collection(CATEGORIES).add(fields).awaitTask()

        Log.i(TAG, "Created category ${reference.id} by $uid")
        return CanteenCategory(
            id = reference.id,
            name = name,
            description = description,
            displayOrder = displayOrder,
            enabled = enabled,
            createdAt = createdAt,
            updatedAt = createdAt,
        )
    }

    override suspend fun updateCategory(
        categoryId: String,
        name: String?,
        description: String?,
        displayOrder: Int?,
        enabled: Boolean?,
        updatedAt: Long
    ) {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("Signed out")

        // Only include explicitly supplied fields; skip if all null
        val fields = categoryUpdateFields(name, description, displayOrder, enabled, updatedAt)
        if (fields.size > 1) { // size > 1 because updatedAt is always present
            firestore.collection(CATEGORIES).document(categoryId).update(fields).awaitTask()
            Log.i(TAG, "Updated category $categoryId by $uid")
        } else {
            Log.i(TAG, "No fields to update for category $categoryId")
        }
    }

    override suspend fun setCategoryEnabled(categoryId: String, enabled: Boolean) {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("Signed out")

        firestore.collection(CATEGORIES).document(categoryId)
            .update(
                "enabled", enabled,
                "updatedAt", System.currentTimeMillis()
            )
            .awaitTask()
        Log.i(TAG, "Category $categoryId enabled=$enabled by $uid")
    }

    override suspend fun createMenuItem(
        categoryId: String,
        name: String,
        description: String?,
        pricePaise: Long,
        imageUrl: String?,
        isAvailable: Boolean,
        displayOrder: Int,
        createdAt: Long
    ): CanteenMenuItem {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Cannot create menu item while signed out")

        val fields = menuItemFields(categoryId, name, description, pricePaise, imageUrl, isAvailable, displayOrder, createdAt)
        val reference = firestore.collection(MENU_ITEMS).add(fields).awaitTask()

        Log.i(TAG, "Created menu item ${reference.id} in category $categoryId by $uid")
        return CanteenMenuItem(
            id = reference.id,
            categoryId = categoryId,
            name = name,
            description = description,
            pricePaise = pricePaise,
            imageUrl = imageUrl,
            isAvailable = isAvailable,
            displayOrder = displayOrder,
            createdAt = createdAt,
            updatedAt = createdAt,
        )
    }

    override suspend fun updateMenuItem(
        itemId: String,
        name: String?,
        description: String?,
        pricePaise: Long?,
        imageUrl: String?,
        isAvailable: Boolean?,
        displayOrder: Int?,
        updatedAt: Long
    ) {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("Signed out")

        val fields = menuItemUpdateFields(name, description, pricePaise, imageUrl, isAvailable, displayOrder, updatedAt)
        if (fields.size > 1) { // updatedAt always present
            firestore.collection(MENU_ITEMS).document(itemId).update(fields).awaitTask()
            Log.i(TAG, "Updated menu item $itemId by $uid")
        } else {
            Log.i(TAG, "No fields to update for menu item $itemId")
        }
    }

    override suspend fun setMenuItemAvailable(itemId: String, isAvailable: Boolean) {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("Signed out")

        firestore.collection(MENU_ITEMS).document(itemId)
            .update(
                "isAvailable", isAvailable,
                "updatedAt", System.currentTimeMillis()
            )
            .awaitTask()
        Log.i(TAG, "Menu item $itemId isAvailable=$isAvailable by $uid")
    }

    private companion object {
        const val CATEGORIES = "canteenCategories"
        const val MENU_ITEMS = "canteenMenuItems"
    }
}