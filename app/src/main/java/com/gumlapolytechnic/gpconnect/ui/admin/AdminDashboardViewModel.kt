package com.gumlapolytechnic.gpconnect.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gumlapolytechnic.gpconnect.data.model.AdminModule
import com.gumlapolytechnic.gpconnect.data.model.Department
import com.gumlapolytechnic.gpconnect.data.model.MEMBER_ROLES
import com.gumlapolytechnic.gpconnect.data.model.Notice
import com.gumlapolytechnic.gpconnect.data.model.SignupRequest
import com.gumlapolytechnic.gpconnect.data.model.User
import com.gumlapolytechnic.gpconnect.data.model.UserRole
import com.gumlapolytechnic.gpconnect.data.model.departmentModule
import com.gumlapolytechnic.gpconnect.data.repository.NoticeQuery
import com.gumlapolytechnic.gpconnect.data.repository.NoticeRepository
import com.gumlapolytechnic.gpconnect.data.repository.SignupRequestRepository
import com.gumlapolytechnic.gpconnect.data.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AdminDashboardUiState(
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val isSuperAdmin: Boolean = false,
    val isHod: Boolean = false,
    val module: AdminModule? = null,
    val department: Department? = null,
    val totalNotices: Int = 0,
    val pinnedNotices: Int = 0,
    val totalUsers: Int = 0,
    val totalAdmins: Int = 0,
    val enabledAdmins: Int = 0,
    val disabledAdmins: Int = 0,
    val pendingRequests: Int = 0,
    val noticesByModule: Map<AdminModule, Int> = emptyMap(),
    val notices: List<Notice> = emptyList(),
)

/**
 * Role-aware admin dashboard state. SUPER_ADMIN sees global counts (users,
 * admins, notices by module, all notices); department admins see only their
 * own module's notices and counts. An HOD additionally sees how many signup
 * requests are waiting in their own department. All mutations go through the
 * repository so Firestore security rules remain the authority.
 */
class AdminDashboardViewModel(
    private val adminUser: User,
    private val noticeRepository: NoticeRepository,
    userRepository: UserRepository,
    signupRequestRepository: SignupRequestRepository,
) : ViewModel() {

    private val refresh = MutableStateFlow(0)

    /** The module the role grants — the role is the single authority for this. */
    private val module: AdminModule? = adminUser.role.departmentModule

    /**
     * Pending signup requests visible to this admin. Only a super admin or an
     * HOD may list requests at all, so nobody else even opens a listener (the
     * rules would reject it). A super admin also sees pending HOD requests;
     * an HOD's badge counts member requests only, matching its inbox.
     */
    private val pendingRequests: Flow<Int> = when {
        adminUser.role == UserRole.SUPER_ADMIN -> signupRequestRepository.observeAllRequests()
        adminUser.isHod -> signupRequestRepository.observeDepartmentRequests(
            requireNotNull(adminUser.departmentOrNull),
        )
        else -> flowOf(Result.success(emptyList()))
    }.map { result: Result<List<SignupRequest>> ->
        val memberScopeOnly = adminUser.isHod
        result.getOrDefault(emptyList()).count {
            it.isPending && (!memberScopeOnly || it.requestedRole in MEMBER_ROLES)
        }
    }

    /**
     * A super admin aggregates college-wide counts, so it additionally listens
     * to the user collection; every other admin is confined to its own module's
     * notices. Kept as its own property (rather than an inline if-expression
     * with `.stateIn` hung off the end) so the flow's type is stated once and
     * both branches are checked against it.
     */
    private val dashboardState: Flow<AdminDashboardUiState> =
        if (adminUser.role == UserRole.SUPER_ADMIN) {
            combine(
                noticeRepository.observeNotices(),
                userRepository.observeUsers(),
                pendingRequests,
                refresh,
            ) { notices, users, pending, _ ->
                buildSuperState(notices, users, pending)
            }
        } else {
            combine(
                noticeRepository.observeNotices(NoticeQuery(module = module)),
                pendingRequests,
                refresh,
            ) { notices, pending, _ ->
                buildDepartmentState(notices, pending)
            }
        }

    val state: StateFlow<AdminDashboardUiState> = dashboardState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AdminDashboardUiState(
            isSuperAdmin = adminUser.role == UserRole.SUPER_ADMIN,
            isHod = adminUser.isHod,
            module = module,
            department = adminUser.departmentOrNull,
        ),
    )

    fun deleteNotice(noticeId: String) {
        viewModelScope.launch { noticeRepository.deleteNotice(noticeId) }
    }

    fun togglePinned(notice: Notice) {
        viewModelScope.launch { noticeRepository.setPinned(notice.id, !notice.isPinned) }
    }

    fun retry() {
        refresh.value += 1
    }

    private fun buildSuperState(notices: List<Notice>, users: List<User>, pending: Int) =
        AdminDashboardUiState(
            isLoading = false,
            isSuperAdmin = true,
            totalNotices = notices.size,
            pinnedNotices = notices.count { it.isPinned },
            totalUsers = users.size,
            totalAdmins = users.count { it.isAdmin },
            enabledAdmins = users.count { it.isAdmin && it.enabled },
            disabledAdmins = users.count { it.isAdmin && !it.enabled },
            pendingRequests = pending,
            noticesByModule = AdminModule.entries.associateWith { module ->
                notices.count { it.module == module }
            },
            notices = notices,
        )

    private fun buildDepartmentState(notices: List<Notice>, pending: Int) =
        AdminDashboardUiState(
            isLoading = false,
            isSuperAdmin = false,
            isHod = adminUser.isHod,
            module = module,
            department = adminUser.departmentOrNull,
            totalNotices = notices.size,
            pinnedNotices = notices.count { it.isPinned },
            pendingRequests = pending,
            notices = notices,
        )
}
