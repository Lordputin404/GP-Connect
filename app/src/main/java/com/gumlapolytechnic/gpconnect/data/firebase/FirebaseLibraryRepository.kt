package com.gumlapolytechnic.gpconnect.data.firebase

import android.util.Log
import com.gumlapolytechnic.gpconnect.data.model.Book
import com.gumlapolytechnic.gpconnect.data.model.Department
import com.gumlapolytechnic.gpconnect.data.repository.BookDraft
import com.gumlapolytechnic.gpconnect.data.repository.BookQuery
import com.gumlapolytechnic.gpconnect.data.repository.LibraryRepository
import com.gumlapolytechnic.gpconnect.data.repository.applyBookQuery
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Firestore-backed library catalog. Mirrors the calendar repository: a
 * single collection listener per query with client-side search/ordering
 * ([applyBookQuery]) so no composite index is needed. Member reads carry a
 * `departmentId == own department` constraint — the rules reject any member
 * query without it — while the LIBRARY_ADMIN/SUPER_ADMIN listen to the whole
 * collection. Writes return [Result] so a rules rejection shows as an admin
 * error state rather than a crash.
 */
class FirebaseLibraryRepository : LibraryRepository {

    private val firestore get() = FirebaseServices.firestore

    override fun observeBooks(query: BookQuery): Flow<Result<List<Book>>> =
        callbackFlow {
            // Department-scoped reads filter server-side: the rules `list`
            // grants members only their own department's books, so carrying
            // the constraint in the query keeps member reads exact and makes
            // a rules misconfiguration visible as an error flow.
            val collection = if (query.department != null) {
                firestore.collection(LIBRARY_BOOKS)
                    .whereEqualTo("departmentId", query.department.id)
            } else {
                firestore.collection(LIBRARY_BOOKS)
            }
            val registration: ListenerRegistration =
                collection.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(
                            TAG,
                            "observeBooks failed: code=${error.code} message=${error.message}",
                            error,
                        )
                        trySend(Result.failure(error))
                        return@addSnapshotListener
                    }
                    val books = snapshot?.documents
                        ?.mapNotNull { it.toBook() }
                        .orEmpty()
                        .applyBookQuery(query)
                    trySend(Result.success(books))
                }
            awaitClose { registration.remove() }
        }

    override suspend fun getBook(bookId: String): Book? =
        firestore.collection(LIBRARY_BOOKS).document(bookId).get().awaitTask()?.toBook()

    override suspend fun createBook(draft: BookDraft): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        firestore.collection(LIBRARY_BOOKS)
            .add(
                bookFields(
                    title = draft.title,
                    author = draft.author,
                    department = draft.department,
                    category = draft.category,
                    isbn = draft.isbn,
                    rackNumber = draft.rackNumber,
                    totalCopies = draft.totalCopies,
                    availableCopies = draft.availableCopies,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            .awaitTask()
        Log.i(TAG, "Created book '${draft.title}' (${draft.department.id})")
    }.onFailure { failure ->
        Log.e(TAG, "createBook failed: ${failure.message}", failure)
    }.map { }

    override suspend fun updateBook(book: Book): Result<Unit> = runCatching {
        val department = book.departmentOrNull ?: error("Book ${book.id} has no resolvable department")
        firestore.collection(LIBRARY_BOOKS).document(book.id)
            .set(
                bookFields(
                    title = book.title,
                    author = book.author,
                    department = department,
                    category = book.category,
                    isbn = book.isbn,
                    rackNumber = book.rackNumber,
                    totalCopies = book.totalCopies,
                    availableCopies = book.availableCopies,
                    createdAt = book.createdAt,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            .awaitTask()
        Log.i(TAG, "Updated book '${book.title}' (${book.id})")
    }.onFailure { failure ->
        Log.e(TAG, "updateBook(${book.id}) failed: ${failure.message}", failure)
    }.map { }

    override suspend fun setAvailableCopies(bookId: String, availableCopies: Int): Result<Unit> =
        runCatching {
            firestore.collection(LIBRARY_BOOKS).document(bookId)
                .update("availableCopies", availableCopies, "updatedAt", System.currentTimeMillis())
                .awaitTask()
            Log.i(TAG, "Set availableCopies=$availableCopies on book $bookId")
        }.onFailure { failure ->
            Log.e(TAG, "setAvailableCopies($bookId) failed: ${failure.message}", failure)
        }.map { }

    override suspend fun deleteBook(bookId: String): Result<Unit> = runCatching {
        firestore.collection(LIBRARY_BOOKS).document(bookId).delete().awaitTask()
        Log.i(TAG, "Deleted book $bookId")
    }.onFailure { failure ->
        Log.e(TAG, "deleteBook($bookId) failed: ${failure.message}", failure)
    }.map { }

    private companion object {
        const val TAG = "GPFirebaseLibrary"
        const val LIBRARY_BOOKS = "libraryBooks"
    }
}

/**
 * All fields written to one book document. `departmentId` is the canonical
 * Department enum name — the same registry as `users.department`, never a
 * display label. Timestamps are epoch-millisecond longs, matching the
 * Notice/Calendar pattern.
 */
internal fun bookFields(
    title: String,
    author: String,
    department: Department,
    category: String,
    isbn: String,
    rackNumber: String,
    totalCopies: Int,
    availableCopies: Int,
    createdAt: Long,
    updatedAt: Long,
): Map<String, Any?> = mapOf(
    "title" to title.trim(),
    "author" to author.trim(),
    "departmentId" to department.id,
    "category" to category.trim(),
    "isbn" to isbn.trim(),
    "rackNumber" to rackNumber.trim(),
    "totalCopies" to totalCopies,
    "availableCopies" to availableCopies,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt,
)

/** Defensive parse of `libraryBooks/{bookId}`. */
internal fun com.google.firebase.firestore.DocumentSnapshot.toBook(): Book? {
    if (!exists()) return null
    return Book(
        id = id,
        title = getString("title").orEmpty(),
        author = getString("author").orEmpty(),
        departmentId = getString("departmentId").orEmpty(),
        category = getString("category").orEmpty(),
        isbn = getString("isbn").orEmpty(),
        rackNumber = getString("rackNumber").orEmpty(),
        totalCopies = getLong("totalCopies")?.toInt() ?: 0,
        availableCopies = getLong("availableCopies")?.toInt() ?: 0,
        createdAt = getLong("createdAt") ?: 0L,
        updatedAt = getLong("updatedAt") ?: 0L,
    )
}
