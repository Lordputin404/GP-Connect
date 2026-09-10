package com.gumlapolytechnic.gpconnect.data.model

/**
 * College Calendar event categories. Enum names are persisted verbatim in
 * `calendarEvents.type` and mirrored by the allow-lists in firestore.rules.
 */
enum class CalendarEventType {
    EXAM,
    HOLIDAY,
    ACTIVITY,
    CO_CURRICULAR,
}

/** Confirmation state of a calendar event. Enum names persisted verbatim. */
enum class CalendarEventStatus {
    CONFIRMED,
    TENTATIVE,
}

/**
 * Supported calendar event attachment file types. The extension allow-list is
 * mirrored by storage.rules (`calendarEvents/{eventId}/attachments/…`).
 */
enum class EventAttachmentType {
    PDF,
    DOC,
    DOCX,
    JPG,
    JPEG,
    PNG,
    WEBP;

    val mimeType: String
        get() = when (this) {
            PDF -> "application/pdf"
            DOC -> "application/msword"
            DOCX -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            JPG, JPEG -> "image/jpeg"
            PNG -> "image/png"
            WEBP -> "image/webp"
        }

    companion object {
        /** Extension (lowercase, without dot) → type; unknown extensions yield null. */
        private val byExtension = entries.associateBy { it.name.lowercase() }

        /** `null` when the file name has no supported extension. */
        fun fromFileName(fileName: String): EventAttachmentType? =
            byExtension[fileName.substringAfterLast('.', "").lowercase()]
    }
}

/**
 * Metadata of one file attached to a calendar event. The binary lives in
 * Firebase Storage at [storagePath] (never in Firestore); this metadata is
 * embedded in the `calendarEvents/{eventId}` document's `attachments` array.
 */
data class EventAttachment(
    val name: String,
    val storagePath: String,
    val downloadUrl: String,
    val mimeType: String,
    /** File size in bytes. */
    val size: Long,
    val type: EventAttachmentType,
)

/**
 * One entry of the College Calendar, stored at `calendarEvents/{eventId}`.
 *
 * Dates are epoch milliseconds persisted as Firestore numbers — the same
 * convention as every other timestamp in this project (Notice, SignupRequest,
 * users), so converters and rules stay uniform. For a single-day event
 * [endDate] equals [startDate]; a still-running or future event satisfies
 * `endDate >= startOfDay(now)` — evaluated via [CalendarQuery], never here.
 */
data class CalendarEvent(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val startDate: Long = 0L,
    val endDate: Long = 0L,
    val type: CalendarEventType = CalendarEventType.ACTIVITY,
    val isAllDay: Boolean = false,
    val status: CalendarEventStatus = CalendarEventStatus.CONFIRMED,
    val isPublished: Boolean = false,
    val attachments: List<EventAttachment> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)
