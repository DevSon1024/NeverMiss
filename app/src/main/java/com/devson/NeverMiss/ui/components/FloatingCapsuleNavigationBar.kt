package com.devson.NeverMiss.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.devson.NeverMiss.navigation.Screen

enum class NavDestination(
    val route: String,
    val icon: ImageVector,
    val contentDescription: String
) {
    HOME(Screen.Home.route, Icons.Default.Home, "Home"),
    APP_STREAKS(Screen.AppStreaksDashboard.route, Icons.Default.Apps, "App Streaks"),
    ANALYTICS(Screen.Analytics.route, Icons.Default.Analytics, "Analytics"),
    SETTINGS(Screen.Settings.route, Icons.Default.Settings, "Settings")
}

@Composable
fun FloatingCapsuleNavigationBar(
    currentRoute: String,
    onNavigateToRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp).copy(alpha = 0.94f),
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier
                .height(60.dp)
                .fillMaxWidth(0.92f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavDestination.values().forEach { destination ->
                    val isSelected = currentRoute == destination.route
                    CapsuleNavItem(
                        destination = destination,
                        isSelected = isSelected,
                        onClick = {
                            if (!isSelected) {
                                onNavigateToRoute(destination.route)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CapsuleNavItem(
    destination: NavDestination,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val indicatorColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(durationMillis = 220),
        label = "capsule_indicator_color"
    )

    val iconTint by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 220),
        label = "capsule_icon_tint"
    )

    Box(
        modifier = Modifier
            .sizeIn(minWidth = 56.dp, minHeight = 48.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(width = 48.dp, height = 32.dp)
                    .clip(CircleShape)
                    .background(indicatorColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = destination.icon,
                    contentDescription = destination.contentDescription,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            )
        }
    }
}
