package com.gumlapolytechnic.gpconnect.ui.admin

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gumlapolytechnic.gpconnect.data.model.User

/** Routes for the admin navigation graph (no bottom bar — top app bars only). */
object AdminRoutes {
    const val DASHBOARD = "admin-dashboard"
    const val CREATE_NOTICE = "admin-create-notice"
    const val EDIT_NOTICE = "admin-edit/{noticeId}"
    const val EDIT_NOTICE_ARG = "noticeId"

    fun editNotice(noticeId: String) = "admin-edit/$noticeId"
}

/**
 * Admin workspace shell. Completely separate from the student navigation:
 * no bottom navigation, ordinary destinations with top app bars and natural
 * back behavior. Replaced at the composition root the moment the admin
 * session ends, so logout can never leave an authenticated screen behind.
 */
@Composable
fun AdminApp(user: User, onLogout: () -> Unit) {
    val navController = rememberNavController()
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
    }
}
