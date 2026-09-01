package com.gumlapolytechnic.gpconnect.ui.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gumlapolytechnic.gpconnect.data.model.Course
import com.gumlapolytechnic.gpconnect.data.model.Department
import com.gumlapolytechnic.gpconnect.data.model.SEMESTER_RANGE
import com.gumlapolytechnic.gpconnect.data.model.SignupSubmission
import com.gumlapolytechnic.gpconnect.data.model.UserRole
import com.gumlapolytechnic.gpconnect.data.repository.AuthRepository
import com.gumlapolytechnic.gpconnect.data.repository.RegistrationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Fields the signup form can flag individually. */
enum class SignupField {
    NAME,
    EMAIL,
    PASSWORD,
    CONFIRM_PASSWORD,
    DEPARTMENT,
    COURSE,
    SEMESTER,
    ROLL_NO,
}

data class SignupUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val requestedRole: UserRole = UserRole.STUDENT,
    val department: Department? = null,
    val course: Course? = null,
    val semester: String = "",
    val rollNo: String = "",
    val isSubmitting: Boolean = false,
    val submitted: Boolean = false,
    val error: RegistrationResult? = null,
    val fieldErrors: Set<SignupField> = emptySet(),
) {
    val departments: List<Department> = Department.entries
    val applicantRoles: List<UserRole> = listOf(
        UserRole.STUDENT,
        UserRole.TEACHER,
        UserRole.FACULTY_ADMIN,
    )

    /** Courses offered by the chosen department; empty until one is chosen. */
    val availableCourses: List<Course> get() = department?.courses.orEmpty()

    val isStudentApplicant: Boolean get() = requestedRole == UserRole.STUDENT

    /** A HOD applicant is reviewed by the super admin, not a department HOD. */
    val isHodApplicant: Boolean get() = requestedRole == UserRole.FACULTY_ADMIN
}

/**
 * Signup request form. Validation happens here; account creation, the
 * disabled-profile write and the PENDING request write are delegated to
 * [AuthRepository.register] so there is exactly one authentication path in the
 * app.
 *
 * The password is held in UI state only long enough to hand it to Firebase
 * Authentication — it is never part of [SignupSubmission] and never reaches
 * Firestore.
 */
class SignupViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SignupUiState())
    val uiState: StateFlow<SignupUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) = clearAndUpdate(SignupField.NAME) { it.copy(name = value) }

    fun onEmailChange(value: String) = clearAndUpdate(SignupField.EMAIL) { it.copy(email = value) }

    fun onPasswordChange(value: String) =
        clearAndUpdate(SignupField.PASSWORD) { it.copy(password = value) }

    fun onConfirmPasswordChange(value: String) =
        clearAndUpdate(SignupField.CONFIRM_PASSWORD) { it.copy(confirmPassword = value) }

    fun onRollNoChange(value: String) =
        clearAndUpdate(SignupField.ROLL_NO) { it.copy(rollNo = value) }

    fun onSemesterChange(value: String) =
        clearAndUpdate(SignupField.SEMESTER) { it.copy(semester = value.filter(Char::isDigit)) }

    fun onRoleChange(role: UserRole) {
        if (role !in _uiState.value.applicantRoles) return
        _uiState.update { current ->
            // Academic identity only applies to a student applicant; a teacher
            // or HOD applicant claims none of it.
            if (role != UserRole.STUDENT) {
                current.copy(
                    requestedRole = role,
                    course = null,
                    semester = "",
                    rollNo = "",
                    error = null,
                    fieldErrors = emptySet(),
                )
            } else {
                current.copy(requestedRole = role, error = null, fieldErrors = emptySet())
            }
        }
    }

    fun onDepartmentChange(department: Department) {
        _uiState.update { current ->
            current.copy(
                department = department,
                // A course belongs to one department, so a stale selection must go.
                course = current.course?.takeIf { it.department == department },
                error = null,
                fieldErrors = current.fieldErrors - SignupField.DEPARTMENT,
            )
        }
    }

    fun onCourseChange(course: Course) =
        clearAndUpdate(SignupField.COURSE) { it.copy(course = course) }

    fun submit() {
        val current = _uiState.value
        if (current.isSubmitting) return

        val errors = validate(current)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(fieldErrors = errors, error = null) }
            return
        }

        val submission = SignupSubmission(
            name = current.name.trim(),
            email = current.email.trim(),
            requestedRole = current.requestedRole,
            department = requireNotNull(current.department),
            course = current.course.takeIf { current.isStudentApplicant },
            semester = current.semester.toIntOrNull().takeIf { current.isStudentApplicant },
            rollNo = current.rollNo.trim().takeIf { current.isStudentApplicant && it.isNotBlank() },
        )

        _uiState.update { it.copy(isSubmitting = true, error = null, fieldErrors = emptySet()) }
        viewModelScope.launch {
            when (val result = authRepository.register(submission, current.password)) {
                RegistrationResult.Success -> _uiState.update {
                    // Clear the password from memory as soon as it is no longer needed.
                    it.copy(
                        isSubmitting = false,
                        submitted = true,
                        password = "",
                        confirmPassword = "",
                    )
                }
                else -> _uiState.update { it.copy(isSubmitting = false, error = result) }
            }
        }
    }

    private fun validate(state: SignupUiState): Set<SignupField> = buildSet {
        if (state.name.isBlank()) add(SignupField.NAME)
        if (!EMAIL_PATTERN.matches(state.email.trim())) add(SignupField.EMAIL)
        if (state.password.length < MIN_PASSWORD_LENGTH) add(SignupField.PASSWORD)
        if (state.confirmPassword != state.password) add(SignupField.CONFIRM_PASSWORD)
        if (state.department == null) add(SignupField.DEPARTMENT)
        if (state.isStudentApplicant) {
            if (state.course == null) add(SignupField.COURSE)
            val semester = state.semester.toIntOrNull()
            if (semester == null || semester !in SEMESTER_RANGE) add(SignupField.SEMESTER)
            if (state.rollNo.isBlank()) add(SignupField.ROLL_NO)
        }
    }

    private fun clearAndUpdate(field: SignupField, transform: (SignupUiState) -> SignupUiState) {
        _uiState.update { current ->
            transform(current).copy(error = null, fieldErrors = current.fieldErrors - field)
        }
    }

    private companion object {
        /** Firebase Authentication's own minimum for Email/Password accounts. */
        const val MIN_PASSWORD_LENGTH = 6
        val EMAIL_PATTERN = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}
