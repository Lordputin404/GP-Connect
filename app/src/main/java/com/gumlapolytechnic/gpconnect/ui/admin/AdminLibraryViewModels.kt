package com.gumlapolytechnic.gpconnect.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gumlapolytechnic.gpconnect.data.model.Book
import com.gumlapolytechnic.gpconnect.data.model.Department
import com.gumlapolytechnic.gpconnect.data.repository.BookDraft
import com.gumlapolytechnic.gpconnect.data.repository.BookQuery
import com.gumlapolytechnic.gpconnect.data.repository.LibraryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminLibraryUiState(
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val search: String = "",
    val department: Department? = null,
    val books: List<Book> = emptyList(),
    val busyIds: Set<String> = emptySet(),
    val actionFailed: Boolean = false,
) {
    val isFiltered: Boolean get() = search.isNotBlank() || department != null
}

/**
 * Library management state for the LIBRARY_ADMIN (and SUPER_ADMIN): the
 * catalog across **all** departments, plus delete mutations. Department
 * filter and search both apply together; writes go through the repository
 * and return [Result] — the Firestore rules reject any other caller, which
 * this UI simply surfaces as an error banner.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AdminLibraryViewModel(
    private val libraryRepository: LibraryRepository,
) : ViewModel() {

    private val search = MutableStateFlow("")
    private val department = MutableStateFlow<Department?>(null)
    private val refresh = MutableStateFlow(0)
    private val busyIds = MutableStateFlow<Set<String>>(emptySet())
    private val actionFailed = MutableStateFlow(false)

    private val books = combine(search, department, refresh) { querySearch, queryDepartment, _ ->
        BookQuery(department = queryDepartment, search = querySearch)
    }.flatMapLatest { query ->
        libraryRepository.observeBooks(query)
    }

    val uiState: StateFlow<AdminLibraryUiState> =
        combine(books, busyIds, actionFailed) { result, busy, failed ->
            result.fold(
                onSuccess = { list ->
                    AdminLibraryUiState(
                        isLoading = false,
                        search = search.value,
                        department = department.value,
                        books = list,
                        busyIds = busy,
                        actionFailed = failed,
                    )
                },
                onFailure = {
                    AdminLibraryUiState(isLoading = false, isError = true)
                },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AdminLibraryUiState(),
        )

    fun onSearchChange(value: String) {
        search.value = value
    }

    fun onDepartmentChange(value: Department?) {
        department.value = value
    }

    fun deleteBook(book: Book) = act(book.id) {
        libraryRepository.deleteBook(book.id)
    }

    /** Availability/copy-count quick edit from the management list. */
    fun setAvailableCopies(book: Book, availableCopies: Int) = act(book.id) {
        libraryRepository.setAvailableCopies(book.id, availableCopies)
    }

    fun dismissActionError() {
        actionFailed.value = false
    }

    fun retry() {
        refresh.value += 1
    }

    private fun act(id: String, action: suspend () -> Result<Unit>) {
        if (id in busyIds.value) return
        busyIds.update { it + id }
        actionFailed.value = false
        viewModelScope.launch {
            val result = action()
            busyIds.update { it - id }
            if (result.isFailure) actionFailed.value = true
        }
    }
}

data class AdminBookFormUiState(
    val isLoading: Boolean = false,
    val notFound: Boolean = false,
    val isEditMode: Boolean = false,
    val title: String = "",
    val author: String = "",
    val department: Department? = null,
    val category: String = "",
    val isbn: String = "",
    val rackNumber: String = "",
    val totalCopiesText: String = "",
    val availableCopiesText: String = "",
    val titleError: Boolean = false,
    val authorError: Boolean = false,
    val departmentError: Boolean = false,
    val copiesError: Boolean = false,
    val isSaving: Boolean = false,
    val saveError: Boolean = false,
    val saved: Boolean = false,
)

/**
 * Book create/edit form state (the AdminCalendarEventFormViewModel pattern).
 * Create mode files a [BookDraft]; edit mode preloads by ID, preserves
 * id/createdAt, and updates in place. The department is chosen from the
 * canonical Department enum — there is no free-text department input. The
 * Firestore rules allow only the LIBRARY_ADMIN and SUPER_ADMIN to write
 * libraryBooks — a rejected save surfaces as saveError, never a crash.
 */
class AdminBookFormViewModel(
    private val libraryRepository: LibraryRepository,
    private val editBookId: String?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AdminBookFormUiState(
            isLoading = editBookId != null,
            isEditMode = editBookId != null,
        ),
    )
    val uiState: StateFlow<AdminBookFormUiState> = _uiState.asStateFlow()

    init {
        val id = editBookId
        if (id != null) {
            viewModelScope.launch {
                val book = libraryRepository.getBook(id)
                _uiState.update { state ->
                    if (book == null) {
                        state.copy(isLoading = false, notFound = true)
                    } else {
                        state.copy(
                            isLoading = false,
                            title = book.title,
                            author = book.author,
                            department = book.departmentOrNull,
                            category = book.category,
                            isbn = book.isbn,
                            rackNumber = book.rackNumber,
                            totalCopiesText = if (book.totalCopies > 0) book.totalCopies.toString() else "",
                            availableCopiesText =
                                if (book.availableCopies > 0) book.availableCopies.toString() else "",
                        )
                    }
                }
            }
        }
    }

    fun onTitleChange(value: String) {
        _uiState.update { it.copy(title = value, titleError = false) }
    }

    fun onAuthorChange(value: String) {
        _uiState.update { it.copy(author = value, authorError = false) }
    }

    fun onDepartmentChange(value: Department) {
        _uiState.update { it.copy(department = value, departmentError = false) }
    }

    fun onCategoryChange(value: String) {
        _uiState.update { it.copy(category = value) }
    }

    fun onIsbnChange(value: String) {
        _uiState.update { it.copy(isbn = value) }
    }

    fun onRackNumberChange(value: String) {
        _uiState.update { it.copy(rackNumber = value) }
    }

    fun onTotalCopiesChange(value: String) {
        _uiState.update { it.copy(totalCopiesText = value.filter { it.isDigit() }, copiesError = false) }
    }

    fun onAvailableCopiesChange(value: String) {
        _uiState.update { it.copy(availableCopiesText = value.filter { it.isDigit() }, copiesError = false) }
    }

    fun save() {
        val state = _uiState.value
        val totalCopies = state.totalCopiesText.toIntOrNull() ?: 0
        val availableCopies = state.availableCopiesText.toIntOrNull() ?: 0
        val titleError = state.title.isBlank()
        val authorError = state.author.isBlank()
        val departmentError = state.department == null
        val copiesError = totalCopies < 1 || availableCopies < 0 || availableCopies > totalCopies
        if (titleError || authorError || departmentError || copiesError) {
            _uiState.update {
                it.copy(
                    titleError = titleError,
                    authorError = authorError,
                    departmentError = departmentError,
                    copiesError = copiesError,
                )
            }
            return
        }

        val department = state.department!!
        _uiState.update {
            it.copy(isSaving = true, titleError = false, authorError = false, departmentError = false, copiesError = false, saveError = false)
        }
        viewModelScope.launch {
            val succeeded = if (editBookId == null) {
                libraryRepository.createBook(
                    BookDraft(
                        title = state.title.trim(),
                        author = state.author.trim(),
                        department = department,
                        category = state.category.trim(),
                        isbn = state.isbn.trim(),
                        rackNumber = state.rackNumber.trim(),
                        totalCopies = totalCopies,
                        availableCopies = availableCopies,
                    ),
                ).isSuccess
            } else {
                val existing = libraryRepository.getBook(editBookId)
                if (existing != null) {
                    libraryRepository.updateBook(
                        existing.copy(
                            title = state.title.trim(),
                            author = state.author.trim(),
                            departmentId = department.id,
                            category = state.category.trim(),
                            isbn = state.isbn.trim(),
                            rackNumber = state.rackNumber.trim(),
                            totalCopies = totalCopies,
                            availableCopies = availableCopies,
                        ),
                    ).isSuccess
                } else {
                    false
                }
            }
            _uiState.update {
                if (succeeded) {
                    it.copy(isSaving = false, saved = true)
                } else {
                    it.copy(isSaving = false, saveError = true)
                }
            }
        }
    }
}
