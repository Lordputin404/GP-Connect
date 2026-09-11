package com.gumlapolytechnic.gpconnect.ui.faculty

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gumlapolytechnic.gpconnect.data.model.Department
import com.gumlapolytechnic.gpconnect.data.model.Faculty
import com.gumlapolytechnic.gpconnect.data.repository.FacultyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Student faculty list state — always exactly the caller's own department. */
data class FacultyListUiState(
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    /** Null when the caller has no department on their profile. */
    val department: Department? = null,
    val faculty: List<Faculty> = emptyList(),
)

/**
 * Faculty directory of the signed-in student's own department. The department
 * comes from the caller's profile — there is deliberately no way to request
 * another department's faculty from this screen, and the Firestore rules
 * would reject such a read anyway.
 */
class FacultyListViewModel(
    facultyRepository: FacultyRepository,
    callerDepartment: Department?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FacultyListUiState(department = callerDepartment))
    val uiState: StateFlow<FacultyListUiState> = _uiState.asStateFlow()

    init {
        // Without a department there is nothing to list; the rules would
        // reject the query outright.
        callerDepartment?.let { department ->
            viewModelScope.launch {
                facultyRepository.observeFaculty(department).collect { result ->
                    _uiState.value = result.fold(
                        onSuccess = { faculty ->
                            FacultyListUiState(
                                isLoading = false,
                                department = department,
                                faculty = faculty,
                            )
                        },
                        onFailure = {
                            FacultyListUiState(
                                isLoading = false,
                                isError = true,
                                department = department,
                            )
                        },
                    )
                }
            }
        } ?: run {
            _uiState.value = FacultyListUiState(isLoading = false, department = null)
        }
    }
}

/** Faculty detail state, resolved from the list snapshot by id. */
data class FacultyDetailUiState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val isError: Boolean = false,
    val department: Department? = null,
    val faculty: Faculty? = null,
)

/**
 * Streams the faculty list of the caller's own department and selects one
 * member by id. The member is re-resolved on every emission, so a live HOD
 * edit appears immediately; an id that no longer exists shows "not found".
 */
class FacultyDetailViewModel(
    facultyRepository: FacultyRepository,
    callerDepartment: Department?,
    private val facultyId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FacultyDetailUiState(department = callerDepartment))
    val uiState: StateFlow<FacultyDetailUiState> = _uiState.asStateFlow()

    init {
        // A bare `return` is prohibited in an init block; ?.let skips the
        // stream when the caller has no department (rules would reject it).
        callerDepartment?.let { department ->
            viewModelScope.launch {
                facultyRepository.observeFaculty(department).collect { result ->
                    _uiState.value = result.fold(
                        onSuccess = { list ->
                            val faculty = list.firstOrNull { it.id == facultyId }
                            FacultyDetailUiState(
                                isLoading = false,
                                notFound = faculty == null,
                                department = department,
                                faculty = faculty,
                            )
                        },
                        onFailure = {
                            FacultyDetailUiState(
                                isLoading = false,
                                isError = true,
                                department = department,
                            )
                        },
                    )
                }
            }
        } ?: run {
            _uiState.value = FacultyDetailUiState(isLoading = false, department = null)
        }
    }
}
