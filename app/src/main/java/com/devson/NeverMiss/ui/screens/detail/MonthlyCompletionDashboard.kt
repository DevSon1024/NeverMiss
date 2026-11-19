package com.devson.NeverMiss.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devson.NeverMiss.core.util.AppClock
import com.devson.NeverMiss.domain.model.StreakCalculator
import com.devson.NeverMiss.ui.theme.AccentCoral
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MonthlyCompletionDashboard(
    completedDates: Set<String>,
    missedDates: Set<String> = emptySet(),
    currentMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = AppClock.now()
    val todayStr = remember(today) { StreakCalculator.formatDate(today) }

    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfWeek = currentMonth.atDay(1).dayOfWeek.value // 1 (Mon) to 7 (Sun)
    val leadingEmptyCells = (firstDayOfWeek - 1) % 7

    val completedThisMonthCount = remember(completedDates, currentMonth) {
        val monthPrefix = currentMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        completedDates.count { it.startsWith(monthPrefix) }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Month Navigation Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onMonthChange(currentMonth.minusMonths(1)) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous Month",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                val monthTitle = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
                Text(
                    text = monthTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(onClick = { onMonthChange(currentMonth.plusMonths(1)) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next Month",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Day of week labels (M T W T F S S)
            val dayHeaders = listOf("M", "T", "W", "T", "F", "S", "S")
            Row(modifier = Modifier.fillMaxWidth()) {
                dayHeaders.forEach { label ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar Grid
            val totalCells = leadingEmptyCells + daysInMonth
            val totalRows = (totalCells + 6) / 7

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (row in 0 until totalRows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (col in 0 until 7) {
                            val cellIndex = row * 7 + col
                            val dayNumber = cellIndex - leadingEmptyCells + 1

                            if (dayNumber in 1..daysInMonth) {
                                val date = currentMonth.atDay(dayNumber)
                                val dateStr = StreakCalculator.formatDate(date)
                                val isCompleted = completedDates.contains(dateStr)
                                val isMissed = missedDates.contains(dateStr)
                                val isToday = (dateStr == todayStr)
                                val isFuture = date.isAfter(today)

                                DashboardDayCell(
                                    day = dayNumber,
                                    isCompleted = isCompleted,
                                    isMissed = isMissed,
                                    isToday = isToday,
                                    isFuture = isFuture,
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Summary & Legend Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$completedThisMonthCount completed this month",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Text("Done", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(AccentCoral)
                        )
                        Text("Missed", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardDayCell(
    day: Int,
    isCompleted: Boolean,
    isMissed: Boolean,
    isToday: Boolean,
    isFuture: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isCompleted -> MaterialTheme.colorScheme.primary
        isMissed -> AccentCoral.copy(alpha = 0.2f)
        else -> Color.Transparent
    }

    val contentColor = when {
        isCompleted -> MaterialTheme.colorScheme.onPrimary
        isMissed -> AccentCoral
        isFuture -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(backgroundColor)
            .then(
                if (isToday && !isCompleted) {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                } else if (isMissed) {
                    Modifier.border(1.dp, AccentCoral.copy(alpha = 0.5f), CircleShape)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isCompleted) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
        } else if (isMissed) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
        } else {
            Text(
                text = day.toString(),
                color = contentColor,
                fontSize = 12.sp,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
