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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.gumlapolytechnic.gpconnect.ui.components.CategoryChip
import com.gumlapolytechnic.gpconnect.ui.components.ChipRow
import com.gumlapolytechnic.gpconnect.ui.components.EmptyState
import com.gumlapolytechnic.gpconnect.ui.components.FieldLabel
import com.gumlapolytechnic.gpconnect.ui.components.labelRes
import com.gumlapolytechnic.gpconnect.util.Dates

/**
 * Calendar event create/edit form (the AdminNoticeFormScreen pattern): title,
 * description, type chips, start/end date pickers, all-day switch,
 * Confirmed/Tentative chips and the publish switch. The publish switch covers
 * "publish/unpublish" at creation; the management list toggles it later.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCalendarEventFormScreen(
    editEventId: String?,
    onBack: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as GPConnectApplication
    val viewModel: AdminCalendarEventFormViewModel = viewModel(key = editEventId ?: "create") {
        AdminCalendarEventFormViewModel(app.container.calendarRepository, editEventId)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

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
