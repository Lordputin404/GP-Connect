package com.gumlapolytechnic.gpconnect.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gumlapolytechnic.gpconnect.R
import com.gumlapolytechnic.gpconnect.data.model.User
import com.gumlapolytechnic.gpconnect.ui.calendar.CalendarScreen
import com.gumlapolytechnic.gpconnect.ui.home.HomeScreen
import com.gumlapolytechnic.gpconnect.ui.notices.NoticesScreen
import com.gumlapolytechnic.gpconnect.ui.profile.ProfileScreen

/** Route names for the student navigation graph. */
object Routes {
    const val HOME = "home"
    const val NOTICES = "notices"
    const val CALENDAR = "calendar"
    const val PROFILE = "profile"
}

private enum class TopLevelDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    HOME(Routes.HOME, R.string.tab_home, Icons.Filled.Home, Icons.Outlined.Home),
    NOTICES(Routes.NOTICES, R.string.tab_notices, Icons.Filled.Notifications, Icons.Outlined.Notifications),
    CALENDAR(Routes.CALENDAR, R.string.tab_calendar, Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    PROFILE(Routes.PROFILE, R.string.tab_profile, Icons.Filled.Person, Icons.Outlined.Person),
}

/**
 * Student app shell: bottom navigation with the four top-level destinations.
 * Tab taps use launchSingleTop + saveState/restoreState so repeated taps never
 * stack duplicate destinations and each tab keeps its own back-stack state.
 */
@Composable
fun StudentApp(user: User, onLogout: () -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    val selected = currentRoute == destination.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                contentDescription = stringResource(destination.labelRes),
                            )
                        },
                        label = { Text(stringResource(destination.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.HOME) { HomeScreen(user = user) }
            composable(Routes.NOTICES) { NoticesScreen() }
            composable(Routes.CALENDAR) { CalendarScreen() }
            composable(Routes.PROFILE) { ProfileScreen(user = user, onLogout = onLogout) }
        }
    }
}
