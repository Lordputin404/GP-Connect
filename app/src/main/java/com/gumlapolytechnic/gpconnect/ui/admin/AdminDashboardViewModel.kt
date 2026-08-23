package com.gumlapolytechnic.gpconnect.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gumlapolytechnic.gpconnect.data.model.AdminModule
import com.gumlapolytechnic.gpconnect.data.model.Notice
import com.gumlapolytechnic.gpconnect.data.model.User
import com.gumlapolytechnic.gpconnect.data.model.UserRole
import com.gumlapolytechnic.gpconnect.data.repository.NoticeQuery
import com.gumlapolytechnic.gpconnect.data.repository.NoticeRepository
import com.gumlapolytechnic.gpconnect.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AdminDashboardUiState(
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val isSuperAdmin: Boolean = false,
    val module: AdminModule? = null,
    val totalNotices: Int = 0,
    val pinnedNotices: Int = 0,
    val totalUsers: Int = 0,
    val totalAdmins: Int = 0,
    val enabledAdmins: Int = 0,
    val disabledAdmins: Int = 0,
    val noticesByModule: Map<AdminModule, Int> = emptyMap(),
    val notices: List<Notice> = emptyList(),
)

/**
 * Role-aware admin dashboard state. SUPER_ADMIN sees global counts (users,
 * admins, notices by module, all notices); department admins see only their
 * own module's notices and counts. All mutations go through the repository so
 * Firestore security rules remain the authority.
 */
class AdminDashboardViewModel(
    private val adminUser: User,
    private val noticeRepository: NoticeRepository,
    userRepository: UserRepository,
) : ViewModel() {

    private val refresh = MutableStateFlow(0)

    private val uiState: StateFlow<AdminDashboardUiState> =
        if (adminUser.role == UserRole.SUPER_ADMIN) {
            combine(
                noticeRepository.observeNotices(),
                userRepository.observeUsers(),
                refresh,
            ) { notices, users, _ ->
                buildSuperState(notices, users)
            }
        } else {
            combine(
                noticeRepository.observeNotices(NoticeQuery(module = adminUser.module)),
                refresh,
            ) { notices, _ ->
                buildDepartmentState(notices)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AdminDashboardUiState(
                isSuperAdmin = adminUser.role == UserRole.SUPER_ADMIN,
                module = adminUser.module,
            ),
        )

    val state: StateFlow<AdminDashboardUiState> = uiState

    fun deleteNotice(noticeId: String) {
        viewModelScope.launch { noticeRepository.deleteNotice(noticeId) }
    }

    fun togglePinned(notice: Notice) {
        viewModelScope.launch { noticeRepository.setPinned(notice.id, !notice.isPinned) }
    }

    fun retry() {
        refresh.value += 1
    }

    private fun buildSuperState(notices: List<Notice>, users: List<User>) =
        AdminDashboardUiState(
            isLoading = false,
            isSuperAdmin = true,
            totalNotices = notices.size,
            pinnedNotices = notices.count { it.isPinned },
            totalUsers = users.size,
            totalAdmins = users.count { it.isAdmin },
            enabledAdmins = users.count { it.isAdmin && it.enabled },
            disabledAdmins = users.count { it.isAdmin && !it.enabled },
            noticesByModule = AdminModule.entries.associateWith { module ->
                notices.count { it.module == module }
            },
            notices = notices,
        )

    private fun buildDepartmentState(notices: List<Notice>) =
        AdminDashboardUiState(
            isLoading = false,
            isSuperAdmin = false,
            module = adminUser.module,
            totalNotices = notices.size,
            pinnedNotices = notices.count { it.isPinned },
            notices = notices,
        )
}
