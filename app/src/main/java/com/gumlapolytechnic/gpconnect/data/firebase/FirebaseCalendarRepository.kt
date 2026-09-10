package com.gumlapolytechnic.gpconnect.data.firebase

import android.util.Log
import com.gumlapolytechnic.gpconnect.data.model.CalendarEvent
import com.gumlapolytechnic.gpconnect.data.repository.CalendarEventDraft
import com.gumlapolytechnic.gpconnect.data.repository.CalendarQuery
import com.gumlapolytechnic.gpconnect.data.repository.CalendarRepository
import com.gumlapolytechnic.gpconnect.data.repository.applyCalendarQuery
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Firestore-backed College Calendar. Mirrors the notice repository: a single
 * collection listener per query with client-side filtering/ordering
 * ([applyCalendarQuery]) so no composite index is needed. Reads are governed
 * by firestore.rules — members see published events only, the SUPER_ADMIN
 * everything — and every write returns [Result] so a rules rejection shows as
 * an admin error state rather than a crash.
 */
class FirebaseCalendarRepository : CalendarRepository {

    private val firestore get() = FirebaseServices.firestore

    override fun observeEvents(query: CalendarQuery): Flow<Result<List<CalendarEvent>>> =
        callbackFlow {
            // Published-only reads filter server-side. The rules `list` grants
            // members published events and the SUPER_ADMIN everything; carrying
            // the isPublished constraint in the query keeps member reads exact
            // and makes a rules misconfiguration visible as an error flow.
            val collection = if (query.publishedOnly) {
                firestore.collection(CALENDAR_EVENTS).whereEqualTo("isPublished", true)
            } else {
                firestore.collection(CALENDAR_EVENTS)
            }
            val registration: ListenerRegistration =
                collection.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(
                            TAG,
                            "observeEvents failed: code=${error.code} message=${error.message}",
                            error,
                        )
                        trySend(Result.failure(error))
                        return@addSnapshotListener
                    }
                    val events = snapshot?.documents
                        ?.mapNotNull { it.toCalendarEvent() }
                        .orEmpty()
                        .applyCalendarQuery(query)
                    trySend(Result.success(events))
                }
            awaitClose { registration.remove() }
        }

    override fun observeEvent(id: String): Flow<Result<CalendarEvent?>> = callbackFlow {
        val registration: ListenerRegistration =
            firestore.collection(CALENDAR_EVENTS).document(id)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "observeEvent($id) failed: ${error.message}", error)
                        trySend(Result.failure(error))
                        return@addSnapshotListener
                    }
                    trySend(Result.success(snapshot?.toCalendarEvent()))
                }
        awaitClose { registration.remove() }
    }

    override suspend fun getEvent(id: String): CalendarEvent? =
        firestore.collection(CALENDAR_EVENTS).document(id).get().awaitTask()?.toCalendarEvent()

    override suspend fun createEvent(draft: CalendarEventDraft): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        firestore.collection(CALENDAR_EVENTS)
            .add(
                calendarEventFields(
                    title = draft.title,
                    description = draft.description,
                    startDate = draft.startDate,
                    endDate = draft.endDate,
                    type = draft.type,
                    isAllDay = draft.isAllDay,
                    status = draft.status,
                    isPublished = draft.isPublished,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            .awaitTask()
        Log.i(TAG, "Created calendar event '${draft.title}'")
    }.onFailure { failure ->
        Log.e(TAG, "createEvent failed: ${failure.message}", failure)
    }.map { }

    override suspend fun updateEvent(event: CalendarEvent): Result<Unit> = runCatching {
        firestore.collection(CALENDAR_EVENTS).document(event.id)
            .set(
                calendarEventFields(
                    title = event.title,
                    description = event.description,
                    startDate = event.startDate,
                    endDate = event.endDate,
                    type = event.type,
                    isAllDay = event.isAllDay,
                    status = event.status,
                    isPublished = event.isPublished,
                    createdAt = event.createdAt,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            .awaitTask()
        Log.i(TAG, "Updated calendar event '${event.title}' (${event.id})")
    }.onFailure { failure ->
        Log.e(TAG, "updateEvent(${event.id}) failed: ${failure.message}", failure)
    }.map { }

    override suspend fun deleteEvent(eventId: String): Result<Unit> = runCatching {
        firestore.collection(CALENDAR_EVENTS).document(eventId).delete().awaitTask()
        Log.i(TAG, "Deleted calendar event $eventId")
    }.onFailure { failure ->
        Log.e(TAG, "deleteEvent($eventId) failed: ${failure.message}", failure)
    }.map { }

    override suspend fun setPublished(eventId: String, published: Boolean): Result<Unit> =
        runCatching {
            firestore.collection(CALENDAR_EVENTS).document(eventId)
                .update("isPublished", published, "updatedAt", System.currentTimeMillis())
                .awaitTask()
            Log.i(TAG, "Set isPublished=$published on calendar event $eventId")
        }.onFailure { failure ->
            Log.e(TAG, "setPublished($eventId) failed: ${failure.message}", failure)
        }.map { }

    private companion object {
        const val TAG = "GPFirebaseCalendar"
        const val CALENDAR_EVENTS = "calendarEvents"
    }
}
