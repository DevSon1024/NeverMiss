package com.devson.NeverMiss

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.rememberNavController
import com.devson.NeverMiss.data.local.preferences.UserPreferences
import com.devson.NeverMiss.presentation.navigation.NeverMissNavGraph
import com.devson.NeverMiss.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeState by userPreferences.themeFlow.collectAsState(initial = UserPreferences.AppTheme.SYSTEM)
            val dynamicColorState by userPreferences.dynamicColorsFlow.collectAsState(initial = true)
            val appLockPinState by userPreferences.appLockPinFlow.collectAsState(initial = "")

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
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            NeverMissNavGraph(navController = navController)
                        }
                    }
                }
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
            .background(ObsidianBg)
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
                color = TextPrimaryDark
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isError) "Incorrect PIN. Try again." else "Enter your 4-digit security PIN",
                fontSize = 13.sp,
                color = if (isError) AccentCoral else TextSecondaryDark
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
                                CardSlate
                            }
                        )
                        .border(1.dp, BorderSlate, CircleShape)
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
                                    .background(CardSlate)
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
                                    Icon(Icons.Default.Backspace, contentDescription = "Backspace", tint = TextPrimaryDark)
                                } else {
                                    Text(
                                        text = key,
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark
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