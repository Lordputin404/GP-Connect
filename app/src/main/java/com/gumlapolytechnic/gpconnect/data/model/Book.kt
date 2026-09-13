package com.gumlapolytechnic.gpconnect.data.model

/**
 * One library catalog entry, stored at `libraryBooks/{bookId}`.
 *
 * [departmentId] is the canonical [Department] id (the enum name — the same
 * single registry used by `users.department`), never a display label; it is
 * validated against the `departmentIds()` allow-list in firestore.rules.
 *
 * Copies model availability only. Book issuing/returning, fines and
 * per-student borrowing records are deliberately out of scope for this
 * phase: `availableCopies` counts what is on the shelf right now,
 * `totalCopies` what the library owns.
 */
data class Book(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val departmentId: String = "",
    val category: String = "",
    val isbn: String = "",
    val rackNumber: String = "",
    val totalCopies: Int = 0,
    val availableCopies: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    /** Resolved department, tolerating legacy label values defensively. */
    val departmentOrNull: Department? get() = Department.resolveOrNull(departmentId)

    val isAvailable: Boolean get() = availableCopies > 0
}
