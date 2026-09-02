package com.gumlapolytechnic.gpconnect.data.firebase

import android.util.Log
import com.gumlapolytechnic.gpconnect.data.model.CanteenOrder
import com.gumlapolytechnic.gpconnect.data.model.CheckoutRequest
import com.gumlapolytechnic.gpconnect.data.model.OrderItemSnapshot
import com.gumlapolytechnic.gpconnect.data.model.OrderStatus
import com.gumlapolytechnic.gpconnect.data.repository.OrderRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.HttpsCallableResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import java.io.IOException

/**
 * Firebase-backed canteen order repository.
 *
 * Checkout path:
 *   submitOrder → callable `placeCanteenOrder` → trusted Cloud Function → Firestore transaction
 *   → returns orderId → fetch authoritative CanteenOrder from Firestore
 *
 * Students NEVER directly write to `canteenOrders`. The callable function is the
 * single authoritative checkout creation path.
 */
class FirebaseOrderRepository : OrderRepository {

    private val firestore get() = FirebaseServices.firestore
    private val functions get() = FirebaseFunctions.getInstance()
    private val auth get() = FirebaseServices.auth

    private val TAG = "GPFirebaseOrders"

    // --- Student operations ---------------------------------------------------

    override suspend fun submitOrder(request: CheckoutRequest): Result<CanteenOrder> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("No authenticated user"))

        // Build request payload - ONLY menuItemId, quantity, and idempotencyKey
        val payload = mutableMapOf<String, Any>()
        payload["idempotencyKey"] = request.idempotencyKey
        payload["items"] = request.items.map { item ->
            mapOf(
                "menuItemId" to item.menuItemId,
                "quantity" to item.quantity
            )
        }

        return try {
            Log.i(TAG, "Submitting order with idempotencyKey=${request.idempotencyKey} for user=$uid")
            val callable = functions.getHttpsCallable("placeCanteenOrder")
            val result = callable.call(payload).awaitResult()

            // Extract orderId from successful response
            val data = result.data as? Map<*, *>
            val orderId = data?.get("orderId") as? String
                ?: return Result.failure(IllegalStateException("Cloud Function response missing orderId"))

            // Fetch the authoritative order document created by the Cloud Function
            val orderSnap = firestore.collection(ORDERS).document(orderId).get().awaitTask()
            val order = orderSnap?.toCanteenOrder()
                ?: return Result.failure(IllegalStateException("Created order document not found: $orderId"))

            Log.i(TAG, "Order submitted successfully: orderId=$orderId, total=${order.formattedTotal()}")
            Result.success(order)
        } catch (e: Exception) {
            Log.e(TAG, "submitOrder failed for uid=$uid", e)
            Result.failure(mapFunctionsError(e))
        }
    }

    override fun observeActiveOrder(customerId: String): Flow<CanteenOrder?> {
        val uid = auth.currentUid() ?: return flow { emit(null) }
        // Students can only observe their own active order
        val effectiveCustomerId = if (customerId == uid) uid else {
            Log.w(TAG, "Student $uid attempted to observe another user's active order ($customerId); returning null")
            return flow { emit(null) }
        }

        return callbackFlow {
            val registration = firestore.collection(ORDERS)
                .whereEqualTo("customerId", effectiveCustomerId)
                .whereIn("status", ACTIVE_STATUSES)
                .limit(1)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "observeActiveOrder error: ${error.message}", error)
                        trySend(null)
                        return@addSnapshotListener
                    }
                    val order = snapshot?.documents?.firstOrNull()?.toCanteenOrder()
                    trySend(order)
                }
            awaitClose { registration.remove() }
        }
    }

    override fun observeOrderHistory(customerId: String): Flow<List<CanteenOrder>> {
        val uid = auth.currentUid() ?: return flow { emit(emptyList()) }
        // Students can only observe their own order history
        val effectiveCustomerId = if (customerId == uid) uid else {
            Log.w(TAG, "Student $uid attempted to observe another user's order history ($customerId); returning empty")
            return flow { emit(emptyList()) }
        }

        return callbackFlow {
            val registration = firestore.collection(ORDERS)
                .whereEqualTo("customerId", effectiveCustomerId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "observeOrderHistory error: ${error.message}", error)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val orders = snapshot?.documents
                        ?.mapNotNull { it.toCanteenOrder() }
                        .orEmpty()
                    trySend(orders)
                }
            awaitClose { registration.remove() }
        }
    }

    override fun observeOrder(orderId: String): Flow<CanteenOrder?> {
        val uid = auth.currentUid() ?: return flow { emit(null) }
        return callbackFlow {
            val registration = firestore.collection(ORDERS).document(orderId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "observeOrder($orderId) error: ${error.message}", error)
                        trySend(null)
                        return@addSnapshotListener
                    }
                    val order = snapshot?.toCanteenOrder()
                    // Enforce ownership: student can only read their own orders
                    val authorized = order == null || order.customerId == uid
                    trySend(if (authorized) order else null)
                }
            awaitClose { registration.remove() }
        }
    }

    override suspend fun cancelOrder(orderId: String, reason: String?): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("No authenticated user"))

        return try {
            // Verify ownership before attempting cancellation
            val orderSnap = firestore.collection(ORDERS).document(orderId).get().awaitTask()
            val order = orderSnap?.toCanteenOrder()
                ?: return Result.failure(IllegalStateException("Order not found: $orderId"))

            if (order.customerId != uid) {
                return Result.failure(SecurityException("Cannot cancel another user's order"))
            }

            val updates = mutableMapOf<String, Any>(
                "status" to OrderStatus.CANCELLED.name,
                "updatedAt" to System.currentTimeMillis()
            )
            reason?.takeIf { it.isNotBlank() }?.let { updates["cancellationReason"] = it }

            firestore.collection(ORDERS).document(orderId).update(updates).awaitTask()
            Log.i(TAG, "Order $orderId cancelled by user $uid")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "cancelOrder($orderId) failed for uid=$uid", e)
            Result.failure(mapFirestoreError(e))
        }
    }

    // --- Canteen Admin / Super Admin operations -------------------------------

    override fun observeActiveOrderQueue(): Flow<Result<List<CanteenOrder>>> {
        // Role enforcement is handled by Firestore Security Rules
        // This will return Result.failure if the user lacks admin permissions
        return callbackFlow {
            val registration = firestore.collection(ORDERS)
                .whereIn("status", ACTIVE_STATUSES)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "observeActiveOrderQueue error: ${error.message}", error)
                        trySend(Result.failure(error))
                        return@addSnapshotListener
                    }
                    val orders = snapshot?.documents
                        ?.mapNotNull { it.toCanteenOrder() }
                        .orEmpty()
                    trySend(Result.success(orders))
                }
            awaitClose { registration.remove() }
        }
    }

    override suspend fun updateOrderStatus(
        orderId: String,
        newStatus: OrderStatus,
        cancellationReason: String?
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("No authenticated user"))

        return try {
            // Verify the order exists
            val orderSnap = firestore.collection(ORDERS).document(orderId).get().awaitTask()
            val order = orderSnap?.toCanteenOrder()
                ?: return Result.failure(IllegalStateException("Order not found: $orderId"))

            // Verify the transition is valid (defense in depth - rules also enforce this)
            if (!order.status.canTransitionTo(newStatus)) {
                return Result.failure(IllegalArgumentException(
                    "Invalid status transition: ${order.status} -> $newStatus"
                ))
            }

            val updates = mutableMapOf<String, Any>(
                "status" to newStatus.name,
                "updatedAt" to System.currentTimeMillis(),
                "decidedBy" to uid  // Derived from authenticated user, not caller-provided
            )
            // Only set cancellationReason for CANCELLED status
            if (newStatus == OrderStatus.CANCELLED) {
                cancellationReason?.takeIf { it.isNotBlank() }?.let { updates["cancellationReason"] = it }
            }

            firestore.collection(ORDERS).document(orderId).update(updates).awaitTask()
            Log.i(TAG, "Order $orderId status updated to $newStatus by $uid")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "updateOrderStatus($orderId -> $newStatus) failed for uid=$uid", e)
            Result.failure(mapFirestoreError(e))
        }
    }

    private companion object {
        const val ORDERS = "canteenOrders"
        val ACTIVE_STATUSES = listOf(
            OrderStatus.PENDING.name,
            OrderStatus.CONFIRMED.name,
            OrderStatus.PREPARING.name,
            OrderStatus.READY.name
        )
    }

    private fun FirebaseAuth.currentUid(): String? = currentUser?.uid

    private fun mapFunctionsError(e: Exception): Exception {
        return when {
            e is FirebaseFunctionsException -> when (e.code) {
                com.google.firebase.functions.FirebaseFunctionsException.Code.UNAUTHENTICATED ->
                    SecurityException("Authentication required")
                com.google.firebase.functions.FirebaseFunctionsException.Code.PERMISSION_DENIED ->
                    SecurityException("Permission denied")
                com.google.firebase.functions.FirebaseFunctionsException.Code.INVALID_ARGUMENT ->
                    IllegalArgumentException(e.message ?: "Invalid argument")
                com.google.firebase.functions.FirebaseFunctionsException.Code.NOT_FOUND ->
                    IllegalStateException(e.message ?: "Not found")
                com.google.firebase.functions.FirebaseFunctionsException.Code.FAILED_PRECONDITION ->
                    IllegalStateException(e.message ?: "Precondition failed")
                com.google.firebase.functions.FirebaseFunctionsException.Code.OUT_OF_RANGE ->
                    IllegalStateException(e.message ?: "Out of range")
                com.google.firebase.functions.FirebaseFunctionsException.Code.ALREADY_EXISTS ->
                    IllegalStateException(e.message ?: "Already exists")
                else -> IOException(e.message ?: "Server error")
            }
            e is IOException -> e
            else -> IOException("Network or unknown error: ${e.message}", e)
        }
    }

    private fun mapFirestoreError(e: Exception): Exception {
        // Pattern from existing repositories
        return when (e) {
            is com.google.firebase.firestore.FirebaseFirestoreException -> when (e.code) {
                com.google.firebase.firestore.FirebaseFirestoreException.Code.UNAVAILABLE,
                com.google.firebase.firestore.FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                    IOException(e.message ?: "Network error")
                else -> SecurityException("Access denied: ${e.message}")
            }
            else -> IOException("Firestore error: ${e.message}", e)
        }
    }
}