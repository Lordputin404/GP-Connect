package com.gumlapolytechnic.gpconnect.data.repository

import com.gumlapolytechnic.gpconnect.data.model.CalendarEvent
import com.gumlapolytechnic.gpconnect.data.model.CalendarEventStatus
import com.gumlapolytechnic.gpconnect.data.model.CalendarEventType
import com.gumlapolytechnic.gpconnect.data.model.EventAttachment
import com.gumlapolytechnic.gpconnect.util.Dates
import java.io.File
import kotlinx.coroutines.flow.Flow

/**
 * A file picked in the admin form, ready to be uploaded with the save:
 * the content-resolver [uri] plus the metadata needed before the upload
 * (display name, byte size). The supported-type check happens in the form —
 * [EventAttachmentType.fromFileName] — so unsupported picks never reach the
 * repository.
 */
data class PendingAttachment(
    val uri: android.net.Uri,
    val name: String,
    val size: Long,
)

/**
 * Query for the calendar list. All constraints combine and are evaluated on
 * the client by [applyCalendarQuery]; `publishedOnly` additionally shapes the
 * Firestore query itself (see FirebaseCalendarRepository).
 */
data class CalendarQuery(
    val type: CalendarEventType? = null,
    val publishedOnly: Boolean = true,
    val upcomingOnly: Boolean = false,
)

/** Input for creating a calendar event. */
data class CalendarEventDraft(
    val title: String,
    val description: String,
    val startDate: Long,
    val endDate: Long,
    val type: CalendarEventType,
    val isAllDay: Boolean,
    val status: CalendarEventStatus,
    val isPublished: Boolean,
    /** Files picked in the form; uploaded to Storage on create. */
    val pendingUploads: List<PendingAttachment> = emptyList(),
)

/**
 * College Calendar contract. Reads are constrained by Firestore security
 * rules: members may read only published events, the SUPER_ADMIN everything.
 * Write methods return [Result] so a rules rejection surfaces in the admin
 * UI as an error banner instead of a crash.
 *
 * Attachment binaries live in Firebase Storage under
 * `calendarEvents/{eventId}/attachments/`; only metadata is stored on the
 * Firestore document. Uploads/download/cleanup are governed by storage.rules.
 */
interface CalendarRepository {
    fun observeEvents(query: CalendarQuery = CalendarQuery()): Flow<Result<List<CalendarEvent>>>

    fun observeEvent(id: String): Flow<Result<CalendarEvent?>>

    suspend fun getEvent(id: String): CalendarEvent?

    suspend fun createEvent(draft: CalendarEventDraft): Result<Unit>

    /** Replaces the stored event wholesale; the ID identifies the target. */
    suspend fun updateEvent(event: CalendarEvent): Result<Unit>

    /**
     * Replaces the stored event wholesale and synchronizes its attachments:
     * uploads [pendingUploads] to Storage, deletes [removedAttachments] from
     * Storage, then persists the resulting metadata list on the document.
     */
    suspend fun updateEvent(
        event: CalendarEvent,
        pendingUploads: List<PendingAttachment>,
        removedAttachments: List<EventAttachment>,
    ): Result<Unit>

    suspend fun deleteEvent(eventId: String): Result<Unit>

    suspend fun setPublished(eventId: String, published: Boolean): Result<Unit>

    /**
     * Downloads one attachment into the app's private cache (returned as a
     * [File]) so it can be opened/shared via a FileProvider URI. Fails if
     * storage.rules deny the read or the transfer errors.
     */
    suspend fun downloadAttachment(
        attachment: EventAttachment,
        targetDirectory: File,
    ): Result<File>
}

/**
 * Client-side query evaluation: published/type/upcoming filters and
 * upcoming-first ordering (soonest start first; past events afterwards,
 * newest first). The "today counts" rule uses the local start of day so a
 * single-day event stays upcoming on its own day.
 */
internal fun List<CalendarEvent>.applyCalendarQuery(query: CalendarQuery): List<CalendarEvent> {
    val todayStart = Dates.startOfDay(System.currentTimeMillis())
    val (upcoming, past) = partition { it.endDate >= todayStart }
    val filter: (CalendarEvent) -> Boolean = { event ->
        (!query.publishedOnly || event.isPublished) &&
            (query.type == null || event.type == query.type)
    }
    return (upcoming.sortedBy { it.startDate } + past.sortedByDescending { it.startDate })
        .filter(filter)
}
