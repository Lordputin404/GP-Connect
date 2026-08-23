package com.gumlapolytechnic.gpconnect.data.mock

/**
 * Lightweight fictional event previews for the Home dashboard. This is a
 * preview only — the real Events module (data, detail screens, registration)
 * belongs to Phase 6 and will replace these with a proper model/repository.
 */
data class CampusEventPreview(
    val title: String,
    val dayLabel: String,
    val monthLabel: String,
    val location: String,
)

object MockEventPreviews {
    val upcoming: List<CampusEventPreview> = listOf(
        CampusEventPreview(
            title = "Srijan 2026 — Annual Tech Fest",
            dayLabel = "18",
            monthLabel = "OCT",
            location = "Main Auditorium",
        ),
        CampusEventPreview(
            title = "Alumni Interaction Session",
            dayLabel = "24",
            monthLabel = "OCT",
            location = "Seminar Hall B",
        ),
        CampusEventPreview(
            title = "Annual Sports Day",
            dayLabel = "05",
            monthLabel = "NOV",
            location = "Main Ground",
        ),
    )
}
