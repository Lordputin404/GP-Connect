package com.gumlapolytechnic.gpconnect.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gumlapolytechnic.gpconnect.GPConnectApplication
import com.gumlapolytechnic.gpconnect.R
import com.gumlapolytechnic.gpconnect.data.model.Attachment
import com.gumlapolytechnic.gpconnect.data.model.Course
import com.gumlapolytechnic.gpconnect.data.model.Department
import com.gumlapolytechnic.gpconnect.data.model.NoticeCategory
import com.gumlapolytechnic.gpconnect.data.model.User
import com.gumlapolytechnic.gpconnect.ui.components.CategoryChip
import com.gumlapolytechnic.gpconnect.ui.components.ChipRow
import com.gumlapolytechnic.gpconnect.ui.components.EmptyState
import com.gumlapolytechnic.gpconnect.ui.components.FieldLabel
import com.gumlapolytechnic.gpconnect.ui.components.labelRes
import com.gumlapolytechnic.gpconnect.util.Dates

/**
 * Notice create/edit form (one form, two modes): title, category chips, body,
 * audience selectors, date picker, pinned toggle and attachment metadata.
 * Inline validation, save-disabled-while-saving, and a saved signal that
 * returns to the dashboard.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminNoticeFormScreen(
    adminUser: User,
    editNoticeId: String?,
    onBack: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as GPConnectApplication
    val viewModel: AdminNoticeFormViewModel = viewModel(key = editNoticeId ?: "create") {
        AdminNoticeFormViewModel(
            noticeRepository = app.container.noticeRepository,
            adminUser = adminUser,
            editNoticeId = editNoticeId,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showDatePicker by remember { mutableStateOf(false) }
    var showAttachmentDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved) {
        if (state.saved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (state.isEditMode) {
                                R.string.admin_form_edit_title
                            } else {
                                R.string.admin_form_create_title
                            },
                        ),
                    )
                },
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
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 48.dp),
                    )
                }
            }
            state.notFound -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState()),
                ) {
                    EmptyState(
                        title = stringResource(R.string.notice_not_found_title),
                        message = stringResource(R.string.notice_not_found_body),
                    )
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(horizontal = 16.dp),
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = state.title,
                        onValueChange = viewModel::onTitleChange,
                        label = { Text(stringResource(R.string.admin_form_field_title)) },
                        isError = state.titleError,
                        supportingText = {
                            if (state.titleError) {
                                Text(stringResource(R.string.admin_form_error_title_required))
                            }
                        },
                        singleLine = true,
                        enabled = !state.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    FieldLabel(stringResource(R.string.admin_form_field_category))
                    ChipRow {
                        NoticeCategory.entries.forEach { category ->
                            CategoryChip(
                                label = stringResource(category.labelRes),
                                selected = state.category == category,
                                onClick = { viewModel.onCategoryChange(category) },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = state.body,
                        onValueChange = viewModel::onBodyChange,
                        label = { Text(stringResource(R.string.admin_form_field_body)) },
                        isError = state.bodyError,
                        supportingText = {
                            if (state.bodyError) {
                                Text(stringResource(R.string.admin_form_error_body_required))
                            }
                        },
                        minLines = 6,
                        enabled = !state.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    FieldLabel(stringResource(R.string.admin_form_field_audience))
                    ChipRow {
                        AudienceType.entries.forEach { type ->
                            CategoryChip(
                                label = stringResource(
                                    when (type) {
                                        AudienceType.ALL -> R.string.admin_audience_all
                                        AudienceType.DEPARTMENT -> R.string.admin_audience_department
                                        AudienceType.COURSE -> R.string.admin_audience_course
                                    },
                                ),
                                selected = state.audienceType == type,
                                onClick = { viewModel.onAudienceTypeChange(type) },
                            )
                        }
                    }
                    when (state.audienceType) {
                        AudienceType.ALL -> Unit
                        AudienceType.DEPARTMENT -> {
                            Spacer(modifier = Modifier.height(4.dp))
                            ChipRow {
                                state.departments.forEach { department ->
                                    CategoryChip(
                                        label = Department.labelFor(department),
                                        selected = state.department == department,
                                        onClick = { viewModel.onDepartmentChange(department) },
                                    )
                                }
                            }
                        }
                        AudienceType.COURSE -> {
                            Spacer(modifier = Modifier.height(4.dp))
                            ChipRow {
                                state.courses.forEach { course ->
                                    CategoryChip(
                                        label = Course.labelFor(course),
                                        selected = state.course == course,
                                        onClick = { viewModel.onCourseChange(course) },
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = state.semesterText,
                                onValueChange = viewModel::onSemesterChange,
                                label = { Text(stringResource(R.string.admin_form_semester_label)) },
                                isError = state.semesterError,
                                supportingText = {
                                    if (state.semesterError) {
                                        Text(stringResource(R.string.admin_form_error_semester))
                                    }
                                },
                                singleLine = true,
                                enabled = !state.isSaving,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    FieldLabel(stringResource(R.string.admin_form_field_date))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = Dates.format(state.createdAt),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        TextButton(onClick = { showDatePicker = true }) {
                            Text(stringResource(R.string.admin_form_date_change))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.admin_form_field_pinned),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Switch(
                            checked = state.isPinned,
                            onCheckedChange = viewModel::onPinnedChange,
                            enabled = !state.isSaving,
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    FieldLabel(stringResource(R.string.admin_form_field_attachments))
                    if (state.attachments.isEmpty()) {
                        Text(
                            text = stringResource(R.string.admin_form_attachments_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    state.attachments.forEach { attachment ->
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 12.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AttachFile,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = attachment.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(onClick = { viewModel.removeAttachment(attachment) }) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = stringResource(
                                            R.string.admin_form_attachment_remove,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { showAttachmentDialog = true },
                        enabled = !state.isSaving,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AttachFile,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.admin_form_add_attachment))
                    }

                    Spacer(modifier = Modifier.height(28.dp))
                    if (state.saveError) {
                        Text(
                            text = stringResource(R.string.admin_form_save_error),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = onBack,
                            enabled = !state.isSaving,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.admin_action_cancel))
                        }
                        Button(
                            onClick = viewModel::save,
                            enabled = !state.isSaving,
                            modifier = Modifier.weight(1f),
                        ) {
                            if (state.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.admin_form_saving))
                            } else {
                                Text(stringResource(R.string.admin_form_action_save))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = state.createdAt)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onDateChange(pickerState.selectedDateMillis ?: state.createdAt)
                        showDatePicker = false
                    },
                ) {
                    Text(stringResource(R.string.admin_form_date_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.admin_action_cancel))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showAttachmentDialog) {
        AttachmentNameDialog(
            onDismiss = { showAttachmentDialog = false },
            onConfirm = { name ->
                viewModel.addAttachment(name)
                showAttachmentDialog = false
            },
        )
    }
}

@Composable
private fun AttachmentNameDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.admin_form_add_attachment)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.admin_form_attachment_hint)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(R.string.admin_form_attachment_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.admin_action_cancel))
            }
        },
    )
}
