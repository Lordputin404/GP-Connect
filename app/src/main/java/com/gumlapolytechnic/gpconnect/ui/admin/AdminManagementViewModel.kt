package com.gumlapolytechnic.gpconnect.ui.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gumlapolytechnic.gpconnect.data.model.Department
import com.gumlapolytechnic.gpconnect.data.model.User
import com.gumlapolytechnic.gpconnect.data.model.UserRole
import com.gumlapolytechnic.gpconnect.data.repository.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AdminManagementUiState(
    val isLoading: Boolean = true,
    val admins: List<User> = emptyList(),
    val currentUserId: String = "",
)

/**
 * Admin Management state (SUPER_ADMIN only): lists administrator accounts,
 * enables/disables them and assigns roles. A super admin cannot modify their
 * own account here —Firestore rules enforce the same restriction server-side.
 */
class AdminManagementViewModel(
    private val userRepository: UserRepository,
    currentUserId: String,
) : ViewModel() {

    val uiState: StateFlow<AdminManagementUiState> = userRepository.observeUsers()
        .map { users ->
            AdminManagementUiState(
                isLoading = false,
                admins = users.filter { it.isAdmin }.sortedBy { it.name.lowercase() },
                currentUserId = currentUserId,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AdminManagementUiState(currentUserId = currentUserId),
        )

    fun setEnabled(uid: String, enabled: Boolean) {
        viewModelScope.launch {
            // A rules rejection throws; log it instead of crashing the workspace.
            runCatching { userRepository.setEnabled(uid, enabled) }
                .onFailure { Log.e(TAG, "setEnabled($uid, $enabled) failed", it) }
        }
    }

    /**
     * Assigns an administrator role. [department] is only meaningful for
     * FACULTY_ADMIN (the HOD role) — the repository drops it for every other
     * role so a library or canteen admin can never end up owning a department.
     */
    fun setAdminRole(uid: String, role: UserRole, department: Department?) {
        viewModelScope.launch {
            runCatching { userRepository.setAdminRole(uid, role, department) }
                .onFailure { Log.e(TAG, "setAdminRole($uid, $role, $department) failed", it) }
        }
    }

    private companion object {
        const val TAG = "GPAdminManagement"
    }
}
