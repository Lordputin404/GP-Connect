package com.gumlapolytechnic.gpconnect.ui.departments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gumlapolytechnic.gpconnect.data.model.Department
import com.gumlapolytechnic.gpconnect.data.model.DepartmentInfo
import com.gumlapolytechnic.gpconnect.data.repository.DepartmentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Departments list state. The department registry itself is the [Department]
 * enum (no Firestore list query needed and no mock data), while each row's
 * info document is streamed live so an HOD edit appears immediately.
 */
class DepartmentsViewModel(
    departmentRepository: DepartmentRepository,
) : ViewModel() {

    /** One live info stream per department, keyed by canonical id. */
    private val infos = Department.entries.associateWith { department ->
        departmentRepository.observeDepartmentInfo(department)
    }

    val uiState: StateFlow<Map<Department, Result<DepartmentInfo?>>> =
        combine(*infos.values.toList().toTypedArray()) { values ->
            Department.entries.zip(values.toList()).toMap()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Department.entries.associateWith { Result.success(null) },
        )
}

/** Department detail screen state: the department's info document. */
data class DepartmentDetailUiState(
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val department: Department? = null,
    val info: DepartmentInfo? = null,
)

/**
 * Streams one department's info document. The HOD's name rides on that
 * document (hodName, stamped by the HOD when saving — students cannot read
 * the users collection), so a single stream carries everything the screen
 * shows. A missing info document is "not available", not an error.
 */
class DepartmentDetailViewModel(
    private val departmentRepository: DepartmentRepository,
    private val departmentId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DepartmentDetailUiState())
    val uiState: StateFlow<DepartmentDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val department = Department.fromIdOrNull(departmentId)
            if (department == null) {
                _uiState.value = DepartmentDetailUiState(isLoading = false)
                return@launch
            }
            departmentRepository.observeDepartmentInfo(department)
                .collect { result ->
                    _uiState.value = result.fold(
                        onSuccess = { info ->
                            DepartmentDetailUiState(
                                isLoading = false,
                                isError = false,
                                department = department,
                                info = info,
                            )
                        },
                        onFailure = {
                            DepartmentDetailUiState(
                                isLoading = false,
                                isError = true,
                                department = department,
                            )
                        },
                    )
                }
        }
    }
}
