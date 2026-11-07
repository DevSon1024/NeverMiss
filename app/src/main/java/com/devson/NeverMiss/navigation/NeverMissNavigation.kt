package com.devson.NeverMiss.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.devson.NeverMiss.ui.screens.AnalyticsScreen
import com.devson.NeverMiss.ui.screens.CreateStreakScreen
import com.devson.NeverMiss.ui.screens.HomeScreen
import com.devson.NeverMiss.ui.screens.SettingsScreen
import com.devson.NeverMiss.ui.screens.appstreaks.AccountSubStreakScreen
import com.devson.NeverMiss.ui.screens.appstreaks.AppStreakDashboardScreen
import com.devson.NeverMiss.ui.screens.appstreaks.AppStreakDetailScreen
import com.devson.NeverMiss.ui.screens.appstreaks.InstalledAppPickerScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object CreateStreak : Screen("create?streakId={streakId}") {
        fun createRoute(streakId: Long = -1L) = "create?streakId=$streakId"
    }
    object Analytics : Screen("analytics")
    object Settings : Screen("settings")
    object AppPicker : Screen("app_picker")
    object AppStreaksDashboard : Screen("app_streaks")
    object AppStreakDetail : Screen("app_streak_detail/{appId}") {
        fun createRoute(appId: Long) = "app_streak_detail/$appId"
    }
    object AccountDetail : Screen("account_detail/{appId}/{accountId}") {
        fun createRoute(appId: Long, accountId: Long) = "account_detail/$appId/$accountId"
    }
}

@Composable
fun NeverMissNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            fadeIn(animationSpec = tween(220)) + slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth / 4 },
                animationSpec = tween(220)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(180)) + slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth / 4 },
                animationSpec = tween(180)
            )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(220)) + slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth / 4 },
                animationSpec = tween(220)
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(180)) + slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth / 4 },
                animationSpec = tween(180)
            )
        }
    ) {
        composable(route = Screen.Home.route) {
            HomeScreen(
                onNavigateToCreate = { navController.navigate(Screen.CreateStreak.createRoute()) },
                onNavigateToEdit = { streakId -> navController.navigate(Screen.CreateStreak.createRoute(streakId)) },
                onNavigateToAnalytics = {
                    navController.navigate(Screen.Analytics.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToAppStreaks = {
                    navController.navigate(Screen.AppStreaksDashboard.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
        
        composable(
            route = Screen.CreateStreak.route,
            arguments = listOf(
                navArgument("streakId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val streakId = backStackEntry.arguments?.getLong("streakId") ?: -1L
            CreateStreakScreen(
                streakId = streakId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(route = Screen.Analytics.route) {
            AnalyticsScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(route = Screen.Settings.route) {
            SettingsScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToAnalytics = {
                    navController.navigate(Screen.Analytics.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.AppPicker.route) {
            InstalledAppPickerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.AppStreaksDashboard.route) {
            AppStreakDashboardScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAppPicker = { navController.navigate(Screen.AppPicker.route) },
                onNavigateToAppDetail = { appId -> navController.navigate(Screen.AppStreakDetail.createRoute(appId)) }
            )
        }

        composable(
            route = Screen.AppStreakDetail.route,
            arguments = listOf(
                navArgument("appId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val appId = backStackEntry.arguments?.getLong("appId") ?: -1L
            AppStreakDetailScreen(
                appId = appId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAccount = { currentAppId, accountId ->
                    navController.navigate(Screen.AccountDetail.createRoute(currentAppId, accountId))
                }
            )
        }

        composable(
            route = Screen.AccountDetail.route,
            arguments = listOf(
                navArgument("appId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument("accountId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val appId = backStackEntry.arguments?.getLong("appId") ?: -1L
            val accountId = backStackEntry.arguments?.getLong("accountId") ?: -1L
            AccountSubStreakScreen(
                appId = appId,
                accountId = accountId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
