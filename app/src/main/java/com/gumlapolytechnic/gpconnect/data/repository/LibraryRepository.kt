package com.gumlapolytechnic.gpconnect.data.repository

import com.gumlapolytechnic.gpconnect.data.model.Book
import com.gumlapolytechnic.gpconnect.data.model.Department
import kotlinx.coroutines.flow.Flow

/**
 * Query for the book list. `department` shapes the Firestore query itself
 * (members must send it to satisfy the rules' list constraint); the optional
 * `search` text is evaluated on the client by [applyBookQuery].
 */
data class BookQuery(
    val department: Department? = null,
    val search: String = "",
)

/** Input for creating a book. The repository stamps the timestamps. */
data class BookDraft(
    val title: String,
    val author: String,
    val department: Department,
    val category: String,
    val isbn: String,
    val rackNumber: String,
    val totalCopies: Int,
    val availableCopies: Int,
)

/**
 * Library catalog contract, stored at `libraryBooks/{bookId}`.
 *
 * Authority, enforced by firestore.rules:
 *  - Reads: members (STUDENT/TEACHER) see only books of their own
 *    department; the LIBRARY_ADMIN and SUPER_ADMIN see everything.
 *  - Writes: LIBRARY_ADMIN and SUPER_ADMIN only. Members have no write
 *    path at all.
 *
 * Write methods return [Result] so a rules rejection surfaces in the admin
 * UI as an error banner instead of a crash.
 */
interface LibraryRepository {
    /**
     * Streams books. A non-null [BookQuery.department] filters server-side
     * (required for member reads); a null department is the admin's
     * all-departories read.
     */
    fun observeBooks(query: BookQuery = BookQuery()): Flow<Result<List<Book>>>

    /** One-shot fetch of a single book document. */
    suspend fun getBook(bookId: String): Book?

    suspend fun createBook(draft: BookDraft): Result<Unit>

    /** Replaces the stored book wholesale; the ID identifies the target. */
    suspend fun updateBook(book: Book): Result<Unit>

    /** Availability/copy-count quick edit: only [availableCopies] changes. */
    suspend fun setAvailableCopies(bookId: String, availableCopies: Int): Result<Unit>

    suspend fun deleteBook(bookId: String): Result<Unit>
}

/**
 * Client-side query evaluation: the search text matches the title or the
 * author, case-insensitive; the department constraint is applied
 * server-side already. Alphabetical title ordering keeps the list stable.
 */
internal fun List<Book>.applyBookQuery(query: BookQuery): List<Book> {
    val needle = query.search.trim()
    return if (needle.isEmpty()) {
        this
    } else {
        filter { book ->
            book.title.contains(needle, ignoreCase = true) ||
                book.author.contains(needle, ignoreCase = true)
        }
    }.sortedBy { it.title.lowercase() }
}
