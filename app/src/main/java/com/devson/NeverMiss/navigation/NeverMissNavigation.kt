package com.devson.NeverMiss.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.devson.NeverMiss.domain.model.StreakType
import com.devson.NeverMiss.ui.components.FloatingCapsuleNavigationBar
import com.devson.NeverMiss.ui.components.NavDestination
import com.devson.NeverMiss.ui.screens.CreateStreakScreen
import com.devson.NeverMiss.ui.screens.HomeScreen
import com.devson.NeverMiss.ui.screens.SettingsScreen
import com.devson.NeverMiss.ui.screens.archive.ArchiveScreen
import com.devson.NeverMiss.ui.screens.appstreaks.AccountSubStreakScreen
import com.devson.NeverMiss.ui.screens.appstreaks.AppStreakDashboardScreen
import com.devson.NeverMiss.ui.screens.appstreaks.InstalledAppPickerScreen
import com.devson.NeverMiss.ui.screens.appstreaks.TrackingDiagnosticsScreen
import com.devson.NeverMiss.ui.screens.detail.CommonStreakDetailScreen
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface IntervalTimerSettingsEntryPoint {
    fun intervalTimerSettingsRepository(): com.devson.NeverMiss.core.reminder.interval.IntervalTimerSettingsRepository
    fun intervalTimerAlertPlayback(): com.devson.NeverMiss.core.reminder.interval.IntervalTimerAlertPlayback
}

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object CreateStreak : Screen("create?streakId={streakId}") {
        fun createRoute(streakId: Long = -1L) = "create?streakId=$streakId"
    }
    object StreakDetail : Screen("streak_detail/{type}/{id}") {
        fun createRoute(type: StreakType, id: Long) = "streak_detail/${type.name}/$id"
    }
    object Settings : Screen("settings")
    object AppPicker : Screen("app_picker")
    object AppStreaksDashboard : Screen("app_streaks?section={section}") {
        fun createRoute(section: String = "app") = "app_streaks?section=$section"
    }
    object AppStreakDetail : Screen("app_streak_detail/{appId}") {
        fun createRoute(appId: Long) = "app_streak_detail/$appId"
    }
    object AccountDetail : Screen("account_detail/{appId}/{accountId}") {
        fun createRoute(appId: Long, accountId: Long) = "account_detail/$appId/$accountId"
    }
    object TrackingDiagnostics : Screen("tracking_diagnostics")
    object Archive : Screen("archive")
    object IntervalTimers : Screen("interval_timers")
    object IntervalTimerSettings : Screen("interval_timer_settings")
}

@Composable
fun NeverMissNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Home.route,
    liquidGlassEnabled: Boolean = false
) {
    val bottomBarRoutes = remember {
        NavDestination.entries.map { it.route }.toSet()
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute != null && currentRoute in bottomBarRoutes

    val backdropLayer = rememberGraphicsLayer()
    var containerOffsetInRoot by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                containerOffsetInRoot = coords.positionInRoot()
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (showBottomBar && liquidGlassEnabled) {
                        Modifier.drawWithContent {
                            backdropLayer.record {
                                this@drawWithContent.drawContent()
                            }
                            drawLayer(backdropLayer)
                        }
                    } else {
                        Modifier
                    }
                )
        ) {
            NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route
                val initialIdx = NavDestination.entries.indexOfFirst { it.route == initialRoute }
                val targetIdx = NavDestination.entries.indexOfFirst { it.route == targetRoute }
                if (initialIdx >= 0 && targetIdx >= 0 && initialIdx != targetIdx) {
                    val direction = if (targetIdx > initialIdx) 1 else -1
                    fadeIn(animationSpec = tween(260, easing = FastOutSlowInEasing)) + slideInHorizontally(
                        initialOffsetX = { fullWidth -> direction * (fullWidth / 5) },
                        animationSpec = tween(260, easing = FastOutSlowInEasing)
                    )
                } else {
                    fadeIn(animationSpec = tween(220)) + slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth / 4 },
                        animationSpec = tween(220)
                    )
                }
            },
            exitTransition = {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route
                val initialIdx = NavDestination.entries.indexOfFirst { it.route == initialRoute }
                val targetIdx = NavDestination.entries.indexOfFirst { it.route == targetRoute }
                if (initialIdx >= 0 && targetIdx >= 0 && initialIdx != targetIdx) {
                    val direction = if (targetIdx > initialIdx) -1 else 1
                    fadeOut(animationSpec = tween(220, easing = FastOutSlowInEasing)) + slideOutHorizontally(
                        targetOffsetX = { fullWidth -> direction * (fullWidth / 5) },
                        animationSpec = tween(220, easing = FastOutSlowInEasing)
                    )
                } else {
                    fadeOut(animationSpec = tween(180)) + slideOutHorizontally(
                        targetOffsetX = { fullWidth -> -fullWidth / 4 },
                        animationSpec = tween(180)
                    )
                }
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
                onNavigateToStreakDetail = { id, type ->
                    navController.navigate(Screen.StreakDetail.createRoute(type, id))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToAppStreaks = {
                    navController.navigate(Screen.AppStreaksDashboard.createRoute("app")) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToAppPicker = {
                    navController.navigate(Screen.AppPicker.route)
                },
                onNavigateToStreaksSection = { section ->
                    navController.navigate(Screen.AppStreaksDashboard.createRoute(section)) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToArchive = {
                    navController.navigate(Screen.Archive.route)
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

        composable(
            route = Screen.StreakDetail.route,
            arguments = listOf(
                navArgument("type") {
                    type = NavType.StringType
                    defaultValue = StreakType.NORMAL.name
                },
                navArgument("id") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val typeStr = backStackEntry.arguments?.getString("type") ?: StreakType.NORMAL.name
            val type = try {
                StreakType.valueOf(typeStr)
            } catch (e: Exception) {
                StreakType.NORMAL
            }
            val id = backStackEntry.arguments?.getLong("id") ?: -1L

            CommonStreakDetailScreen(
                type = type,
                id = id,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { editId ->
                    navController.navigate(Screen.CreateStreak.createRoute(editId))
                },
                onNavigateToAccountDetail = { appId, accountId ->
                    navController.navigate(Screen.AccountDetail.createRoute(appId, accountId))
                }
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
                onNavigateToAppStreaks = {
                    navController.navigate(Screen.AppStreaksDashboard.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDiagnostics = { navController.navigate(Screen.TrackingDiagnostics.route) }
            )
        }

        composable(route = Screen.AppPicker.route) {
            InstalledAppPickerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AppStreaksDashboard.route,
            arguments = listOf(
                navArgument("section") {
                    type = NavType.StringType
                    defaultValue = "app"
                }
            )
        ) { backStackEntry ->
            val section = backStackEntry.arguments?.getString("section") ?: "app"
            AppStreakDashboardScreen(
                initialSection = section,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAppPicker = { navController.navigate(Screen.AppPicker.route) },
                onNavigateToAppDetail = { appId ->
                    navController.navigate(Screen.StreakDetail.createRoute(StreakType.APP, appId))
                },
                onNavigateToNormalDetail = { streakId ->
                    navController.navigate(Screen.StreakDetail.createRoute(StreakType.NORMAL, streakId))
                },
                onNavigateToEditNormal = { streakId ->
                    navController.navigate(Screen.CreateStreak.createRoute(streakId))
                },
                onNavigateToCreateNormal = {
                    navController.navigate(Screen.CreateStreak.createRoute())
                },
                onNavigateToDiagnostics = { navController.navigate(Screen.TrackingDiagnostics.route) },
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
                }
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
            CommonStreakDetailScreen(
                type = StreakType.APP,
                id = appId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { editId ->
                    navController.navigate(Screen.CreateStreak.createRoute(editId))
                },
                onNavigateToAccountDetail = { currentAppId, accountId ->
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

        composable(route = Screen.TrackingDiagnostics.route) {
            TrackingDiagnosticsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.Archive.route) {
            ArchiveScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToStreakDetail = { id, type ->
                    navController.navigate(Screen.StreakDetail.createRoute(type, id))
                }
            )
        }

        composable(route = Screen.IntervalTimers.route) {
            com.devson.NeverMiss.ui.screens.intervaltimer.IntervalTimerScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate(Screen.IntervalTimerSettings.route) }
            )
        }

        composable(route = Screen.IntervalTimerSettings.route) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val entryPoint = dagger.hilt.android.EntryPointAccessors.fromApplication(
                context.applicationContext,
                IntervalTimerSettingsEntryPoint::class.java
            )
            com.devson.NeverMiss.ui.screens.intervaltimer.IntervalTimerSettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                settingsRepository = entryPoint.intervalTimerSettingsRepository(),
                alertPlayback = entryPoint.intervalTimerAlertPlayback()
            )
        }
        }
    }

        AnimatedVisibility(
            visible = showBottomBar,
            enter = fadeIn(tween(180)) + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut(tween(150)) + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            FloatingCapsuleNavigationBar(
                currentRoute = currentRoute ?: Screen.Home.route,
                onNavigateToRoute = { route ->
                    if (route != currentRoute) {
                        val targetRoute = if (route == Screen.AppStreaksDashboard.route) {
                            Screen.AppStreaksDashboard.createRoute("app")
                        } else {
                            route
                        }
                        navController.navigate(targetRoute) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                liquidGlassEnabled = liquidGlassEnabled,
                backdropLayer = if (liquidGlassEnabled) backdropLayer else null,
                containerOffsetInRoot = containerOffsetInRoot
            )
        }
    }
}
