package com.gumlapolytechnic.gpconnect.ui.faculty

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Groups
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
import com.gumlapolytechnic.gpconnect.data.model.Faculty
import com.gumlapolytechnic.gpconnect.data.model.User
import com.gumlapolytechnic.gpconnect.ui.components.EmptyState
import com.gumlapolytechnic.gpconnect.ui.components.ErrorState
import com.gumlapolytechnic.gpconnect.ui.components.NoticeCardShimmer

/**
 * Student Faculty directory: the teaching staff of the signed-in student's
 * own department only. The department comes from the user profile — there is
 * no department selector, and the Firestore rules reject any cross-
 * department read regardless.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacultyScreen(user: User, onFacultyClick: (String) -> Unit, onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as GPConnectApplication
    val viewModel: FacultyListViewModel = viewModel {
        FacultyListViewModel(app.container.facultyRepository, user.departmentOrNull)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                // Status-bar inset is already consumed by the outer student
                // Scaffold (same rationale as the other student screens).
                windowInsets = WindowInsets(0.dp),
                title = { Text(stringResource(R.string.feature_faculty_title)) },
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
            state.department == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState()),
                ) {
                    EmptyState(
                        title = stringResource(R.string.faculty_no_department_title),
                        message = stringResource(R.string.faculty_no_department_body),
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
                        .padding(innerPadding),
                ) {
                    ErrorState(
                        message = stringResource(R.string.faculty_error_body),
                    )
                }
            }
            state.faculty.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState()),
                ) {
                    EmptyState(
                        title = stringResource(R.string.faculty_empty_title),
                        message = stringResource(R.string.faculty_empty_body),
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
                    items(state.faculty, key = { it.id }) { member ->
                        FacultyRow(
                            member = member,
                            onClick = { onFacultyClick(member.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FacultyRow(member: Faculty, onClick: () -> Unit) {
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
                imageVector = Icons.Outlined.Badge,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = member.designation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
 * Student faculty detail: name, designation, department, room number, email
 * and phone — with "not available" placeholders for the optional fields the
 * HOD left empty. No qualification field by design.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacultyDetailScreen(user: User, facultyId: String, onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as GPConnectApplication
    val viewModel: FacultyDetailViewModel = viewModel(key = facultyId) {
        FacultyDetailViewModel(app.container.facultyRepository, user.departmentOrNull, facultyId)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp),
                title = { Text(stringResource(R.string.faculty_detail_title)) },
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
            state.department == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState()),
                ) {
                    EmptyState(
                        title = stringResource(R.string.faculty_no_department_title),
                        message = stringResource(R.string.faculty_no_department_body),
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
                        message = stringResource(R.string.faculty_error_body),
                    )
                }
            }
            state.notFound || state.faculty == null -> {
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
                val member = state.faculty!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 20.dp),
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Groups,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp),
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = member.name,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = member.designation,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    FacultyDetailSection(
                        label = stringResource(R.string.faculty_section_department),
                        value = state.department!!.displayName,
                    )
                    FacultyDetailSection(
                        label = stringResource(R.string.faculty_section_room),
                        value = member.roomNumber,
                    )
                    FacultyDetailSection(
                        label = stringResource(R.string.faculty_section_email),
                        value = member.email,
                    )
                    FacultyDetailSection(
                        label = stringResource(R.string.faculty_section_phone),
                        value = member.phone,
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                }
            }
        }
    }
}

/** One label/value block; optional values show "not available" when blank. */
@Composable
private fun FacultyDetailSection(label: String, value: String) {
    val present = value.isNotBlank()
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
        text = if (present) value else stringResource(R.string.departments_not_available),
        style = MaterialTheme.typography.bodyLarge,
        color = if (present) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
    Spacer(modifier = Modifier.height(14.dp))
}
