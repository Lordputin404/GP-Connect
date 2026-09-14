package com.gumlapolytechnic.gpconnect.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gumlapolytechnic.gpconnect.data.model.Book
import com.gumlapolytechnic.gpconnect.data.model.Department
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
import kotlinx.coroutines.launch

/** Student library list state — the college-wide catalog with filters. */
data class LibraryUiState(
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    /** Selected department filter; null is the All filter. */
    val selectedDepartment: Department? = null,
    val search: String = "",
    val books: List<Book> = emptyList(),
) {
    val isFiltered: Boolean get() = search.isNotBlank() || selectedDepartment != null
}

/**
 * Library catalog for every enabled member — college-wide, not scoped to
 * the caller's own department. The department filter and the search text
 * apply together: the department filters the Firestore query server-side,
 * the search text matches title/author on the client.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(
    libraryRepository: LibraryRepository,
) : ViewModel() {

    private val search = MutableStateFlow("")
    private val department = MutableStateFlow<Department?>(null)

    private val books = combine(search, department) { querySearch, queryDepartment ->
        BookQuery(department = queryDepartment, search = querySearch)
    }.flatMapLatest { query ->
        libraryRepository.observeBooks(query)
    }

    val uiState: StateFlow<LibraryUiState> =
        combine(books, search, department) { result, searchText, selectedDepartment ->
            result.fold(
                onSuccess = { list ->
                    LibraryUiState(
                        isLoading = false,
                        selectedDepartment = selectedDepartment,
                        search = searchText,
                        books = list,
                    )
                },
                onFailure = {
                    LibraryUiState(
                        isLoading = false,
                        isError = true,
                        selectedDepartment = selectedDepartment,
                        search = searchText,
                    )
                },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LibraryUiState(),
        )

    fun onSearchChange(value: String) {
        search.value = value
    }

    fun onDepartmentChange(value: Department?) {
        department.value = value
    }
}

/** Library book detail state, resolved from the list snapshot by id. */
data class LibraryBookDetailUiState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val isError: Boolean = false,
    val book: Book? = null,
)

/**
 * Streams the college-wide book list and selects one book by id. The book
 * is re-resolved on every emission, so a live library admin edit appears
 * immediately; an id that no longer exists shows "not found".
 */
class LibraryBookDetailViewModel(
    libraryRepository: LibraryRepository,
    private val bookId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryBookDetailUiState())
    val uiState: StateFlow<LibraryBookDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            libraryRepository.observeBooks(BookQuery())
                .collect { result ->
                    _uiState.value = result.fold(
                        onSuccess = { list ->
                            val book = list.firstOrNull { it.id == bookId }
                            LibraryBookDetailUiState(
                                isLoading = false,
                                notFound = book == null,
                                book = book,
                            )
                        },
                        onFailure = {
                            LibraryBookDetailUiState(isLoading = false, isError = true)
                        },
                    )
                }
        }
    }
}
