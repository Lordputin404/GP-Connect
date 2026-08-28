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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.gumlapolytechnic.gpconnect.data.model.User
import com.gumlapolytechnic.gpconnect.ui.components.EmptyState
import com.gumlapolytechnic.gpconnect.ui.components.ErrorState
import com.gumlapolytechnic.gpconnect.ui.components.NoticeCardShimmer
import com.gumlapolytechnic.gpconnect.ui.components.SectionHeader

/**
 * Department roster for an HOD: promote a student to TEACHER, return a teacher
 * to STUDENT, and enable or disable an account — all limited to the HOD's own
 * department by the Firestore rules, not merely by this screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherManagementScreen(adminUser: User, onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as GPConnectApplication
    val viewModel: TeacherManagementViewModel = viewModel {
        TeacherManagementViewModel(app.container.userRepository, adminUser.departmentOrNull)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.admin_teachers_title)) },
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
            !state.hasDepartment -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState()),
                ) {
                    EmptyState(
                        title = stringResource(R.string.admin_teachers_no_department_title),
                        message = stringResource(R.string.admin_teachers_no_department_body),
                    )
                }
            }
            state.isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    repeat(4) { NoticeCardShimmer() }
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
                        message = stringResource(R.string.admin_teachers_error),
                        onRetry = viewModel::retry,
                    )
                }
            }
            state.teachers.isEmpty() && state.students.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState()),
                ) {
                    EmptyState(
                        title = stringResource(R.string.admin_teachers_empty_title),
                        message = stringResource(R.string.admin_teachers_empty_body),
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
                        state.department?.let { department ->
                            Text(
                                text = stringResource(
                                    R.string.admin_teachers_scope,
                                    department.displayName,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (state.actionFailed) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = stringResource(R.string.admin_teachers_action_failed),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.weight(1f),
                                    )
                                    TextButton(onClick = viewModel::dismissActionError) {
                                        Text(stringResource(R.string.admin_action_dismiss))
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        SectionHeader(
                            title = stringResource(
                                R.string.admin_teachers_section_teachers,
                                state.teachers.size,
                            ),
                        )
                    }

                    if (state.teachers.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.admin_teachers_none),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        items(state.teachers, key = { it.id }) { teacher ->
                            MemberCard(
                                member = teacher,
                                isBusy = teacher.id in state.busyUids,
                                actionLabel = stringResource(R.string.admin_teachers_demote),
                                onAction = { viewModel.demoteToStudent(teacher) },
                                onToggleEnabled = {
                                    viewModel.setEnabled(teacher, !teacher.enabled)
                                },
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        SectionHeader(
                            title = stringResource(
                                R.string.admin_teachers_section_students,
                                state.students.size,
                            ),
                        )
                    }

                    if (state.students.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.admin_teachers_no_students),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        items(state.students, key = { it.id }) { student ->
                            MemberCard(
                                member = student,
                                isBusy = student.id in state.busyUids,
                                actionLabel = stringResource(R.string.admin_teachers_promote),
                                onAction = { viewModel.promoteToTeacher(student) },
                                onToggleEnabled = {
                                    viewModel.setEnabled(student, !student.enabled)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberCard(
    member: User,
    isBusy: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
    onToggleEnabled: () -> Unit,
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
                        text = member.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = member.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val details = listOfNotNull(
                        member.courseOrNull?.displayName ?: member.course,
                        member.semester?.let { stringResource(R.string.admin_teachers_semester, it) },
                        member.rollNo,
                    )
                    if (details.isNotEmpty()) {
                        Text(
                            text = details.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Switch(checked = member.enabled, onCheckedChange = { onToggleEnabled() })
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.admin_requests_working),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    TextButton(onClick = onAction) { Text(actionLabel) }
                }
                Spacer(modifier = Modifier.weight(1f))
                if (!member.enabled) {
                    Text(
                        text = stringResource(R.string.admin_management_disabled),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
