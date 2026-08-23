package com.gumlapolytechnic.gpconnect.data.firebase

import com.google.android.gms.tasks.Task
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Suspends until the Play Services [Task] completes. Written locally because
 * the Firebase modules do not ship kotlinx-coroutines-play-services
 * transitively (verified against the firebase-firestore 26.6.0 POM).
 */
internal suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            continuation.resume(task.result)
        } else {
            continuation.cancel(task.exception ?: RuntimeException("Firebase task failed"))
        }
    }
}
