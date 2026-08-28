package com.gumlapolytechnic.gpconnect.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gumlapolytechnic.gpconnect.data.model.AdminModule
import com.gumlapolytechnic.gpconnect.data.model.Attachment
import com.gumlapolytechnic.gpconnect.data.model.Audience
import com.gumlapolytechnic.gpconnect.data.model.Course
import com.gumlapolytechnic.gpconnect.data.model.Department
import com.gumlapolytechnic.gpconnect.data.model.NoticeCategory
import com.gumlapolytechnic.gpconnect.data.model.SEMESTER_RANGE
import com.gumlapolytechnic.gpconnect.data.model.User
import com.gumlapolytechnic.gpconnect.data.model.UserRole
import com.gumlapolytechnic.gpconnect.data.model.departmentModule
import com.gumlapolytechnic.gpconnect.data.repository.NoticeDraft
import com.gumlapolytechnic.gpconnect.data.repository.NoticeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Which audience selector is active in the form. */
enum class AudienceType { ALL, DEPARTMENT, COURSE }

/**
 * Audience selectors use the canonical [Department] / [Course] ids so a notice's
 * stored audience matches the value in `users.department` / `users.course`
 * exactly. Display labels are resolved in the UI.
 */
private val DEPARTMENT_IDS: List<String> = Department.entries.map { it.id }

private val COURSE_IDS: List<String> = Course.entries.map { it.id }

data class NoticeFormUiState(
    val isLoading: Boolean = false,
    val notFound: Boolean = false,
    val isEditMode: Boolean = false,
    val title: String = "",
    val body: String = "",
    val category: NoticeCategory = NoticeCategory.GENERAL,
    val audienceType: AudienceType = AudienceType.ALL,
    val department: String = DEPARTMENT_IDS.first(),
    val course: String = COURSE_IDS.first(),
    val semesterText: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val attachments: List<Attachment> = emptyList(),
    val departments: List<String> = DEPARTMENT_IDS,
    val courses: List<String> = COURSE_IDS,
    val titleError: Boolean = false,
    val bodyError: Boolean = false,
    val semesterError: Boolean = false,
    val isSaving: Boolean = false,
    val saveError: Boolean = false,
    val saved: Boolean = false,
)

/**
 * Notice create/edit form state. Create mode: a [NoticeDraft] stamped with the
 * acting admin's authority — SUPER_ADMIN publishes GLOBAL content, department
 * admins publish their own module (Firestore rules reject mismatches).
 * Edit mode: preloads by ID, preserves ID/author/ownership, and updates in
 * place — read markers are untouched, so read/unread state survives edits.
 */
class AdminNoticeFormViewModel(
    private val noticeRepository: NoticeRepository,
    private val adminUser: User,
    private val editNoticeId: String?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        NoticeFormUiState(
            isLoading = editNoticeId != null,
            isEditMode = editNoticeId != null,
        ),
    )
    val uiState: StateFlow<NoticeFormUiState> = _uiState.asStateFlow()

    init {
        val id = editNoticeId
        if (id != null) {
            viewModelScope.launch {
                val notice = noticeRepository.getNotice(id)
                _uiState.update { state ->
                    if (notice == null) {
                        state.copy(isLoading = false, notFound = true)
                    } else {
                        state.copy(
                            isLoading = false,
                            title = notice.title,
                            body = notice.body,
                            category = notice.category,
                            audienceType = when (notice.audience) {
                                Audience.All -> AudienceType.ALL
                                is Audience.Department -> AudienceType.DEPARTMENT
                                is Audience.Course -> AudienceType.COURSE
                            },
                            // Legacy notices may store a display label; fold it
                            // onto the canonical id so the chip matches.
                            department = (notice.audience as? Audience.Department)
                                ?.department
                                ?.let { Department.resolveOrNull(it)?.id ?: it }
                                ?: state.department,
                            course = (notice.audience as? Audience.Course)
                                ?.course
                                ?.let { Course.resolveOrNull(it)?.id ?: it }
                                ?: state.course,
                            semesterText = (notice.audience as? Audience.Course)
                                ?.semester?.toString().orEmpty(),
                            createdAt = notice.createdAt,
                            isPinned = notice.isPinned,
                            attachments = notice.attachments,
                        )
                    }
                }
            }
        }
    }

    fun onTitleChange(value: String) {
        _uiState.update { it.copy(title = value, titleError = false) }
    }

    fun onBodyChange(value: String) {
        _uiState.update { it.copy(body = value, bodyError = false) }
    }

    fun onCategoryChange(value: NoticeCategory) {
        _uiState.update { it.copy(category = value) }
    }

    fun onAudienceTypeChange(value: AudienceType) {
        _uiState.update { it.copy(audienceType = value, semesterError = false) }
    }

    fun onDepartmentChange(value: String) {
        _uiState.update { it.copy(department = value) }
    }

    fun onCourseChange(value: String) {
        _uiState.update { it.copy(course = value) }
    }

    fun onSemesterChange(value: String) {
        _uiState.update { it.copy(semesterText = value.filter { it.isDigit() }, semesterError = false) }
    }

    fun onDateChange(timestampMs: Long) {
        _uiState.update { it.copy(createdAt = timestampMs) }
    }

    fun onPinnedChange(pinned: Boolean) {
        _uiState.update { it.copy(isPinned = pinned) }
    }

    fun addAttachment(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        _uiState.update { it.copy(attachments = it.attachments + Attachment(trimmed)) }
    }

    fun removeAttachment(attachment: Attachment) {
        _uiState.update { it.copy(attachments = it.attachments - attachment) }
    }

    fun save() {
        val state = _uiState.value
        val titleError = state.title.isBlank()
        val bodyError = state.body.isBlank()
        val semester = state.semesterText.trim().toIntOrNull()
        val semesterError = state.semesterText.isNotBlank() &&
            (semester == null || semester !in SEMESTER_RANGE)

        if (titleError || bodyError || semesterError) {
            _uiState.update {
                it.copy(
                    titleError = titleError,
                    bodyError = bodyError,
                    semesterError = semesterError,
                )
            }
            return
        }

        val audience = when (state.audienceType) {
            AudienceType.ALL -> Audience.All
            AudienceType.DEPARTMENT -> Audience.Department(state.department)
            AudienceType.COURSE -> Audience.Course(state.course, semester)
        }

        _uiState.update {
            it.copy(isSaving = true, titleError = false, bodyError = false, semesterError = false, saveError = false)
        }
        viewModelScope.launch {
            val succeeded = try {
                if (editNoticeId == null) {
                    noticeRepository.createNotice(
                        NoticeDraft(
                            title = state.title.trim(),
                            body = state.body.trim(),
                            category = state.category,
                            audience = audience,
                            isPinned = state.isPinned,
                            attachments = state.attachments,
                            author = adminUser.name,
                            createdAt = state.createdAt,
                            ownerRole = adminUser.role,
                            module = if (adminUser.role == UserRole.SUPER_ADMIN) {
                                AdminModule.GLOBAL
                            } else {
                                adminUser.role.departmentModule ?: AdminModule.GLOBAL
                            },
                        ),
                    )
                } else {
                    val existing = noticeRepository.getNotice(editNoticeId)
                    if (existing != null) {
                        noticeRepository.updateNotice(
                            existing.copy(
                                title = state.title.trim(),
                                body = state.body.trim(),
                                category = state.category,
                                audience = audience,
                                isPinned = state.isPinned,
                                attachments = state.attachments,
                                createdAt = state.createdAt,
                            ),
                        )
                    }
                }
                true
            } catch (e: Exception) {
                false
            }
            _uiState.update {
                if (succeeded) {
                    it.copy(isSaving = false, saved = true)
                } else {
                    it.copy(isSaving = false, saveError = true)
                }
            }
        }
    }
}
