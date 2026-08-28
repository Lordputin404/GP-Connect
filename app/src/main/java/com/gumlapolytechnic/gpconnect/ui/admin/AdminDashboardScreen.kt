package com.gumlapolytechnic.gpconnect.ui.admin

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.HowToReg
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gumlapolytechnic.gpconnect.GPConnectApplication
import com.gumlapolytechnic.gpconnect.R
import com.gumlapolytechnic.gpconnect.data.model.AdminModule
import com.gumlapolytechnic.gpconnect.data.model.Notice
import com.gumlapolytechnic.gpconnect.data.model.User
import com.gumlapolytechnic.gpconnect.ui.components.CategoryBadge
import com.gumlapolytechnic.gpconnect.ui.components.EmptyState
import com.gumlapolytechnic.gpconnect.ui.components.ErrorState
import com.gumlapolytechnic.gpconnect.ui.components.NoticeCardShimmer
import com.gumlapolytechnic.gpconnect.ui.components.SectionHeader
import com.gumlapolytechnic.gpconnect.ui.components.moduleLabel
import com.gumlapolytechnic.gpconnect.ui.components.roleLabel
import com.gumlapolytechnic.gpconnect.util.Dates

/**
 * Role-aware admin dashboard. SUPER_ADMIN: welcome, global counters (users,
 * admins, notices), notices-by-module breakdown, Create Notice, Admin
 * Management and the full notice list. Department admins: welcome with their
 * module, module counters, Create Notice and their module's notice list only.
 * An HOD (FACULTY_ADMIN with a department) additionally gets their department's
 * signup request inbox and teacher management.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    adminUser: User,
    onLogout: () -> Unit,
    onCreateNotice: () -> Unit,
    onEditNotice: (String) -> Unit,
    onOpenAdminManagement: (() -> Unit)?,
    onOpenSignupRequests: (() -> Unit)?,
    onOpenTeachers: (() -> Unit)?,
) {
    val app = LocalContext.current.applicationContext as GPConnectApplication
    val viewModel: AdminDashboardViewModel = viewModel {
        AdminDashboardViewModel(
            adminUser = adminUser,
            noticeRepository = app.container.noticeRepository,
            userRepository = app.container.userRepository,
            signupRequestRepository = app.container.signupRequestRepository,
        )
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var noticePendingDelete by remember { mutableStateOf<Notice?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.admin_portal_title)) },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = stringResource(R.string.admin_cd_logout),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    repeat(5) { NoticeCardShimmer() }
                }
            }
            state.isError -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState()),
                ) {
                    ErrorState(
                        message = stringResource(R.string.admin_dashboard_error),
                        onRetry = viewModel::retry,
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 24.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = roleLabel(adminUser.role),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = adminUser.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        if (state.isSuperAdmin) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                StatCard(
                                    label = stringResource(R.string.admin_stat_notices),
                                    value = state.totalNotices,
                                    modifier = Modifier.weight(1f),
                                )
                                StatCard(
                                    label = stringResource(R.string.admin_stat_admins),
                                    value = state.totalAdmins,
                                    modifier = Modifier.weight(1f),
                                )
                                StatCard(
                                    label = stringResource(R.string.admin_stat_users),
                                    value = state.totalUsers,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(
                                    R.string.admin_stat_admin_status,
                                    state.enabledAdmins,
                                    state.disabledAdmins,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(
                                    R.string.admin_stat_pending_requests,
                                    state.pendingRequests,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.admin_stat_by_module),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            // Horizontal scroll keeps every chip at its
                            // intrinsic width — the last chip ("Global: 0")
                            // must never be squeezed into leftover space.
                            // FACILITY is excluded: the Facilities section is
                            // retired from the UI (role infra remains).
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                state.noticesByModule.forEach { (module, count) ->
                                    if (module != AdminModule.FACILITY) {
                                        ModuleCountChip(module = module, count = count)
                                    }
                                }
                            }
                        } else {
                            state.module?.let { module ->
                                Text(
                                    text = stringResource(
                                        R.string.admin_module_intro,
                                        moduleLabel(module),
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            // An HOD's whole workspace is scoped to one
                            // department, so name it before the counters.
                            state.department?.let { department ->
                                Text(
                                    text = stringResource(
                                        R.string.admin_department_intro,
                                        department.displayName,
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                StatCard(
                                    label = stringResource(R.string.admin_stat_notices),
                                    value = state.totalNotices,
                                    modifier = Modifier.weight(1f),
                                )
                                StatCard(
                                    label = stringResource(R.string.admin_stat_pinned),
                                    value = state.pinnedNotices,
                                    modifier = Modifier.weight(1f),
                                )
                                if (state.isHod) {
                                    StatCard(
                                        label = stringResource(R.string.admin_stat_pending),
                                        value = state.pendingRequests,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onCreateNotice, modifier = Modifier.fillMaxWidth()) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.admin_action_create_notice))
                        }
                        if (onOpenAdminManagement != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = onOpenAdminManagement,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Groups,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.admin_management_title))
                            }
                        }
                        if (onOpenSignupRequests != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = onOpenSignupRequests,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.HowToReg,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (state.pendingRequests > 0) {
                                        stringResource(
                                            R.string.admin_requests_title_count,
                                            state.pendingRequests,
                                        )
                                    } else {
                                        stringResource(R.string.admin_requests_title)
                                    },
                                )
                            }
                        }
                        if (onOpenTeachers != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = onOpenTeachers,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.School,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.admin_teachers_title))
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        SectionHeader(title = stringResource(R.string.admin_section_manage))
                    }

                    if (state.notices.isEmpty()) {
                        item {
                            EmptyState(
                                title = stringResource(R.string.admin_dashboard_empty_title),
                                message = stringResource(R.string.admin_dashboard_empty_body),
                            )
                        }
                    } else {
                        items(state.notices, key = { it.id }) { notice ->
                            AdminNoticeCard(
                                notice = notice,
                                onEdit = { onEditNotice(notice.id) },
                                onDelete = { noticePendingDelete = notice },
                                onTogglePin = { viewModel.togglePinned(notice) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (noticePendingDelete != null) {
        val notice = noticePendingDelete!!
        AlertDialog(
            onDismissRequest = { noticePendingDelete = null },
            title = { Text(stringResource(R.string.admin_delete_title)) },
            text = {
                Text(
                    text = notice.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteNotice(notice.id)
                        noticePendingDelete = null
                    },
                ) {
                    Text(
                        text = stringResource(R.string.admin_action_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { noticePendingDelete = null }) {
                    Text(stringResource(R.string.admin_action_cancel))
                }
            },
        )
    }
}

@Composable
private fun ModuleCountChip(module: AdminModule, count: Int) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = "${moduleLabel(module)}: $count",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun StatCard(label: String, value: Int, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AdminNoticeCard(
    notice: Notice,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryBadge(category = notice.category)
                    if (notice.isPinned) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = stringResource(R.string.cd_pinned),
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Text(
                    text = Dates.format(notice.createdAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = notice.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = moduleLabel(notice.module),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onTogglePin) {
                    Icon(
                        imageVector = Icons.Filled.PushPin,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(
                            if (notice.isPinned) R.string.admin_action_unpin else R.string.admin_action_pin,
                        ),
                    )
                }
                TextButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.admin_action_edit))
                }
                TextButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.admin_action_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
