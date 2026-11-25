package com.devson.NeverMiss.ui.screens.intervaltimer

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devson.NeverMiss.domain.model.IntervalTimer
import com.devson.NeverMiss.domain.model.IntervalTimerMode
import com.devson.NeverMiss.domain.model.IntervalTimerStatus
import com.devson.NeverMiss.ui.theme.SecondaryTeal
import com.devson.NeverMiss.ui.theme.WarningAmber

@Composable
fun IntervalTimerCard(
    timer: IntervalTimer,
    currentNowMillis: Long,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    val statusColor by animateColorAsState(
        targetValue = when (timer.status) {
            IntervalTimerStatus.RUNNING -> SecondaryTeal
            IntervalTimerStatus.PAUSED -> WarningAmber
            IntervalTimerStatus.COMPLETED -> MaterialTheme.colorScheme.primary
            IntervalTimerStatus.STOPPED -> MaterialTheme.colorScheme.error
            IntervalTimerStatus.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "statusColor"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (timer.status == IntervalTimerStatus.RUNNING) {
                statusColor.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Title, Mode chip & overflow menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = timer.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = IntervalTimerUiHelper.formatConfiguredDuration(timer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Status Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = timer.status.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    // More Menu
                    Box {
                        IconButton(onClick = { isMenuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Timer options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        DropdownMenu(
                            expanded = isMenuExpanded,
                            onDismissRequest = { isMenuExpanded = false }
                        ) {
                            if (timer.status != IntervalTimerStatus.RUNNING) {
                                DropdownMenuItem(
                                    text = { Text("Edit") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Edit, contentDescription = null)
                                    },
                                    onClick = {
                                        isMenuExpanded = false
                                        onEdit()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    isMenuExpanded = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            // Body: Remaining time / Occurrences display
            val remainingMillis = when (timer.status) {
                IntervalTimerStatus.RUNNING -> {
                    val trigger = timer.nextTriggerAtMillis ?: currentNowMillis
                    (trigger - currentNowMillis).coerceAtLeast(0L)
                }
                IntervalTimerStatus.PAUSED -> {
                    timer.remainingAtPauseMillis ?: 0L
                }
                IntervalTimerStatus.IDLE -> {
                    if (timer.mode == IntervalTimerMode.COUNTDOWN) timer.durationMillis else timer.intervalMillis
                }
                IntervalTimerStatus.COMPLETED,
                IntervalTimerStatus.STOPPED -> 0L
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (timer.status == IntervalTimerStatus.RUNNING || timer.status == IntervalTimerStatus.PAUSED) "Remaining" else "Timer Status",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (timer.status == IntervalTimerStatus.COMPLETED) {
                            "Completed"
                        } else if (timer.status == IntervalTimerStatus.STOPPED) {
                            "Stopped"
                        } else {
                            IntervalTimerUiHelper.formatDuration(remainingMillis)
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (timer.status == IntervalTimerStatus.RUNNING) statusColor else MaterialTheme.colorScheme.onSurface
                    )
                }

                if (timer.mode == IntervalTimerMode.INTERVAL) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Occurrences",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = IntervalTimerUiHelper.formatOccurrences(timer),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Action Controls based on status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (timer.status) {
                    IntervalTimerStatus.IDLE -> {
                        Button(
                            onClick = onStart,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start")
                        }
                    }
                    IntervalTimerStatus.RUNNING -> {
                        OutlinedButton(
                            onClick = onStop,
                            shape = CircleShape,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Stop")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onPause,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = WarningAmber)
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pause")
                        }
                    }
                    IntervalTimerStatus.PAUSED -> {
                        OutlinedButton(
                            onClick = onStop,
                            shape = CircleShape,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Stop")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onResume,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Resume")
                        }
                    }
                    IntervalTimerStatus.COMPLETED,
                    IntervalTimerStatus.STOPPED -> {
                        Button(
                            onClick = onRestart,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restart")
                        }
                    }
                }
            }
        }
    }
}
