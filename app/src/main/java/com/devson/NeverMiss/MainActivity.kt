package com.devson.NeverMiss

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.rememberNavController
import com.devson.NeverMiss.data.local.preferences.UserPreferences
import com.devson.NeverMiss.navigation.NeverMissNavGraph
import com.devson.NeverMiss.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint
import com.devson.NeverMiss.core.reminder.ReminderScheduler
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferences: UserPreferences

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    @Inject
    lateinit var processAppUsageEventsUseCase: com.devson.NeverMiss.domain.usecase.appstreaks.ProcessAppUsageEventsUseCase

    private val pendingNotificationTarget = mutableStateOf<Pair<Long, com.devson.NeverMiss.domain.model.StreakType>?>(null)

    private fun extractNotificationTarget(intent: Intent?) {
        val streakId = intent?.getLongExtra("STREAK_ID", -1L) ?: -1L
        if (streakId > 0L) {
            val typeName = intent?.getStringExtra("STREAK_TYPE")
            val streakType = try {
                if (typeName != null) com.devson.NeverMiss.domain.model.StreakType.valueOf(typeName)
                else com.devson.NeverMiss.domain.model.StreakType.NORMAL
            } catch (_: Exception) {
                com.devson.NeverMiss.domain.model.StreakType.NORMAL
            }
            pendingNotificationTarget.value = streakId to streakType
            intent?.removeExtra("STREAK_ID")
            intent?.removeExtra("STREAK_TYPE")
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractNotificationTarget(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        extractNotificationTarget(intent)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        // Background reschedule active reminders on app start
        lifecycleScope.launch(Dispatchers.IO) {
            reminderScheduler.scheduleAllActiveReminders()
        }

        setContent {
            val themeState by userPreferences.themeFlow.collectAsState(initial = UserPreferences.AppTheme.SYSTEM)
            val dynamicColorState by userPreferences.dynamicColorsFlow.collectAsState(initial = true)
            val liquidGlassNavBarState by userPreferences.liquidGlassNavBarFlow.collectAsState(initial = false)
            val appLockPinState by userPreferences.appLockPinFlow.collectAsState(initial = "")
            val devOverrideEnabledState by userPreferences.devOverrideDateEnabledFlow.collectAsState(initial = false)
            val devOverrideDateState by userPreferences.devOverrideDateFlow.collectAsState(initial = "")

            LaunchedEffect(devOverrideEnabledState, devOverrideDateState) {
                com.devson.NeverMiss.core.util.AppClock.init(devOverrideEnabledState, devOverrideDateState)
            }

            val darkTheme = when (themeState) {
                UserPreferences.AppTheme.LIGHT -> false
                UserPreferences.AppTheme.DARK -> true
                UserPreferences.AppTheme.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            NeverMissTheme(
                darkTheme = darkTheme,
                dynamicColor = dynamicColorState
            ) {
                var isUnlocked by remember { mutableStateOf(false) }
                
                LaunchedEffect(appLockPinState) {
                    // If no PIN is configured, unlock automatically
                    if (appLockPinState.isEmpty()) {
                        isUnlocked = true
                    }
                }

                if (!isUnlocked && appLockPinState.isNotEmpty()) {
                    AppLockScreen(
                        correctPin = appLockPinState,
                        onUnlockSuccess = { isUnlocked = true }
                    )
                } else {
                    val navController = rememberNavController()
                    val notificationTarget by pendingNotificationTarget
                    LaunchedEffect(notificationTarget) {
                        val target = notificationTarget
                        if (target != null) {
                            navController.navigate(
                                com.devson.NeverMiss.navigation.Screen.StreakDetail.createRoute(
                                    target.second,
                                    target.first
                                )
                            ) {
                                launchSingleTop = true
                            }
                            pendingNotificationTarget.value = null
                        }
                    }
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        NeverMissNavGraph(
                            navController = navController,
                            liquidGlassEnabled = liquidGlassNavBarState
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Fast-path foreground reconciliation catch-up
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                processAppUsageEventsUseCase()
            } catch (e: Exception) {
                // Ignore transient errors on resume
            }
        }
    }
}

@Composable
fun AppLockScreen(
    correctPin: String,
    onUnlockSuccess: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        // Top status info
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "App Locked",
                tint = if (isError) AccentCoral else PrimaryViolet,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "NeverMiss is Locked",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isError) "Incorrect PIN. Try again." else "Enter your 4-digit security PIN",
                fontSize = 13.sp,
                color = if (isError) AccentCoral else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Dot indicators representing entered digits
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0..3) {
                val filled = i < enteredPin.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (filled) {
                                if (isError) AccentCoral else PrimaryViolet
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                )
            }
        }

        // Keypad grid
        Column(
            modifier = Modifier.padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "back")
            )

            keys.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    row.forEach { key ->
                        if (key.isEmpty()) {
                            // Blank spacer for alignment
                            Box(modifier = Modifier.size(72.dp))
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                                        isError = false
                                        if (key == "back") {
                                            if (enteredPin.isNotEmpty()) {
                                                enteredPin = enteredPin.dropLast(1)
                                            }
                                        } else {
                                            if (enteredPin.length < 4) {
                                                enteredPin += key
                                                if (enteredPin.length == 4) {
                                                    if (enteredPin == correctPin) {
                                                        onUnlockSuccess()
                                                    } else {
                                                        isError = true
                                                        enteredPin = ""
                                                    }
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (key == "back") {
                                    Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Backspace", tint = MaterialTheme.colorScheme.onSurface)
                                } else {
                                    Text(
                                        text = key,
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}