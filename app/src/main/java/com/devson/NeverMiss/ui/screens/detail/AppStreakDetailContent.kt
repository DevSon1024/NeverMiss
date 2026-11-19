package com.devson.NeverMiss.ui.screens.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devson.NeverMiss.domain.model.SharedStreakDetail
import com.devson.NeverMiss.domain.model.apptracking.TrackedAccount
import com.devson.NeverMiss.ui.theme.SecondaryTeal
import java.time.YearMonth

@Composable
fun AppStreakDetailContent(
    detail: SharedStreakDetail,
    completedDates: Set<String>,
    currentMonth: YearMonth,
    accounts: List<TrackedAccount>,
    onMonthChange: (YearMonth) -> Unit,
    onAccountClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        if (accounts.size <= 1) {
            // Single Account or App-Level Calendar
            MonthlyCompletionDashboard(
                completedDates = completedDates,
                missedDates = emptySet(),
                currentMonth = currentMonth,
                onMonthChange = onMonthChange
            )
        } else {
            // Multi-Account Sub-Streaks Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "TRACKED ACCOUNTS (${accounts.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                accounts.forEach { account ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onAccountClick(account.id) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

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

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = "View Account",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Monthly Completion Dashboard for the overall app
            MonthlyCompletionDashboard(
                completedDates = completedDates,
                missedDates = emptySet(),
                currentMonth = currentMonth,
                onMonthChange = onMonthChange
            )
        }
    }
}
