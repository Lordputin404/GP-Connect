package com.gumlapolytechnic.gpconnect.ui.signup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gumlapolytechnic.gpconnect.GPConnectApplication
import com.gumlapolytechnic.gpconnect.R
import com.gumlapolytechnic.gpconnect.data.repository.RegistrationResult
import com.gumlapolytechnic.gpconnect.ui.components.CategoryChip
import com.gumlapolytechnic.gpconnect.ui.components.ChipRow
import com.gumlapolytechnic.gpconnect.ui.components.FieldLabel
import com.gumlapolytechnic.gpconnect.ui.components.roleLabel

/**
 * Signup request form. The applicant chooses their department (and, as a
 * student, their course, semester and roll number) and the request is filed for
 * that department's HOD to approve.
 *
 * Nothing here grants access: the account is created disabled and the applicant
 * is signed out again immediately, so a pending request can never be used to
 * sign in. The password goes to Firebase Authentication only — never to
 * Firestore.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as GPConnectApplication
    val viewModel: SignupViewModel = viewModel { SignupViewModel(app.container.authRepository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var passwordVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.signup_title)) },
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
        if (state.submitted) {
            SignupSubmittedPanel(
                onBack = onBack,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.signup_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            FieldLabel(stringResource(R.string.signup_field_role))
            ChipRow {
                state.applicantRoles.forEach { role ->
                    CategoryChip(
                        label = roleLabel(role),
                        selected = state.requestedRole == role,
                        onClick = { viewModel.onRoleChange(role) },
                    )
                }
            }

            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.signup_field_name)) },
                isError = SignupField.NAME in state.fieldErrors,
                supportingText = {
                    if (SignupField.NAME in state.fieldErrors) {
                        Text(stringResource(R.string.signup_error_name))
                    }
                },
                singleLine = true,
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = { Text(stringResource(R.string.login_email_label)) },
                isError = SignupField.EMAIL in state.fieldErrors,
                supportingText = {
                    if (SignupField.EMAIL in state.fieldErrors) {
                        Text(stringResource(R.string.signup_error_email))
                    }
                },
                singleLine = true,
                enabled = !state.isSubmitting,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                label = { Text(stringResource(R.string.password_label)) },
                trailingIcon = {
                    IconButton(
                        onClick = { passwordVisible = !passwordVisible },
                        enabled = !state.isSubmitting,
                    ) {
                        Icon(
                            imageVector = if (passwordVisible) {
                                Icons.Filled.VisibilityOff
                            } else {
                                Icons.Filled.Visibility
                            },
                            contentDescription = stringResource(R.string.cd_toggle_password),
                        )
                    }
                },
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = SignupField.PASSWORD in state.fieldErrors,
                supportingText = {
                    Text(
                        text = if (SignupField.PASSWORD in state.fieldErrors) {
                            stringResource(R.string.signup_error_password)
                        } else {
                            stringResource(R.string.signup_password_hint)
                        },
                    )
                },
                singleLine = true,
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChange,
                label = { Text(stringResource(R.string.signup_field_confirm_password)) },
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = SignupField.CONFIRM_PASSWORD in state.fieldErrors,
                supportingText = {
                    if (SignupField.CONFIRM_PASSWORD in state.fieldErrors) {
                        Text(stringResource(R.string.signup_error_confirm_password))
                    }
                },
                singleLine = true,
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            )

            FieldLabel(stringResource(R.string.signup_field_department))
            ChipRow {
                state.departments.forEach { department ->
                    CategoryChip(
                        label = department.displayName,
                        selected = state.department == department,
                        onClick = { viewModel.onDepartmentChange(department) },
                    )
                }
            }
            if (SignupField.DEPARTMENT in state.fieldErrors) {
                Text(
                    text = stringResource(R.string.signup_error_department),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (state.isStudentApplicant) {
                FieldLabel(stringResource(R.string.signup_field_course))
                if (state.availableCourses.isEmpty()) {
                    Text(
                        text = stringResource(R.string.signup_course_pick_department),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    ChipRow {
                        state.availableCourses.forEach { course ->
                            CategoryChip(
                                label = course.displayName,
                                selected = state.course == course,
                                onClick = { viewModel.onCourseChange(course) },
                            )
                        }
                    }
                }
                if (SignupField.COURSE in state.fieldErrors) {
                    Text(
                        text = stringResource(R.string.signup_error_course),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                OutlinedTextField(
                    value = state.semester,
                    onValueChange = viewModel::onSemesterChange,
                    label = { Text(stringResource(R.string.signup_field_semester)) },
                    isError = SignupField.SEMESTER in state.fieldErrors,
                    supportingText = {
                        if (SignupField.SEMESTER in state.fieldErrors) {
                            Text(stringResource(R.string.admin_form_error_semester))
                        }
                    },
                    singleLine = true,
                    enabled = !state.isSubmitting,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = state.rollNo,
                    onValueChange = viewModel::onRollNoChange,
                    label = { Text(stringResource(R.string.profile_roll_no_label)) },
                    isError = SignupField.ROLL_NO in state.fieldErrors,
                    supportingText = {
                        if (SignupField.ROLL_NO in state.fieldErrors) {
                            Text(stringResource(R.string.signup_error_roll_no))
                        }
                    },
                    singleLine = true,
                    enabled = !state.isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            state.error?.let { error ->
                Text(
                    text = registrationErrorMessage(error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Button(
                onClick = viewModel::submit,
                enabled = !state.isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.signup_submitting))
                } else {
                    Text(stringResource(R.string.signup_submit))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SignupSubmittedPanel(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.signup_success_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.signup_success_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text(stringResource(R.string.signup_back_to_login))
        }
    }
}

/** Maps every [RegistrationResult] failure to a user-facing message. */
@Composable
private fun registrationErrorMessage(error: RegistrationResult): String = when (error) {
    RegistrationResult.Success -> ""
    RegistrationResult.EmailAlreadyInUse -> stringResource(R.string.signup_error_email_in_use)
    RegistrationResult.InvalidEmail -> stringResource(R.string.signup_error_email)
    RegistrationResult.WeakPassword -> stringResource(R.string.signup_error_password)
    RegistrationResult.NetworkError -> stringResource(R.string.error_network)
    RegistrationResult.RateLimited -> stringResource(R.string.error_rate_limited)
    RegistrationResult.ProviderMisconfigured -> stringResource(R.string.error_provider_misconfigured)
    RegistrationResult.RequestRejected -> stringResource(R.string.signup_error_rejected)
    RegistrationResult.UnknownFailure -> stringResource(R.string.signup_error_unknown)
}
