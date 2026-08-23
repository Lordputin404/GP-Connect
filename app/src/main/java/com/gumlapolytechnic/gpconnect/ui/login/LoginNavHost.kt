package com.gumlapolytechnic.gpconnect.ui.login

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gumlapolytechnic.gpconnect.ui.admin.AdminLoginScreen

object LoginRoutes {
    const val STUDENT_LOGIN = "student-login"
    const val ADMIN_LOGIN = "admin-login"
}

/**
 * Pre-authentication navigation graph: student login is the start
 * destination; the Admin Login placeholder sits one hop above it with proper
 * back navigation. This entire graph is replaced by the student app the
 * moment authentication succeeds (root-level state switch, so login is never
 * left in the authenticated back stack).
 */
@Composable
fun LoginNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = LoginRoutes.STUDENT_LOGIN,
    ) {
        composable(LoginRoutes.STUDENT_LOGIN) {
            StudentLoginScreen(
                onAdminLoginClick = { navController.navigate(LoginRoutes.ADMIN_LOGIN) },
            )
        }
        composable(LoginRoutes.ADMIN_LOGIN) {
            AdminLoginScreen(onBack = { navController.popBackStack() })
        }
    }
}
