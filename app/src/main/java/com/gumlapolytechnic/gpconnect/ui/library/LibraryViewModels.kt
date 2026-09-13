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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Student library list state — always exactly the caller's own department. */
data class LibraryUiState(
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    /** Null when the caller has no department on their profile. */
    val department: Department? = null,
    val search: String = "",
    val books: List<Book> = emptyList(),
) {
    val isFiltered: Boolean get() = search.isNotBlank()
}

/**
 * Library catalog of the signed-in member's own department. The department
 * comes from the caller's profile — there is deliberately no department
 * selector, and the Firestore rules would reject a cross-department query
 * anyway. Search text matches title/author on the client.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(
    libraryRepository: LibraryRepository,
    callerDepartment: Department?,
) : ViewModel() {

    private val search = MutableStateFlow("")

    private val books = search.flatMapLatest { searchText ->
        if (callerDepartment != null) {
            libraryRepository.observeBooks(
                BookQuery(department = callerDepartment, search = searchText),
            )
        } else {
            // Without a department there is nothing to list; the rules
            // would reject the query outright. Keep the flow idle.
            flowOf(Result.success(emptyList<Book>()))
        }
    }

    val uiState: StateFlow<LibraryUiState> =
        combine(books, search) { result, searchText ->
            result.fold(
                onSuccess = { list ->
                    LibraryUiState(
                        isLoading = false,
                        department = callerDepartment,
                        search = searchText,
                        books = list,
                    )
                },
                onFailure = {
                    LibraryUiState(
                        isLoading = false,
                        isError = true,
                        department = callerDepartment,
                        search = searchText,
                    )
                },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LibraryUiState(department = callerDepartment),
        )

    fun onSearchChange(value: String) {
        search.value = value
    }
}

/** Library book detail state, resolved from the list snapshot by id. */
data class LibraryBookDetailUiState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val isError: Boolean = false,
    val department: Department? = null,
    val book: Book? = null,
)

/**
 * Streams the book list of the caller's own department and selects one
 * book by id. The book is re-resolved on every emission, so a live library
 * admin edit appears immediately; an id that no longer exists (or belongs
 * to another department) shows "not found".
 */
class LibraryBookDetailViewModel(
    libraryRepository: LibraryRepository,
    callerDepartment: Department?,
    private val bookId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryBookDetailUiState(department = callerDepartment))
    val uiState: StateFlow<LibraryBookDetailUiState> = _uiState.asStateFlow()

    init {
        // A bare `return` is prohibited in an init block; ?.let skips the
        // stream when the caller has no department (rules would reject it).
        callerDepartment?.let { department ->
            viewModelScope.launch {
                libraryRepository.observeBooks(BookQuery(department = department))
                    .collect { result ->
                        _uiState.value = result.fold(
                            onSuccess = { list ->
                                val book = list.firstOrNull { it.id == bookId }
                                LibraryBookDetailUiState(
                                    isLoading = false,
                                    notFound = book == null,
                                    department = department,
                                    book = book,
                                )
                            },
                            onFailure = {
                                LibraryBookDetailUiState(
                                    isLoading = false,
                                    isError = true,
                                    department = department,
                                )
                            },
                        )
                    }
            }
        } ?: run {
            _uiState.value = LibraryBookDetailUiState(isLoading = false, department = null)
        }
    }
}
