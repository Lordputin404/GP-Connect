package com.gumlapolytechnic.gpconnect.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gumlapolytechnic.gpconnect.data.model.Department
import com.gumlapolytechnic.gpconnect.data.model.Faculty
import com.gumlapolytechnic.gpconnect.data.repository.FacultyDraft
import com.gumlapolytechnic.gpconnect.data.repository.FacultyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FacultyManagementUiState(
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val hasDepartment: Boolean = true,
    val department: Department? = null,
    val faculty: List<Faculty> = emptyList(),
    val busyIds: Set<String> = emptySet(),
    val actionFailed: Boolean = false,
)

/**
 * HOD's faculty management state for exactly their assigned department: the
 * live faculty list plus delete mutations. Every write goes through the
 * repository, and the Firestore rules re-check the HOD's department
 * server-side — this UI merely surfaces a rejection as an error banner.
 */
class FacultyManagementViewModel(
    private val facultyRepository: FacultyRepository,
    private val department: Department?,
) : ViewModel() {

    private val busyIds = MutableStateFlow<Set<String>>(emptySet())
    private val actionFailed = MutableStateFlow(false)
    private val _uiState = MutableStateFlow(
        FacultyManagementUiState(hasDepartment = department != null, department = department),
    )
    val uiState: StateFlow<FacultyManagementUiState> = _uiState.asStateFlow()

    init {
        // No department bound: nothing to list (the rules would reject it).
        department?.let { bound ->
            viewModelScope.launch {
                facultyRepository.observeFaculty(bound).collect { result ->
                    _uiState.update { state ->
                        result.fold(
                            onSuccess = { list ->
                                state.copy(isLoading = false, isError = false, faculty = list)
                            },
                            onFailure = {
                                state.copy(isLoading = false, isError = true)
                            },
                        )
                    }
                }
            }
        } ?: run {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun deleteFaculty(member: Faculty) {
        val boundDepartment = department ?: return
        act(member.id) { facultyRepository.deleteFaculty(boundDepartment, member.id) }
    }

    fun dismissActionError() {
        actionFailed.value = false
        _uiState.update { it.copy(actionFailed = false) }
    }

    private fun act(id: String, action: suspend () -> Result<Unit>) {
        if (id in busyIds.value) return
        busyIds.update { it + id }
        _uiState.update { it.copy(busyIds = busyIds.value, actionFailed = false) }
        viewModelScope.launch {
            val result = action()
            busyIds.update { it - id }
            _uiState.update {
                it.copy(
                    busyIds = busyIds.value,
                    actionFailed = result.isFailure,
                )
            }
        }
    }
}

data class FacultyFormUiState(
    val isLoading: Boolean = false,
    val notFound: Boolean = false,
    val isEditMode: Boolean = false,
    val name: String = "",
    val designation: String = "",
    val roomNumber: String = "",
    val email: String = "",
    val phone: String = "",
    val nameError: Boolean = false,
    val designationError: Boolean = false,
    val roomNumberError: Boolean = false,
    val isSaving: Boolean = false,
    val saveError: Boolean = false,
    val saved: Boolean = false,
)

/**
 * HOD's faculty add/edit form (the AdminCalendarEventFormViewModel pattern).
 * Only the HOD's own department is ever written — the repository call takes
 * the department from the caller's profile, never from form input. Name,
 * designation and room number are required; email and phone are optional.
 */
class FacultyFormViewModel(
    private val facultyRepository: FacultyRepository,
    private val department: Department?,
    private val editFacultyId: String?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        FacultyFormUiState(
            isLoading = department != null && editFacultyId != null,
            isEditMode = editFacultyId != null,
        ),
    )
    val uiState: StateFlow<FacultyFormUiState> = _uiState.asStateFlow()

    init {
        // No bare `return` in an init block: the preload only runs when both
        // a bound department and an edit id exist (create mode needs neither).
        val boundDepartment = department
        val id = editFacultyId
        if (boundDepartment != null && id != null) {
            viewModelScope.launch {
                val member = facultyRepository.getFaculty(boundDepartment, id)
                _uiState.update { state ->
                    if (member == null) {
                        state.copy(isLoading = false, notFound = true)
                    } else {
                        state.copy(
                            isLoading = false,
                            name = member.name,
                            designation = member.designation,
                            roomNumber = member.roomNumber,
                            email = member.email,
                            phone = member.phone,
                        )
                    }
                }
            }
        }
    }

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value, nameError = false) }
    }

    fun onDesignationChange(value: String) {
        _uiState.update { it.copy(designation = value, designationError = false) }
    }

    fun onRoomNumberChange(value: String) {
        _uiState.update { it.copy(roomNumber = value, roomNumberError = false) }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value) }
    }

    fun onPhoneChange(value: String) {
        _uiState.update { it.copy(phone = value) }
    }

    fun save() {
        val state = _uiState.value
        val nameError = state.name.isBlank()
        val designationError = state.designation.isBlank()
        val roomNumberError = state.roomNumber.isBlank()
        if (nameError || designationError || roomNumberError) {
            _uiState.update {
                it.copy(
                    nameError = nameError,
                    designationError = designationError,
                    roomNumberError = roomNumberError,
                )
            }
            return
        }
        val boundDepartment = department ?: return

        _uiState.update { it.copy(isSaving = true, saveError = false) }
        viewModelScope.launch {
            val succeeded = if (editFacultyId == null) {
                facultyRepository.createFaculty(
                    boundDepartment,
                    FacultyDraft(
                        name = state.name.trim(),
                        designation = state.designation.trim(),
                        roomNumber = state.roomNumber.trim(),
                        email = state.email.trim(),
                        phone = state.phone.trim(),
                    ),
                ).isSuccess
            } else {
                val existing = facultyRepository.getFaculty(boundDepartment, editFacultyId)
                if (existing != null) {
                    facultyRepository.updateFaculty(
                        boundDepartment,
                        existing.copy(
                            name = state.name.trim(),
                            designation = state.designation.trim(),
                            roomNumber = state.roomNumber.trim(),
                            email = state.email.trim(),
                            phone = state.phone.trim(),
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
