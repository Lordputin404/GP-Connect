package com.gumlapolytechnic.gpconnect.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gumlapolytechnic.gpconnect.data.model.User
import com.gumlapolytechnic.gpconnect.data.model.UserRole
import com.gumlapolytechnic.gpconnect.data.model.departmentModule
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
        viewModelScope.launch { userRepository.setEnabled(uid, enabled) }
    }

    fun setRole(uid: String, role: UserRole) {
        viewModelScope.launch { userRepository.setRole(uid, role, role.departmentModule) }
    }
}
