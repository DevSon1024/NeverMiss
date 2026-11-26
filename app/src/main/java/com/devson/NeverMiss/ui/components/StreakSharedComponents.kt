package com.devson.NeverMiss.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devson.NeverMiss.domain.model.StreakType
import com.devson.NeverMiss.domain.usecase.common.DeletionConfirmation
import com.devson.NeverMiss.ui.theme.AccentCoral
import com.devson.NeverMiss.ui.theme.SecondaryTeal

/**
 * Shared expandable speed-dial FAB overlay with a dim scrim rendered behind the menu options.
 */
@Composable
fun BoxScope.ExpandableStreakFabOverlay(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onDismiss: () -> Unit,
    onNormalClick: () -> Unit,
    onAppStreakClick: () -> Unit
) {
    BackHandler(enabled = isExpanded) {
        onDismiss()
    }

    AnimatedVisibility(
        visible = isExpanded,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )
    }

    Box(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .navigationBarsPadding()
            .padding(end = 16.dp, bottom = 96.dp)
    ) {
        ExpandableFabMenu(
            isExpanded = isExpanded,
            onToggle = onToggle,
            onNormalClick = onNormalClick,
            onAppStreakClick = onAppStreakClick
        )
    }
}

@Composable
private fun ExpandableFabMenu(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onNormalClick: () -> Unit,
    onAppStreakClick: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 45f else 0f,
        label = "fab_rotation"
    )

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.clickable(onClick = onNormalClick)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shadowElevation = 2.dp
                    ) {
                        Text(
                            text = "Normal Habit",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    SmallFloatingActionButton(
                        onClick = onNormalClick,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "New Habit"
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.clickable(onClick = onAppStreakClick)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shadowElevation = 2.dp
                    ) {
                        Text(
                            text = "App Streak",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    SmallFloatingActionButton(
                        onClick = onAppStreakClick,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Default.AppShortcut,
                            contentDescription = "New App Streak"
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onToggle,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = if (isExpanded) "Close" else "Add Streak",
                modifier = Modifier
                    .size(28.dp)
                    .rotate(rotation)
            )
        }
    }
}

data class StreakQuickSettingsTarget(
    val id: Long,
    val type: StreakType,
    val name: String,
    val isPaused: Boolean,
    val isArchived: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreakQuickSettingsSheet(
    target: StreakQuickSettingsTarget,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onTogglePause: () -> Unit,
    onToggleArchive: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = target.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (target.type == StreakType.NORMAL) "Habit Streak Settings" else "App Streak Settings",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()

            StreakSheetActionRow(
                icon = Icons.Default.Edit,
                label = "Edit Streak",
                onClick = onEdit
            )
            StreakSheetActionRow(
                icon = if (target.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                label = if (target.isPaused) "Resume Streak" else "Pause Streak",
                onClick = onTogglePause
            )
            StreakSheetActionRow(
                icon = if (target.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                label = if (target.isArchived) "Restore from Vault" else "Move to Archive",
                onClick = onToggleArchive
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            StreakSheetActionRow(
                icon = Icons.Default.Delete,
                label = "Delete Streak",
                tint = AccentCoral,
                onClick = onDelete
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun StreakSheetActionRow(
    icon: ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = tint
        )
    }
}

@Composable
fun EditAppStreakNameDialog(
    appId: Long,
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (Long, String) -> Unit
) {
    var appNameText by remember(appId) { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit App Streak", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Update the display name of this tracked application:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = appNameText,
                    onValueChange = { appNameText = it },
                    label = { Text("App Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = appNameText.trim()
                    if (trimmed.isNotEmpty()) {
                        onSave(appId, trimmed)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeleteStreakConfirmDialog(
    confirmation: DeletionConfirmation?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val targetName = confirmation?.name ?: "streak"
    val recordCount = confirmation?.affectedRecordCount ?: 0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Delete Streak?",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Are you sure you want to permanently delete \"$targetName\"? This action will remove $recordCount related record(s) and cannot be undone."
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun UndoTodayStreakConfirmDialog(
    streakName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Undo Today's Streak?",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Do you want to undo today's completion for \"$streakName\"?"
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentCoral,
                    contentColor = Color.White
                )
            ) {
                Text("Undo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountQuickSettingsSheet(
    accountName: String,
    isEnabled: Boolean,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onToggleEnabled: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = accountName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Account Settings",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()

            StreakSheetActionRow(
                icon = Icons.Default.Edit,
                label = "Rename Account",
                onClick = onRename
            )
            StreakSheetActionRow(
                icon = if (isEnabled) Icons.Default.Pause else Icons.Default.PlayArrow,
                label = if (isEnabled) "Disable Account" else "Enable Account",
                onClick = onToggleEnabled
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            StreakSheetActionRow(
                icon = Icons.Default.Delete,
                label = "Delete Account",
                tint = AccentCoral,
                onClick = onDelete
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
