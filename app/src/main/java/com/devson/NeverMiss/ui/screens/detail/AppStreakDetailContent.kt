package com.devson.NeverMiss.ui.screens.detail

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devson.NeverMiss.core.util.AccountIcon
import com.devson.NeverMiss.core.util.AppClock
import com.devson.NeverMiss.domain.model.SharedStreakDetail
import com.devson.NeverMiss.domain.model.StreakCalculator
import com.devson.NeverMiss.domain.model.apptracking.TrackedAccount
import com.devson.NeverMiss.ui.components.AccountQuickSettingsSheet
import com.devson.NeverMiss.ui.components.UndoTodayStreakConfirmDialog
import com.devson.NeverMiss.ui.theme.SecondaryTeal
import com.devson.NeverMiss.ui.theme.SuccessGreen
import java.time.YearMonth

@Composable
fun AppStreakDetailContent(
    detail: SharedStreakDetail,
    completedDates: Set<String>,
    currentMonth: YearMonth,
    accounts: List<TrackedAccount>,
    onMonthChange: (YearMonth) -> Unit,
    onAccountClick: (Long) -> Unit,
    onMarkAccountDone: (Long) -> Unit = {},
    onUndoAccountDone: (Long) -> Unit = {},
    onRenameAccount: (Long, String) -> Unit = { _, _ -> },
    onToggleAccountEnabled: (Long) -> Unit = {},
    onDeleteAccount: (Long) -> Unit = {},
    onAddAccountClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var undoTargetAccount by remember { mutableStateOf<TrackedAccount?>(null) }
    var sheetTargetAccount by remember { mutableStateOf<TrackedAccount?>(null) }
    var renameTargetAccount by remember { mutableStateOf<TrackedAccount?>(null) }
    var deleteTargetAccount by remember { mutableStateOf<TrackedAccount?>(null) }
    val context = LocalContext.current
    val packageName = detail.description.orEmpty()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        if (packageName.isNotBlank()) {
            OutlinedButton(
                onClick = {
                    try {
                        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                        if (launchIntent != null) {
                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(launchIntent)
                        } else {
                            Toast.makeText(context, "Unable to launch ${detail.name}", Toast.LENGTH_SHORT).show()
                        }
                    } catch (_: Exception) {
                        Toast.makeText(context, "Unable to launch ${detail.name}", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Launch ${detail.name}",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        when {
            // Case 1: No Tracked Accounts
            accounts.isEmpty() -> {
                NoAccountsEmptyState(onAddAccountClick = onAddAccountClick)
            }

            // Case 2: Exactly ONE Tracked Account
            accounts.size == 1 -> {
                val account = accounts.first()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TRACKED ACCOUNT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    TextButton(onClick = onAddAccountClick) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Account")
                    }
                }

                AccountCard(
                    account = account,
                    onClick = { onAccountClick(account.id) },
                    onLongClick = { sheetTargetAccount = account },
                    onMarkDone = { onMarkAccountDone(account.id) },
                    onRequestUndo = { undoTargetAccount = account }
                )

                Text(
                    text = "MONTHLY ACTIVITY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                MonthlyCompletionDashboard(
                    completedDates = completedDates,
                    missedDates = emptySet(),
                    currentMonth = currentMonth,
                    onMonthChange = onMonthChange
                )
            }

            // Case 3: MORE THAN ONE Tracked Account
            else -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TRACKED ACCOUNTS (${accounts.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    TextButton(onClick = onAddAccountClick) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Account")
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    accounts.forEach { account ->
                        AccountCard(
                            account = account,
                            onClick = { onAccountClick(account.id) },
                            onLongClick = { sheetTargetAccount = account },
                            onMarkDone = { onMarkAccountDone(account.id) },
                            onRequestUndo = { undoTargetAccount = account }
                        )
                    }
                }
            }
        }
    }

    undoTargetAccount?.let { account ->
        UndoTodayStreakConfirmDialog(
            streakName = account.accountName,
            onDismiss = { undoTargetAccount = null },
            onConfirm = {
                undoTargetAccount = null
                onUndoAccountDone(account.id)
            }
        )
    }

    sheetTargetAccount?.let { account ->
        AccountQuickSettingsSheet(
            accountName = account.accountName,
            isEnabled = account.isEnabled,
            onDismiss = { sheetTargetAccount = null },
            onRename = {
                sheetTargetAccount = null
                renameTargetAccount = account
            },
            onToggleEnabled = {
                sheetTargetAccount = null
                onToggleAccountEnabled(account.id)
            },
            onDelete = {
                sheetTargetAccount = null
                deleteTargetAccount = account
            }
        )
    }

    renameTargetAccount?.let { account ->
        var editName by remember(account.id) { mutableStateOf(account.accountName) }
        AlertDialog(
            onDismissRequest = { renameTargetAccount = null },
            title = { Text("Rename Account") },
            text = {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Account Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = editName.trim()
                        if (trimmed.isNotEmpty()) {
                            onRenameAccount(account.id, trimmed)
                            renameTargetAccount = null
                        }
                    },
                    enabled = editName.trim().isNotEmpty()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTargetAccount = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    deleteTargetAccount?.let { account ->
        AlertDialog(
            onDismissRequest = { deleteTargetAccount = null },
            title = { Text("Delete Account?") },
            text = { Text("This will permanently remove '${account.accountName}' and its logged completion history.") },
            confirmButton = {
                Button(
                    onClick = {
                        deleteTargetAccount = null
                        onDeleteAccount(account.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTargetAccount = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AccountCard(
    account: TrackedAccount,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMarkDone: () -> Unit,
    onRequestUndo: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val todayStr = StreakCalculator.formatDate(AppClock.now())
    val isDoneToday = account.lastCompletedDate == todayStr

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(
            1.dp,
            if (isDoneToday) SuccessGreen.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AccountIcon(
                iconSize = 42.dp,
                clipShape = RoundedCornerShape(12.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.accountName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Whatshot,
                        contentDescription = null,
                        tint = SecondaryTeal,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${account.currentStreak} day streak",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryTeal,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            IconButton(
                onClick = {
                    if (isDoneToday) onRequestUndo() else onMarkDone()
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isDoneToday) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                    contentDescription = if (isDoneToday) "Completed Today" else "Mark Streak Done",
                    tint = if (isDoneToday) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "View Account",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun NoAccountsEmptyState(
    onAddAccountClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AccountIcon(
                iconSize = 56.dp,
                clipShape = RoundedCornerShape(16.dp)
            )

            Text(
                text = "No Tracked Accounts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Add named accounts (e.g., Personal, Work, Alt) to track separate daily sub-streaks for this app.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onAddAccountClick,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Tracked Account")
            }
        }
    }
}
