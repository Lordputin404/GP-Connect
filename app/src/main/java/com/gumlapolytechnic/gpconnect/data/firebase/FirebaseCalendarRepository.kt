package com.gumlapolytechnic.gpconnect.data.firebase

import android.util.Log
import com.gumlapolytechnic.gpconnect.data.model.CalendarEvent
import com.gumlapolytechnic.gpconnect.data.model.EventAttachment
import com.gumlapolytechnic.gpconnect.data.model.EventAttachmentType
import com.gumlapolytechnic.gpconnect.data.repository.CalendarEventDraft
import com.gumlapolytechnic.gpconnect.data.repository.CalendarQuery
import com.gumlapolytechnic.gpconnect.data.repository.CalendarRepository
import com.gumlapolytechnic.gpconnect.data.repository.PendingAttachment
import com.gumlapolytechnic.gpconnect.data.repository.applyCalendarQuery
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.StorageMetadata
import java.io.File
import java.util.UUID
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Firestore + Storage backed College Calendar. Mirrors the notice repository:
 * a single collection listener per query with client-side filtering/ordering
 * ([applyCalendarQuery]) so no composite index is needed. Reads are governed
 * by firestore.rules — members see published events only, the SUPER_ADMIN
 * everything — and every write returns [Result] so a rules rejection shows as
 * an admin error state rather than a crash.
 *
 * Attachment binaries live in Firebase Storage under
 * `calendarEvents/{eventId}/attachments/{uniqueFileName}` (governed by
 * storage.rules); the event document carries only the metadata array.
 */
class FirebaseCalendarRepository : CalendarRepository {

    private val firestore get() = FirebaseServices.firestore
    private val storage get() = FirebaseServices.storage

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
        // The document ID is pre-allocated (document() does not create it) so
        // attachment uploads can target calendarEvents/{id}/attachments/*
        // before the event exists. A failed upload therefore never leaves a
        // half-created event behind — at worst an orphaned file.
        val docRef = firestore.collection(CALENDAR_EVENTS).document()
        val uploaded = uploadPendingAttachments(docRef.id, draft.pendingUploads)
        docRef.set(
            calendarEventFields(
                title = draft.title,
                description = draft.description,
                startDate = draft.startDate,
                endDate = draft.endDate,
                type = draft.type,
                isAllDay = draft.isAllDay,
                status = draft.status,
                isPublished = draft.isPublished,
                attachments = uploaded,
                createdAt = now,
                updatedAt = now,
            ),
        ).awaitTask()
        Log.i(TAG, "Created calendar event '${draft.title}' (${docRef.id}) with ${uploaded.size} attachment(s)")
    }.onFailure { failure ->
        Log.e(TAG, "createEvent failed: ${failure.message}", failure)
    }.map { }

    override suspend fun updateEvent(event: CalendarEvent): Result<Unit> =
        updateEvent(event, pendingUploads = emptyList(), removedAttachments = emptyList())

    override suspend fun updateEvent(
        event: CalendarEvent,
        pendingUploads: List<PendingAttachment>,
        removedAttachments: List<EventAttachment>,
    ): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        // Uploads run first: a failed upload aborts the save before the
        // document is touched, so the event keeps its previous attachments.
        val uploaded = uploadPendingAttachments(event.id, pendingUploads)
        val attachments = event.attachments + uploaded
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
                    attachments = attachments,
                    createdAt = event.createdAt,
                    updatedAt = now,
                ),
            )
            .awaitTask()
        // Dropped attachments: delete their Storage binaries. Best-effort —
        // the metadata is already gone, so a failed delete only orphans a file.
        removedAttachments.forEach { attachment -> deleteAttachmentFile(attachment) }
        Log.i(TAG, "Updated calendar event '${event.title}' (${event.id}): ${attachments.size} attachment(s)")
    }.onFailure { failure ->
        Log.e(TAG, "updateEvent(${event.id}) failed: ${failure.message}", failure)
    }.map { }

    override suspend fun deleteEvent(eventId: String): Result<Unit> = runCatching {
        // Delete the document first, then its attachment binaries best-effort:
        // a failed file delete only orphans files, it never breaks the event.
        val attachments = getEvent(eventId)?.attachments.orEmpty()
        firestore.collection(CALENDAR_EVENTS).document(eventId).delete().awaitTask()
        attachments.forEach { attachment -> deleteAttachmentFile(attachment) }
        Log.i(TAG, "Deleted calendar event $eventId with ${attachments.size} attachment(s)")
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

    override suspend fun downloadAttachment(
        attachment: EventAttachment,
        targetDirectory: File,
    ): Result<File> =
        runCatching {
            // One cache file per event attachment, addressed by its Storage
            // path; the directory is the app's private cache (what the
            // FileProvider shares with external viewer apps).
            val target = File(targetDirectory, attachment.storagePath)
            target.parentFile?.mkdirs()
            if (target.exists()) target.delete()
            storage.getReference(attachment.storagePath).getFile(target).awaitTask()
            target
        }.onFailure { failure ->
            Log.e(TAG, "downloadAttachment(${attachment.storagePath}) failed: ${failure.message}", failure)
        }

    // ---- attachment uploads / cleanup --------------------------------------

    private suspend fun uploadPendingAttachments(
        eventId: String,
        pending: List<PendingAttachment>,
    ): List<EventAttachment> = pending.map { file ->
        val type = EventAttachmentType.fromFileName(file.name)
            ?: throw IllegalArgumentException("Unsupported calendar attachment type: ${file.name}")
        // Storage object name: sanitized display name plus a unique prefix,
        // so re-attaching a same-named file never overwrites the earlier upload.
        val uniqueName = "${System.currentTimeMillis()}-${UUID.randomUUID().toString().take(8)}-" +
            file.name.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val storagePath = "$CALENDAR_EVENTS/$eventId/$ATTACHMENTS/$uniqueName"
        storage.getReference(storagePath)
            .putFile(file.uri, StorageMetadata.Builder().setContentType(type.mimeType).build())
            .awaitTask()
        EventAttachment(
            name = file.name,
            storagePath = storagePath,
            // Deliberately left empty: tokenized download URLs are readable by
            // anyone who has them. Members download through the SDK via
            // storagePath, which storage.rules keep auth-only.
            downloadUrl = "",
            mimeType = type.mimeType,
            size = file.size,
            type = type,
        )
    }

    private suspend fun deleteAttachmentFile(attachment: EventAttachment) {
        runCatching { storage.getReference(attachment.storagePath).delete().awaitTask() }
            .onFailure { failure ->
                Log.w(TAG, "Could not delete attachment '${attachment.storagePath}': ${failure.message}")
            }
    }

    private companion object {
        const val TAG = "GPFirebaseCalendar"
        const val CALENDAR_EVENTS = "calendarEvents"
        const val ATTACHMENTS = "attachments"
    }
}
