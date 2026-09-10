package com.gumlapolytechnic.gpconnect.ui.departments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Domain
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gumlapolytechnic.gpconnect.GPConnectApplication
import com.gumlapolytechnic.gpconnect.R
import com.gumlapolytechnic.gpconnect.data.model.Department
import com.gumlapolytechnic.gpconnect.data.model.DepartmentInfo
import com.gumlapolytechnic.gpconnect.ui.components.EmptyState
import com.gumlapolytechnic.gpconnect.ui.components.ErrorState
import com.gumlapolytechnic.gpconnect.ui.components.NoticeCardShimmer

/**
 * Student Departments list: every department of the college (the canonical
 * enum registry) with its HOD and office room from the live info documents.
 * Tapping a row opens the department detail screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepartmentsScreen(onDepartmentClick: (Department) -> Unit, onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as GPConnectApplication
    val viewModel: DepartmentsViewModel = viewModel { DepartmentsViewModel(app.container.departmentRepository) }
    val infos by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                // Status-bar inset is already consumed by the outer student
                // Scaffold's content padding (same rationale as the calendar
                // detail screen).
                windowInsets = WindowInsets(0.dp),
                title = { Text(stringResource(R.string.departments_screen_title)) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(Department.entries.toList()) { department ->
                DepartmentRow(
                    department = department,
                    info = infos[department]?.getOrNull(),
                    onClick = { onDepartmentClick(department) },
                )
            }
        }
    }
}

@Composable
private fun DepartmentRow(
    department: Department,
    info: DepartmentInfo?,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Domain,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = department.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                val officeRoom = info?.officeRoom
                if (!officeRoom.isNullOrBlank()) {
                    Text(
                        text = stringResource(
                            R.string.departments_row_office_format,
                            officeRoom,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Student department detail: HOD (from the existing assignment system), office
 * room, contact and about — all from Firestore; each section hides when the
 * HOD has not filled it in yet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepartmentDetailScreen(departmentId: String, onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as GPConnectApplication
    val viewModel: DepartmentDetailViewModel = viewModel(key = departmentId) {
        DepartmentDetailViewModel(app.container.departmentRepository, departmentId)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp),
                title = { Text(stringResource(R.string.departments_detail_title)) },
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
                    repeat(3) { NoticeCardShimmer() }
                }
            }
            state.isError -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    ErrorState(
                        message = stringResource(R.string.departments_error_body),
                    )
                }
            }
            state.department == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    EmptyState(
                        title = stringResource(R.string.departments_not_found_title),
                        message = stringResource(R.string.departments_not_found_body),
                    )
                }
            }
            else -> {
                val department = state.department!!
                val info = state.info
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 20.dp),
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = department.displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    DetailSection(
                        label = stringResource(R.string.departments_section_hod),
                        value = info?.hodName.orEmpty(),
                        valuePresent = !info?.hodName.isNullOrBlank(),
                    )
                    DetailSection(
                        label = stringResource(R.string.departments_section_office),
                        value = info?.officeRoom.orEmpty(),
                        valuePresent = !info?.officeRoom.isNullOrBlank(),
                    )
                    DetailSection(
                        label = stringResource(R.string.departments_section_contact),
                        value = info?.contact.orEmpty(),
                        valuePresent = !info?.contact.isNullOrBlank(),
                    )
                    DetailSection(
                        label = stringResource(R.string.departments_section_about),
                        value = info?.about.orEmpty(),
                        valuePresent = !info?.about.isNullOrBlank(),
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                }
            }
        }
    }
}

/** One label/value block; shows "not available" when the value is missing. */
@Composable
private fun DetailSection(
    label: String,
    value: String,
    valuePresent: Boolean,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
        text = if (valuePresent) value else stringResource(R.string.departments_not_available),
        style = MaterialTheme.typography.bodyLarge,
        color = if (valuePresent) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
    Spacer(modifier = Modifier.height(16.dp))
}
