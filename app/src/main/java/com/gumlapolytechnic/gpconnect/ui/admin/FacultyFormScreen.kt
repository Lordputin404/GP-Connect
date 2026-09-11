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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gumlapolytechnic.gpconnect.GPConnectApplication
import com.gumlapolytechnic.gpconnect.R
import com.gumlapolytechnic.gpconnect.data.model.User
import com.gumlapolytechnic.gpconnect.ui.components.EmptyState
import com.gumlapolytechnic.gpconnect.ui.components.FieldLabel

/**
 * HOD's faculty add/edit form: name, designation and room number (required),
 * email and phone (optional). Writes only the HOD's own department, and the
 * Firestore rules re-check it server-side. No qualification field by design.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacultyFormScreen(
    adminUser: User,
    editFacultyId: String?,
    onBack: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as GPConnectApplication
    val viewModel: FacultyFormViewModel = viewModel(key = editFacultyId ?: "create") {
        FacultyFormViewModel(
            facultyRepository = app.container.facultyRepository,
            department = adminUser.departmentOrNull,
            editFacultyId = editFacultyId,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
                                R.string.admin_faculty_edit_title
                            } else {
                                R.string.admin_faculty_add_title
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
            adminUser.departmentOrNull == null -> {
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
                        title = stringResource(R.string.faculty_not_found_title),
                        message = stringResource(R.string.faculty_not_found_body),
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
                        value = state.name,
                        onValueChange = viewModel::onNameChange,
                        label = { Text(stringResource(R.string.faculty_field_name)) },
                        isError = state.nameError,
                        supportingText = {
                            if (state.nameError) {
                                Text(stringResource(R.string.faculty_field_required))
                            }
                        },
                        singleLine = true,
                        enabled = !state.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = state.designation,
                        onValueChange = viewModel::onDesignationChange,
                        label = { Text(stringResource(R.string.faculty_field_designation)) },
                        isError = state.designationError,
                        supportingText = {
                            if (state.designationError) {
                                Text(stringResource(R.string.faculty_field_required))
                            }
                        },
                        singleLine = true,
                        enabled = !state.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = state.roomNumber,
                        onValueChange = viewModel::onRoomNumberChange,
                        label = { Text(stringResource(R.string.faculty_field_room)) },
                        isError = state.roomNumberError,
                        supportingText = {
                            if (state.roomNumberError) {
                                Text(stringResource(R.string.faculty_field_required))
                            }
                        },
                        singleLine = true,
                        enabled = !state.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = state.email,
                        onValueChange = viewModel::onEmailChange,
                        label = { Text(stringResource(R.string.faculty_section_email)) },
                        singleLine = true,
                        enabled = !state.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = state.phone,
                        onValueChange = viewModel::onPhoneChange,
                        label = { Text(stringResource(R.string.faculty_section_phone)) },
                        singleLine = true,
                        enabled = !state.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(28.dp))
                    if (state.saveError) {
                        Text(
                            text = stringResource(R.string.admin_faculty_save_error),
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
}
