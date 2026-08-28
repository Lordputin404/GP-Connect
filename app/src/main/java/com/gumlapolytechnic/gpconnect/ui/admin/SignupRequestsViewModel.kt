package com.gumlapolytechnic.gpconnect.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gumlapolytechnic.gpconnect.data.model.Department
import com.gumlapolytechnic.gpconnect.data.model.SignupRequest
import com.gumlapolytechnic.gpconnect.data.model.User
import com.gumlapolytechnic.gpconnect.data.model.UserRole
import com.gumlapolytechnic.gpconnect.data.model.sortedForInbox
import com.gumlapolytechnic.gpconnect.data.repository.SignupRequestRepository
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

data class SignupRequestsUiState(
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val department: Department? = null,
    val requests: List<SignupRequest> = emptyList(),
    val busyUids: Set<String> = emptySet(),
    val actionFailed: Boolean = false,
)

/**
 * Signup request inbox state. A SUPER_ADMIN sees every request; an HOD sees only
 * their own department's, because the query carries the department filter the
 * Firestore `list` rule demands — a hand-crafted query for another department is
 * rejected by the server, not merely hidden here.
 *
 * The stream yields [Result] so a rules rejection shows as an error state rather
 * than an empty inbox that looks like "no requests".
 */
class SignupRequestsViewModel(
    private val repository: SignupRequestRepository,
    private val adminUser: User,
) : ViewModel() {

    private val isSuperAdmin = adminUser.role == UserRole.SUPER_ADMIN

    /** null for a super admin: the whole college is in scope. */
    private val department: Department? = adminUser.departmentOrNull.takeIf { !isSuperAdmin }

    private val busyUids = MutableStateFlow<Set<String>>(emptySet())
    private val actionFailed = MutableStateFlow(false)
    private val refresh = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val requests: Flow<Result<List<SignupRequest>>> = refresh.flatMapLatest {
        when {
            isSuperAdmin -> repository.observeAllRequests()
            department != null -> repository.observeDepartmentRequests(department)
            // A FACULTY_ADMIN without a department has nothing in scope yet.
            else -> flowOf(Result.success(emptyList()))
        }
    }

    val uiState: StateFlow<SignupRequestsUiState> =
        combine(requests, busyUids, actionFailed) { result, busy, failed ->
            result.fold(
                onSuccess = { list ->
                    SignupRequestsUiState(
                        isLoading = false,
                        department = department,
                        requests = list.sortedForInbox(),
                        busyUids = busy,
                        actionFailed = failed,
                    )
                },
                onFailure = {
                    SignupRequestsUiState(
                        isLoading = false,
                        isError = true,
                        department = department,
                    )
                },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SignupRequestsUiState(department = department),
        )

    fun approve(request: SignupRequest) = decide(request) {
        repository.approve(request, adminUser.id)
    }

    fun reject(request: SignupRequest, note: String?) = decide(request) {
        repository.reject(request, adminUser.id, note?.trim()?.takeIf { it.isNotBlank() })
    }

    fun dismissActionError() {
        actionFailed.value = false
    }

    fun retry() {
        refresh.value += 1
    }

    private fun decide(request: SignupRequest, action: suspend () -> Result<Unit>) {
        if (request.uid in busyUids.value) return
        busyUids.update { it + request.uid }
        actionFailed.value = false
        viewModelScope.launch {
            val result = action()
            busyUids.update { it - request.uid }
            if (result.isFailure) actionFailed.value = true
        }
    }
}
