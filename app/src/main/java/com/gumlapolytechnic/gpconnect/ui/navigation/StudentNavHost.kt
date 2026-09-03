package com.gumlapolytechnic.gpconnect.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restaurant
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gumlapolytechnic.gpconnect.R
import com.gumlapolytechnic.gpconnect.data.model.User
import com.gumlapolytechnic.gpconnect.ui.calendar.CalendarScreen
import com.gumlapolytechnic.gpconnect.ui.canteen.CanteenCartScreen
import com.gumlapolytechnic.gpconnect.ui.canteen.CanteenScreen
import com.gumlapolytechnic.gpconnect.ui.canteen.CanteenItemDetailScreen
import com.gumlapolytechnic.gpconnect.ui.home.HomeScreen
import com.gumlapolytechnic.gpconnect.ui.login.SessionViewModel
import com.gumlapolytechnic.gpconnect.ui.notices.NoticeDetailScreen
import com.gumlapolytechnic.gpconnect.ui.notices.NoticesScreen
import com.gumlapolytechnic.gpconnect.ui.placeholder.CampusFeature
import com.gumlapolytechnic.gpconnect.ui.placeholder.FeaturePlaceholderScreen
import com.gumlapolytechnic.gpconnect.ui.profile.ProfileScreen

/** Route names for the student navigation graph. */
object Routes {
    const val HOME = "home"
    const val NOTICES = "notices"
    const val CALENDAR = "calendar"
    const val CANTEEN = "canteen"
    const val CANTEEN_ITEM = "canteen/item/{itemId}"
    const val CANTEEN_CART = "canteen/cart"
    const val PROFILE = "profile"

    const val NOTICE_DETAIL = "notice/{noticeId}"
    const val NOTICE_DETAIL_ARG = "noticeId"
    const val FEATURE_PLACEHOLDER = "feature/{feature}"
    const val FEATURE_ARG = "feature"
    const val CANTEEN_ITEM_ARG = "itemId"

    fun noticeDetail(noticeId: String) = "notice/$noticeId"
    fun featurePlaceholder(feature: CampusFeature) = "feature/${feature.routeArg}"
    fun canteenItem(itemId: String) = "canteen/item/$itemId"
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
    CANTEEN(Routes.CANTEEN, R.string.tab_canteen, Icons.Filled.Restaurant, Icons.Outlined.Restaurant),
    PROFILE(Routes.PROFILE, R.string.tab_profile, Icons.Filled.Person, Icons.Outlined.Person),
}

private val topLevelRoutes = TopLevelDestination.entries.map { it.route }.toSet()

/**
 * Student app shell. Bottom navigation covers the five top-level
 * destinations; notice detail and feature placeholders open as ordinary
 * destinations above them (bottom bar hidden) with normal back behavior.
 * Tab taps use launchSingleTop + saveState/restoreState so repeated taps
 * never stack duplicate destinations and each tab keeps its own state.
 */
@Composable
fun StudentApp(
    user: User,
    onLogout: () -> Unit,
    sessionViewModel: SessionViewModel,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in topLevelRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { destination ->
                        val selected = currentRoute == destination.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.navigateTopLevel(destination.route) },
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
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
        ) {
composable(Routes.HOME) {
                HomeScreen(
                    user = user,
                    onNoticeClick = { id -> navController.navigate(Routes.noticeDetail(id)) },
                    onViewAllNotices = { navController.navigateTopLevel(Routes.NOTICES) },
                    onFeatureClick = { feature ->
                        navController.navigate(Routes.featurePlaceholder(feature))
                    },
                    onCanteenClick = { navController.navigateTopLevel(Routes.CANTEEN) },
                )
            }
            composable(Routes.CANTEEN) {
                CanteenScreen(
                    onItemClick = { itemId ->
                        navController.navigate(Routes.canteenItem(itemId))
                    },
                    onCartClick = { navController.navigate(Routes.CANTEEN_CART) },
                )
            }
            composable(Routes.CANTEEN_ITEM) { entry ->
                val itemId = entry.arguments?.getString(Routes.CANTEEN_ITEM_ARG)
                if (itemId != null) {
                    CanteenItemDetailScreen(
                        itemId = itemId,
                        onBack = { navController.popBackStack() },
                        onCartClick = { navController.navigate(Routes.CANTEEN_CART) },
                        sessionViewModel = sessionViewModel,
                    )
                }
            }
            composable(Routes.CANTEEN_CART) {
                CanteenCartScreen(
                    onBack = { navController.popBackStack() },
                    sessionViewModel = sessionViewModel,
                )
            }
            composable(Routes.NOTICES) {
                NoticesScreen(
                    onNoticeClick = { id -> navController.navigate(Routes.noticeDetail(id)) },
                )
            }
            composable(Routes.CALENDAR) { CalendarScreen() }
            composable(Routes.PROFILE) {
                ProfileScreen(user = user, onLogout = onLogout)
            }
            composable(Routes.NOTICE_DETAIL) { entry ->
                val noticeId = entry.arguments?.getString(Routes.NOTICE_DETAIL_ARG)
                if (noticeId != null) {
                    NoticeDetailScreen(
                        noticeId = noticeId,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
            composable(Routes.FEATURE_PLACEHOLDER) { entry ->
                val feature = CampusFeature.fromRouteArg(entry.arguments?.getString(Routes.FEATURE_ARG))
                if (feature != null) {
                    FeaturePlaceholderScreen(
                        feature = feature,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}

/** Single top-level navigation pattern: no duplicates, tab state preserved. */
private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
