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
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)
