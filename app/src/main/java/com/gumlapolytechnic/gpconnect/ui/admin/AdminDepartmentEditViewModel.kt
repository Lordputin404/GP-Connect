package com.gumlapolytechnic.gpconnect.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gumlapolytechnic.gpconnect.data.model.Department
import com.gumlapolytechnic.gpconnect.data.model.DepartmentInfo
import com.gumlapolytechnic.gpconnect.data.repository.DepartmentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DepartmentEditUiState(
    val isLoading: Boolean = true,
    /** Null when the caller is not an HOD bound to a department. */
    val department: Department? = null,
    val about: String = "",
    val officeRoom: String = "",
    val contact: String = "",
    val isSaving: Boolean = false,
    val saveError: Boolean = false,
    val saved: Boolean = false,
)

/**
 * HOD's "my department information" form: about, office room, contact. Only
 * the caller's own department (the one on their profile) is ever loaded or
 * written — the repository method takes no department argument from the UI,
 * so a HOD cannot express any other department. The Firestore rules enforce
 * the same restriction server-side.
 *
 * [hodName] is the HOD's own display name; it is stamped onto the info
 * document on save so students can see who heads the department (students
 * cannot read the users collection). The rules require the value to equal
 * the saving HOD's profile displayName, so it cannot be spoofed.
 */
class AdminDepartmentEditViewModel(
    private val departmentRepository: DepartmentRepository,
    hodDepartment: Department?,
    private val hodName: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DepartmentEditUiState(isLoading = hodDepartment != null, department = hodDepartment),
    )
    val uiState: StateFlow<DepartmentEditUiState> = _uiState.asStateFlow()

    init {
        // A bare `return` is prohibited in an init block; ?.let preserves the
        // same skip: without a bound department there is nothing to prefill.
        hodDepartment?.let { department ->
            viewModelScope.launch {
                // One-shot load (the admin-form pattern): after prefill, the
                // fields belong to the editor — a later snapshot replay must
                // never clobber in-progress typing.
                val loaded = departmentRepository.observeDepartmentInfo(department)
                    .first()
                    .getOrNull()
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        about = loaded?.about.orEmpty(),
                        officeRoom = loaded?.officeRoom.orEmpty(),
                        contact = loaded?.contact.orEmpty(),
                    )
                }
            }
        }
    }

    fun onAboutChange(value: String) {
        _uiState.update { it.copy(about = value) }
    }

    fun onOfficeRoomChange(value: String) {
        _uiState.update { it.copy(officeRoom = value) }
    }

    fun onContactChange(value: String) {
        _uiState.update { it.copy(contact = value) }
    }

    fun save() {
        val state = _uiState.value
        val department = state.department ?: return
        _uiState.update { it.copy(isSaving = true, saveError = false) }
        viewModelScope.launch {
            val succeeded = departmentRepository.saveDepartmentInfo(
                department,
                DepartmentInfo(
                    departmentId = department.id,
                    about = state.about,
                    officeRoom = state.officeRoom,
                    contact = state.contact,
                ),
                hodName = hodName,
            ).isSuccess
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
