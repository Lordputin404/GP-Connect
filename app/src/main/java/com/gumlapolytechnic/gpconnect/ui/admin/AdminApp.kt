package com.gumlapolytechnic.gpconnect.ui.admin

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gumlapolytechnic.gpconnect.data.model.User
import com.gumlapolytechnic.gpconnect.data.model.UserRole

/** Routes for the admin navigation graph (no bottom bar — top app bars only). */
object AdminRoutes {
    const val DASHBOARD = "admin-dashboard"
    const val CREATE_NOTICE = "admin-create-notice"
    const val EDIT_NOTICE = "admin-edit/{noticeId}"
    const val EDIT_NOTICE_ARG = "noticeId"
    const val ADMIN_MANAGEMENT = "admin-management"
    const val SIGNUP_REQUESTS = "admin-signup-requests"
    const val TEACHERS = "admin-teachers"
    const val DEPARTMENT_INFO = "admin-department-info"
    const val FACULTY = "admin-faculty"
    const val FACULTY_CREATE = "admin-faculty-create"
    const val FACULTY_EDIT = "admin-faculty-edit/{facultyId}"
    const val FACULTY_EDIT_ARG = "facultyId"
    const val CALENDAR = "admin-calendar"
    const val CALENDAR_CREATE = "admin-calendar-create"
    const val CALENDAR_EDIT = "admin-calendar-edit/{eventId}"
    const val CALENDAR_EDIT_ARG = "eventId"

    fun editNotice(noticeId: String) = "admin-edit/$noticeId"
    fun editCalendarEvent(eventId: String) = "admin-calendar-edit/$eventId"
    fun editFaculty(facultyId: String) = "admin-faculty-edit/$facultyId"
}

/**
 * Role-aware admin workspace shell: SUPER_ADMIN gets the global dashboard and
 * the Admin Management section; department admins (Canteen/Library/Faculty/
 * Facility) get their own module dashboard and content publishing. An HOD
 * (FACULTY_ADMIN bound to a department) additionally gets the signup request
 * inbox and teacher management for that one department. Every destination is
 * top-app-bar based with natural back behavior. The shell is replaced at the
 * composition root the moment the admin session ends.
 */
@Composable
fun AdminApp(user: User, onLogout: () -> Unit) {
    val navController = rememberNavController()
    val isSuperAdmin = user.role == UserRole.SUPER_ADMIN
    // A FACULTY_ADMIN without a resolvable department has no department scope,
    // so it gets no route either — the rules would reject its queries anyway.
    val managesDepartment = isSuperAdmin || user.isHod

    NavHost(
        navController = navController,
        startDestination = AdminRoutes.DASHBOARD,
    ) {
        composable(AdminRoutes.DASHBOARD) {
            AdminDashboardScreen(
                adminUser = user,
                onLogout = onLogout,
                onCreateNotice = { navController.navigate(AdminRoutes.CREATE_NOTICE) },
                onEditNotice = { id -> navController.navigate(AdminRoutes.editNotice(id)) },
                onOpenAdminManagement = if (isSuperAdmin) {
                    { navController.navigate(AdminRoutes.ADMIN_MANAGEMENT) }
                } else {
                    null
                },
                onOpenCalendar = if (isSuperAdmin) {
                    { navController.navigate(AdminRoutes.CALENDAR) }
                } else {
                    null
                },
                onOpenSignupRequests = if (managesDepartment) {
                    { navController.navigate(AdminRoutes.SIGNUP_REQUESTS) }
                } else {
                    null
                },
                onOpenTeachers = if (user.isHod) {
                    { navController.navigate(AdminRoutes.TEACHERS) }
                } else {
                    null
                },
                // Department information editing is scoped to exactly the
                // department on the HOD's profile; the screen and the rules
                // both reject every other department.
                onOpenDepartmentInfo = if (user.isHod) {
                    { navController.navigate(AdminRoutes.DEPARTMENT_INFO) }
                } else {
                    null
                },
                // Faculty directory management, likewise HOD-only and scoped
                // to the caller's own department by screen and rules.
                onOpenFaculty = if (user.isHod) {
                    { navController.navigate(AdminRoutes.FACULTY) }
                } else {
                    null
                },
            )
        }
        composable(AdminRoutes.CREATE_NOTICE) {
            AdminNoticeFormScreen(
                adminUser = user,
                editNoticeId = null,
                onBack = { navController.popBackStack() },
            )
        }
        composable(AdminRoutes.EDIT_NOTICE) { entry ->
            val noticeId = entry.arguments?.getString(AdminRoutes.EDIT_NOTICE_ARG)
            if (noticeId != null) {
                AdminNoticeFormScreen(
                    adminUser = user,
                    editNoticeId = noticeId,
                    onBack = { navController.popBackStack() },
                )
            }
        }
        // Registered only for SUPER_ADMIN — department admins have no route to it.
        if (isSuperAdmin) {
            composable(AdminRoutes.ADMIN_MANAGEMENT) {
                AdminManagementScreen(
                    currentUserId = user.id,
                    onBack = { navController.popBackStack() },
                )
            }
        }
        // College Calendar management is SUPER_ADMIN-only: the rules reject
        // every calendarEvents write from any other role.
        if (isSuperAdmin) {
            composable(AdminRoutes.CALENDAR) {
                AdminCalendarScreen(
                    onAddEvent = { navController.navigate(AdminRoutes.CALENDAR_CREATE) },
                    onEditEvent = { id -> navController.navigate(AdminRoutes.editCalendarEvent(id)) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(AdminRoutes.CALENDAR_CREATE) {
                AdminCalendarEventFormScreen(
                    editEventId = null,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(AdminRoutes.CALENDAR_EDIT) { entry ->
                val eventId = entry.arguments?.getString(AdminRoutes.CALENDAR_EDIT_ARG)
                if (eventId != null) {
                    AdminCalendarEventFormScreen(
                        editEventId = eventId,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
        // Signup approval: SUPER_ADMIN college-wide, HOD for their department.
        if (managesDepartment) {
            composable(AdminRoutes.SIGNUP_REQUESTS) {
                SignupRequestsScreen(
                    adminUser = user,
                    onBack = { navController.popBackStack() },
                )
            }
        }
        // Teacher management is department-scoped, so it is HOD-only.
        if (user.isHod) {
            composable(AdminRoutes.TEACHERS) {
                TeacherManagementScreen(
                    adminUser = user,
                    onBack = { navController.popBackStack() },
                )
            }
            // HOD's own department information (about/office/contact).
            composable(AdminRoutes.DEPARTMENT_INFO) {
                AdminDepartmentEditScreen(
                    adminUser = user,
                    onBack = { navController.popBackStack() },
                )
            }
            // HOD's faculty directory (own department only).
            composable(AdminRoutes.FACULTY) {
                FacultyManagementScreen(
                    adminUser = user,
                    onAddFaculty = { navController.navigate(AdminRoutes.FACULTY_CREATE) },
                    onEditFaculty = { id -> navController.navigate(AdminRoutes.editFaculty(id)) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(AdminRoutes.FACULTY_CREATE) {
                FacultyFormScreen(
                    adminUser = user,
                    editFacultyId = null,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(AdminRoutes.FACULTY_EDIT) { entry ->
                val facultyId = entry.arguments?.getString(AdminRoutes.FACULTY_EDIT_ARG)
                if (facultyId != null) {
                    FacultyFormScreen(
                        adminUser = user,
                        editFacultyId = facultyId,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}
