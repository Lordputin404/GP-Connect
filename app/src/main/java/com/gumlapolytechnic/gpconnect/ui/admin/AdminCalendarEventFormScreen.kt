package com.gumlapolytechnic.gpconnect.ui.admin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AttachFile
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gumlapolytechnic.gpconnect.GPConnectApplication
import com.gumlapolytechnic.gpconnect.R
import com.gumlapolytechnic.gpconnect.data.model.CalendarEventStatus
import com.gumlapolytechnic.gpconnect.data.model.CalendarEventType
import com.gumlapolytechnic.gpconnect.data.repository.PendingAttachment
import com.gumlapolytechnic.gpconnect.ui.components.CategoryChip
import com.gumlapolytechnic.gpconnect.ui.components.ChipRow
import com.gumlapolytechnic.gpconnect.ui.components.EmptyState
import com.gumlapolytechnic.gpconnect.ui.components.FieldLabel
import com.gumlapolytechnic.gpconnect.ui.components.labelRes
import com.gumlapolytechnic.gpconnect.util.Dates

/**
 * Calendar event create/edit form (the AdminNoticeFormScreen pattern): title,
 * description, type chips, start/end date pickers, all-day switch,
 * Confirmed/Tentative chips, the publish switch and a Storage-backed
 * attachment picker. Picked files (PDF/DOC/DOCX/JPG/JPEG/PNG/WebP only) are
 * uploaded with the save — never before it — so a cancelled form leaves no
 * orphaned Storage objects. The publish switch covers "publish/unpublish" at
 * creation; the management list toggles it later.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCalendarEventFormScreen(
    editEventId: String?,
    onBack: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as GPConnectApplication
    val context = LocalContext.current
    val viewModel: AdminCalendarEventFormViewModel = viewModel(key = editEventId ?: "create") {
        AdminCalendarEventFormViewModel(app.container.calendarRepository, editEventId)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    // SAF picker: one document at a time; every pick appends to the pending
    // list, so "Add attachment" may be tapped repeatedly. Unsupported types
    // are rejected by the ViewModel with an inline message.
    val pickAttachment = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            // Persistable permission keeps the Uri readable at save time,
            // which may be much later than the pick.
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            val meta = uri.queryAttachmentNameAndSize(context)
            viewModel.onAttachmentPicked(uri, meta.first, meta.second)
        }
    }

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
                                R.string.admin_calendar_edit_title
                            } else {
                                R.string.admin_calendar_add_title
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
                        title = stringResource(R.string.calendar_event_not_found_title),
                        message = stringResource(R.string.calendar_event_not_found_body),
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
                    OutlinedTextField(
                        value = state.description,
                        onValueChange = viewModel::onDescriptionChange,
                        label = { Text(stringResource(R.string.admin_calendar_field_description)) },
                        minLines = 3,
                        enabled = !state.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    FieldLabel(stringResource(R.string.admin_calendar_field_type))
                    ChipRow {
                        CalendarEventType.entries.forEach { type ->
                            CategoryChip(
                                label = stringResource(type.labelRes),
                                selected = state.type == type,
                                onClick = { viewModel.onTypeChange(type) },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    FieldLabel(stringResource(R.string.admin_calendar_field_start_date))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = Dates.format(state.startDate),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        TextButton(onClick = { showStartPicker = true }) {
                            Text(stringResource(R.string.admin_form_date_change))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    FieldLabel(stringResource(R.string.admin_calendar_field_end_date))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = Dates.format(state.endDate),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        TextButton(onClick = { showEndPicker = true }) {
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
                            text = stringResource(R.string.admin_calendar_field_all_day),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Switch(
                            checked = state.isAllDay,
                            onCheckedChange = viewModel::onAllDayChange,
                            enabled = !state.isSaving,
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    FieldLabel(stringResource(R.string.admin_calendar_field_status))
                    ChipRow {
                        CalendarEventStatus.entries.forEach { status ->
                            CategoryChip(
                                label = stringResource(status.labelRes),
                                selected = state.status == status,
                                onClick = { viewModel.onStatusChange(status) },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.admin_calendar_field_published),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Switch(
                            checked = state.isPublished,
                            onCheckedChange = viewModel::onPublishedChange,
                            enabled = !state.isSaving,
                        )
                    }

                    // --- Attachments (uploaded with the save) -------------------
                    Spacer(modifier = Modifier.height(16.dp))
                    FieldLabel(stringResource(R.string.admin_calendar_field_attachments))
                    if (state.unsupportedAttachment) {
                        Text(
                            text = stringResource(R.string.admin_calendar_attachment_unsupported),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    if (state.existingAttachments.isEmpty() && state.pendingAttachments.isEmpty()) {
                        Text(
                            text = stringResource(R.string.admin_calendar_attachments_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    state.existingAttachments.forEach { attachment ->
                        AttachmentRow(
                            name = attachment.name,
                            meta = stringResource(
                                R.string.admin_calendar_attachment_meta,
                                attachment.type.name,
                                formatSize(attachment.size),
                            ),
                            onRemove = { viewModel.removeExistingAttachment(attachment) },
                            removeLabel = stringResource(R.string.admin_calendar_attachment_remove),
                        )
                    }
                    state.pendingAttachments.forEach { pending ->
                        AttachmentRow(
                            name = pending.name,
                            meta = stringResource(
                                R.string.admin_calendar_attachment_meta,
                                pending.name.substringAfterLast('.', "").uppercase().ifEmpty { "FILE" },
                                formatSize(pending.size),
                            ),
                            onRemove = { viewModel.removePendingAttachment(pending) },
                            removeLabel = stringResource(R.string.admin_calendar_attachment_remove),
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = {
                            pickAttachment.launch(
                                arrayOf(
                                    "application/pdf",
                                    "application/msword",
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                    "image/jpeg",
                                    "image/png",
                                    "image/webp",
                                ),
                            )
                        },
                        enabled = !state.isSaving,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AttachFile,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.admin_calendar_add_attachment))
                    }

                    Spacer(modifier = Modifier.height(28.dp))
                    if (state.saveError) {
                        Text(
                            text = stringResource(R.string.admin_calendar_save_error),
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

    if (showStartPicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = state.startDate)
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onStartDateChange(
                            pickerState.selectedDateMillis ?: state.startDate,
                        )
                        showStartPicker = false
                    },
                ) {
                    Text(stringResource(R.string.admin_form_date_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) {
                    Text(stringResource(R.string.admin_action_cancel))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showEndPicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = state.endDate)
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onEndDateChange(pickerState.selectedDateMillis ?: state.endDate)
                        showEndPicker = false
                    },
                ) {
                    Text(stringResource(R.string.admin_form_date_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) {
                    Text(stringResource(R.string.admin_action_cancel))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

/** One stored-or-picked file row: paperclip, name + "TYPE • size", remove. */
@Composable
private fun AttachmentRow(
    name: String,
    meta: String,
    onRemove: () -> Unit,
    removeLabel: String,
) {
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRemove, enabled = true) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = removeLabel,
                )
            }
        }
    }
}

/** "1.2 MB" / "540 KB" / "800 B" — the size format used by the attachment rows. */
internal fun formatSize(bytes: Long): String = when {
    bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576f)
    bytes >= 1_024 -> "%.0f KB".format(bytes / 1_024f)
    else -> "$bytes B"
}

/** Display name + byte size of a picked SAF document. */
internal fun android.net.Uri.queryAttachmentNameAndSize(
    context: android.content.Context,
): Pair<String, Long> {
    val resolver = context.contentResolver
    val name = runCatching {
        resolver.query(this, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
    }.getOrNull() ?: lastPathSegment ?: "attachment"
    val size = runCatching {
        resolver.query(this, arrayOf(android.provider.OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else null
            }
    }.getOrNull() ?: 0L
    return name to size
}
