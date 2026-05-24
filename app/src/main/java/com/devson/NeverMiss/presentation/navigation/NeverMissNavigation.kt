package com.devson.NeverMiss.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.devson.NeverMiss.presentation.analytics.AnalyticsScreen
import com.devson.NeverMiss.presentation.create.CreateStreakScreen
import com.devson.NeverMiss.presentation.home.HomeScreen
import com.devson.NeverMiss.presentation.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object CreateStreak : Screen("create?streakId={streakId}") {
        fun createRoute(streakId: Long = -1L) = "create?streakId=$streakId"
    }
    object Analytics : Screen("analytics")
    object Settings : Screen("settings")
}

@Composable
fun NeverMissNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(route = Screen.Home.route) {
            HomeScreen(
                onNavigateToCreate = { navController.navigate(Screen.CreateStreak.createRoute()) },
                onNavigateToEdit = { streakId -> navController.navigate(Screen.CreateStreak.createRoute(streakId)) },
                onNavigateToAnalytics = { navController.navigate(Screen.Analytics.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
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
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(route = Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
