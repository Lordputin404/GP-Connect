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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.gumlapolytechnic.gpconnect.data.model.Course
import com.gumlapolytechnic.gpconnect.data.model.Department
import com.gumlapolytechnic.gpconnect.data.model.SignupRequest
import com.gumlapolytechnic.gpconnect.data.model.SignupRequestStatus
import com.gumlapolytechnic.gpconnect.data.model.User
import com.gumlapolytechnic.gpconnect.ui.components.EmptyState
import com.gumlapolytechnic.gpconnect.ui.components.ErrorState
import com.gumlapolytechnic.gpconnect.ui.components.NoticeCardShimmer
import com.gumlapolytechnic.gpconnect.ui.components.roleLabel
import com.gumlapolytechnic.gpconnect.util.Dates

/**
 * Signup request inbox for an HOD (their own department only) or a SUPER_ADMIN
 * (all departments). Approving flips the applicant's profile to the granted
 * role and enables it in the same atomic batch as the request decision, so an
 * approved request and a disabled account can never disagree.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupRequestsScreen(adminUser: User, onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as GPConnectApplication
    val viewModel: SignupRequestsViewModel = viewModel {
        SignupRequestsViewModel(app.container.signupRequestRepository, adminUser)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var rejectTarget by remember { mutableStateOf<SignupRequest?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.admin_requests_title)) },
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
                        message = stringResource(R.string.admin_requests_error),
                        onRetry = viewModel::retry,
                    )
                }
            }
            state.requests.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState()),
                ) {
                    EmptyState(
                        title = stringResource(R.string.admin_requests_empty_title),
                        message = stringResource(R.string.admin_requests_empty_body),
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
                            text = state.department?.let {
                                stringResource(R.string.admin_requests_scope_department, it.displayName)
                            } ?: stringResource(R.string.admin_requests_scope_all),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (state.actionFailed) {
                            Spacer(modifier = Modifier.height(8.dp))
                            ActionErrorBanner(onDismiss = viewModel::dismissActionError)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(state.requests, key = { it.uid }) { request ->
                        SignupRequestCard(
                            request = request,
                            isBusy = request.uid in state.busyUids,
                            onApprove = { viewModel.approve(request) },
                            onReject = { rejectTarget = request },
                        )
                    }
                }
            }
        }
    }

    if (rejectTarget != null) {
        val request = rejectTarget!!
        RejectRequestDialog(
            request = request,
            onDismiss = { rejectTarget = null },
            onConfirm = { note ->
                viewModel.reject(request, note)
                rejectTarget = null
            },
        )
    }
}

@Composable
private fun ActionErrorBanner(onDismiss: () -> Unit) {
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
                text = stringResource(R.string.admin_requests_action_failed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.admin_action_dismiss))
            }
        }
    }
}

@Composable
private fun RejectRequestDialog(
    request: SignupRequest,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit,
) {
    var note by remember(request.uid) { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.admin_requests_reject_title)) },
        text = {
            Column {
                Text(
                    text = request.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.admin_requests_reject_note)) },
                    singleLine = false,
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(note) }) {
                Text(
                    text = stringResource(R.string.admin_requests_reject),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.admin_action_cancel))
            }
        },
    )
}

@Composable
private fun SignupRequestCard(
    request: SignupRequest,
    isBusy: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit,
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
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = request.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = request.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                StatusChip(status = request.status)
            }

            Spacer(modifier = Modifier.height(8.dp))
            DetailRow(
                label = stringResource(R.string.profile_role_label),
                value = roleLabel(request.requestedRole),
            )
            DetailRow(
                label = stringResource(R.string.profile_department_label),
                value = Department.labelFor(request.department),
            )
            request.course?.let { course ->
                DetailRow(
                    label = stringResource(R.string.label_course),
                    value = Course.labelFor(course),
                )
            }
            request.semester?.let { semester ->
                DetailRow(
                    label = stringResource(R.string.label_semester),
                    value = semester.toString(),
                )
            }
            request.rollNo?.let { rollNo ->
                DetailRow(
                    label = stringResource(R.string.profile_roll_no_label),
                    value = rollNo,
                )
            }
            DetailRow(
                label = stringResource(R.string.admin_requests_submitted_label),
                value = Dates.format(request.createdAt),
            )
            request.decisionNote?.let { note ->
                DetailRow(
                    label = stringResource(R.string.admin_requests_note_label),
                    value = note,
                )
            }

            if (request.isPending) {
                Spacer(modifier = Modifier.height(12.dp))
                if (isBusy) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onApprove, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.admin_requests_approve))
                        }
                        OutlinedButton(onClick = onReject, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.admin_requests_reject))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(104.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatusChip(status: SignupRequestStatus) {
    val container = when (status) {
        SignupRequestStatus.PENDING -> MaterialTheme.colorScheme.tertiaryContainer
        SignupRequestStatus.APPROVED -> MaterialTheme.colorScheme.primaryContainer
        SignupRequestStatus.REJECTED -> MaterialTheme.colorScheme.errorContainer
    }
    val content = when (status) {
        SignupRequestStatus.PENDING -> MaterialTheme.colorScheme.onTertiaryContainer
        SignupRequestStatus.APPROVED -> MaterialTheme.colorScheme.onPrimaryContainer
        SignupRequestStatus.REJECTED -> MaterialTheme.colorScheme.onErrorContainer
    }
    val label = when (status) {
        SignupRequestStatus.PENDING -> R.string.admin_requests_status_pending
        SignupRequestStatus.APPROVED -> R.string.admin_requests_status_approved
        SignupRequestStatus.REJECTED -> R.string.admin_requests_status_rejected
    }
    Surface(shape = MaterialTheme.shapes.small, color = container) {
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.labelMedium,
            color = content,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}
