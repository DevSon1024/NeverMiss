package com.devson.NeverMiss.ui.screens.detail

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.devson.NeverMiss.domain.model.SharedStreakDetail
import com.devson.NeverMiss.ui.components.StreakQuickSettingsSheet
import com.devson.NeverMiss.ui.components.StreakQuickSettingsTarget

@Composable
fun StreakManagementMenu(
    detail: SharedStreakDetail,
    onTogglePause: () -> Unit,
    onToggleArchive: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(
        onClick = { expanded = true },
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = "Streak Actions",
            tint = MaterialTheme.colorScheme.onSurface
        )
    }

    if (expanded) {
        StreakQuickSettingsSheet(
            target = StreakQuickSettingsTarget(
                id = detail.id,
                type = detail.type,
                name = detail.name,
                isPaused = detail.isPaused,
                isArchived = detail.isArchived
            ),
            onDismiss = { expanded = false },
            onEdit = {
                expanded = false
                onEdit()
            },
            onTogglePause = {
                expanded = false
                onTogglePause()
            },
            onToggleArchive = {
                expanded = false
                onToggleArchive()
            },
            onDelete = {
                expanded = false
                onDelete()
            }
        )
    }
}
