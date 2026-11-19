package com.devson.NeverMiss.ui.screens.detail

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.devson.NeverMiss.domain.model.SharedStreakDetail
import com.devson.NeverMiss.ui.theme.AccentCoral

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

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        // Edit Action
        DropdownMenuItem(
            text = { Text("Edit Streak") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null
                )
            },
            onClick = {
                expanded = false
                onEdit()
            }
        )

        // Pause / Resume Action
        DropdownMenuItem(
            text = { Text(if (detail.isPaused) "Resume Streak" else "Pause Streak") },
            leadingIcon = {
                Icon(
                    imageVector = if (detail.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = null
                )
            },
            onClick = {
                expanded = false
                onTogglePause()
            }
        )

        // Archive / Restore Action
        DropdownMenuItem(
            text = { Text(if (detail.isArchived) "Restore from Vault" else "Move to Archive") },
            leadingIcon = {
                Icon(
                    imageVector = if (detail.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                    contentDescription = null
                )
            },
            onClick = {
                expanded = false
                onToggleArchive()
            }
        )

        HorizontalDivider()

        // Delete Action
        DropdownMenuItem(
            text = { Text("Delete Streak", color = AccentCoral) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = AccentCoral
                )
            },
            onClick = {
                expanded = false
                onDelete()
            }
        )
    }
}
