package com.gumlapolytechnic.gpconnect.data.firebase

import com.google.android.gms.tasks.Task
import com.google.firebase.functions.HttpsCallableResult
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Suspends until the Play Services [Task] completes. Written locally because
 * the Firebase modules do not ship kotlinx-coroutines-play-services
 * transitively (verified against the firebase-firestore 26.6.0 POM).
 *
 * IMPORTANT: failures RESUME the coroutine with the original exception
 * (resumeWithException). An earlier version used continuation.cancel(cause),
 * which cancelled the calling coroutine and surfaced the real error as an
 * opaque CancellationException — callers' typed catch blocks never ran and
 * every failure collapsed into a generic error.
 */
internal suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            continuation.resume(task.result)
        } else {
            val cause = task.exception ?: RuntimeException("Firebase task failed without an exception")
            android.util.Log.w("GPTaskAwait", "Task failed: ${cause.javaClass.simpleName}: ${cause.message}")
            continuation.resumeWithException(cause)
        }
    }
}

/**
 * Suspends until the Callable Function [Task] of [HttpsCallableResult] completes.
 */
internal suspend fun Task<HttpsCallableResult>.awaitResult(): HttpsCallableResult = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            continuation.resume(task.result)
        } else {
            val cause = task.exception ?: RuntimeException("Callable task failed")
            continuation.resumeWithException(cause)
        }
    }
}
