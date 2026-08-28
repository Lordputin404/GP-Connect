package com.gumlapolytechnic.gpconnect.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import com.gumlapolytechnic.gpconnect.data.model.Department
import com.gumlapolytechnic.gpconnect.data.model.User
import com.gumlapolytechnic.gpconnect.data.model.UserRole
import com.gumlapolytechnic.gpconnect.data.model.departmentModule
import com.gumlapolytechnic.gpconnect.ui.components.CategoryChip
import com.gumlapolytechnic.gpconnect.ui.components.EmptyState
import com.gumlapolytechnic.gpconnect.ui.components.FieldLabel
import com.gumlapolytechnic.gpconnect.ui.components.NoticeCardShimmer
import com.gumlapolytechnic.gpconnect.ui.components.moduleLabel
import com.gumlapolytechnic.gpconnect.ui.components.roleLabel

/**
 * Admin Management (SUPER_ADMIN only): administrator accounts with role,
 * module and enabled state; enable/disable and role assignment. Self
 * modification is blocked in the UI and by the Firestore rules. Creating new
 * administrator Auth accounts requires the Firebase Console (or a backend in
 * a later phase) — this screen manages the profiles of existing accounts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminManagementScreen(currentUserId: String, onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as GPConnectApplication
    val viewModel: AdminManagementViewModel = viewModel {
        AdminManagementViewModel(app.container.userRepository, currentUserId)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var roleTarget by remember { mutableStateOf<User?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.admin_management_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back),
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
            state.admins.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState()),
                ) {
                    EmptyState(
                        title = stringResource(R.string.admin_management_empty_title),
                        message = stringResource(R.string.admin_management_empty_body),
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.admin_management_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(state.admins, key = { it.id }) { admin ->
                        AdminAccountCard(
                            admin = admin,
                            isSelf = admin.id == state.currentUserId,
                            onToggleEnabled = { viewModel.setEnabled(admin.id, !admin.enabled) },
                            onAssignRole = { roleTarget = admin },
                        )
                    }
                }
            }
        }
    }

    if (roleTarget != null) {
        val admin = roleTarget!!
        AssignRoleDialog(
            admin = admin,
            onDismiss = { roleTarget = null },
            onConfirm = { role, department ->
                viewModel.setAdminRole(admin.id, role, department)
                roleTarget = null
            },
        )
    }
}

/**
 * Role assignment for one administrator. FACULTY_ADMIN is the HOD role and each
 * HOD owns exactly one department, so choosing it reveals a department picker
 * and the save action stays disabled until a department is chosen. Every other
 * administrator role has no department at all.
 */
@Composable
private fun AssignRoleDialog(
    admin: User,
    onDismiss: () -> Unit,
    onConfirm: (UserRole, Department?) -> Unit,
) {
    var selectedRole by remember(admin.id) { mutableStateOf(admin.role) }
    var selectedDepartment by remember(admin.id) { mutableStateOf(admin.departmentOrNull) }
    val needsDepartment = selectedRole == UserRole.FACULTY_ADMIN

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.admin_assign_role_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = admin.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = admin.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                adminRoleChoices().forEach { role ->
                    CategoryChip(
                        label = roleLabel(role),
                        selected = selectedRole == role,
                        onClick = { selectedRole = role },
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                if (needsDepartment) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FieldLabel(stringResource(R.string.admin_assign_department_label))
                    Spacer(modifier = Modifier.height(4.dp))
                    Department.entries.forEach { department ->
                        CategoryChip(
                            label = department.displayName,
                            selected = selectedDepartment == department,
                            onClick = { selectedDepartment = department },
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(selectedRole, selectedDepartment.takeIf { needsDepartment })
                },
                enabled = !needsDepartment || selectedDepartment != null,
            ) {
                Text(stringResource(R.string.admin_form_action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.admin_action_cancel))
            }
        },
    )
}

private fun adminRoleChoices(): List<UserRole> = listOf(
    UserRole.SUPER_ADMIN,
    UserRole.LIBRARY_ADMIN,
    UserRole.CANTEEN_ADMIN,
    UserRole.FACULTY_ADMIN,
    UserRole.FACILITY_ADMIN,
)

@Composable
private fun AdminAccountCard(
    admin: User,
    isSelf: Boolean,
    onToggleEnabled: () -> Unit,
    onAssignRole: () -> Unit,
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = admin.name + if (isSelf) {
                            " " + stringResource(R.string.admin_management_you)
                        } else {
                            ""
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = admin.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!isSelf) {
                    Switch(
                        checked = admin.enabled,
                        onCheckedChange = { onToggleEnabled() },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = roleLabel(admin.role),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                // Derived from the role, not from the stored `module` field: that
                // field is display metadata that may be stale or missing, so the
                // chip must never be the thing a reader trusts about authority.
                admin.role.departmentModule?.let { module ->
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Text(
                            text = moduleLabel(module),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
                // Only an HOD owns a department, so this chip doubles as the
                // signal that the department scope has actually been assigned.
                admin.departmentOrNull?.let { department ->
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                    ) {
                        Text(
                            text = department.shortName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                if (!admin.enabled) {
                    Text(
                        text = stringResource(R.string.admin_management_disabled),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            if (!isSelf) {
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    TextButton(onClick = onAssignRole) {
                        Text(stringResource(R.string.admin_assign_role_action))
                    }
                }
            }
        }
    }
}
