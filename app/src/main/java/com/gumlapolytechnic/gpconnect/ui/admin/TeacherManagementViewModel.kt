package com.gumlapolytechnic.gpconnect.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gumlapolytechnic.gpconnect.data.model.Department
import com.gumlapolytechnic.gpconnect.data.model.User
import com.gumlapolytechnic.gpconnect.data.model.UserRole
import com.gumlapolytechnic.gpconnect.data.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TeacherManagementUiState(
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val hasDepartment: Boolean = true,
    val department: Department? = null,
    val teachers: List<User> = emptyList(),
    val students: List<User> = emptyList(),
    val busyUids: Set<String> = emptySet(),
    val actionFailed: Boolean = false,
)

/**
 * Teacher management for one department. The HOD may promote a student in their
 * own department to TEACHER, return a teacher to STUDENT, and enable or disable
 * either — nothing else. The department itself is never part of the write, so a
 * member cannot be moved between departments, and the roster query carries the
 * department filter the Firestore `list` rule requires.
 */
class TeacherManagementViewModel(
    private val userRepository: UserRepository,
    private val department: Department?,
) : ViewModel() {

    private val busyUids = MutableStateFlow<Set<String>>(emptySet())
    private val actionFailed = MutableStateFlow(false)
    private val refresh = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val members: Flow<Result<List<User>>> = refresh.flatMapLatest {
        if (department == null) {
            flowOf(Result.success(emptyList()))
        } else {
            userRepository.observeDepartmentMembers(department)
        }
    }

    val uiState: StateFlow<TeacherManagementUiState> =
        combine(members, busyUids, actionFailed) { result, busy, failed ->
            result.fold(
                onSuccess = { people ->
                    TeacherManagementUiState(
                        isLoading = false,
                        hasDepartment = department != null,
                        department = department,
                        teachers = people.filter { it.role == UserRole.TEACHER },
                        students = people.filter { it.role == UserRole.STUDENT },
                        busyUids = busy,
                        actionFailed = failed,
                    )
                },
                onFailure = {
                    TeacherManagementUiState(
                        isLoading = false,
                        isError = true,
                        hasDepartment = department != null,
                        department = department,
                    )
                },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TeacherManagementUiState(
                hasDepartment = department != null,
                department = department,
            ),
        )

    fun promoteToTeacher(user: User) = act(user.id) {
        userRepository.setMemberRole(user.id, UserRole.TEACHER)
    }

    fun demoteToStudent(user: User) = act(user.id) {
        userRepository.setMemberRole(user.id, UserRole.STUDENT)
    }

    fun setEnabled(user: User, enabled: Boolean) = act(user.id) {
        userRepository.setEnabled(user.id, enabled)
    }

    fun dismissActionError() {
        actionFailed.value = false
    }

    fun retry() {
        refresh.value += 1
    }

    private fun act(uid: String, action: suspend () -> Unit) {
        if (uid in busyUids.value) return
        busyUids.update { it + uid }
        actionFailed.value = false
        viewModelScope.launch {
            // The repository throws when the rules reject the write.
            val result = runCatching { action() }
            busyUids.update { it - uid }
            if (result.isFailure) actionFailed.value = true
        }
    }
}
