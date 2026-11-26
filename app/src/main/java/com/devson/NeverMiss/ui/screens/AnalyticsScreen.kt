package com.devson.NeverMiss.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devson.NeverMiss.core.util.AppClock
import com.devson.NeverMiss.ui.theme.*
import com.devson.NeverMiss.viewmodel.AnalyticsState
import com.devson.NeverMiss.viewmodel.AnalyticsViewModel
import com.devson.NeverMiss.viewmodel.StreakAnalyticsSummary
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToAppStreaks: () -> Unit = {},
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics & Insights", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Spacer(
                modifier = Modifier
                    .navigationBarsPadding()
                    .height(80.dp)
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            color = MaterialTheme.colorScheme.background
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(
                    top = 12.dp,
                    bottom = innerPadding.calculateBottomPadding() + 24.dp
                )
            ) {
                // Header Banner: Consistency Score
                item(key = "header_banner") {
                    ConsistencyScoreCard(state = state)
                }

                // Grid Stats Section
                item(key = "grid_stats") {
                    StatsGrid(state = state)
                }

                // Weekly Rate Bar Chart
                item(key = "weekly_chart") {
                    WeeklyRateChartCard(weeklyRates = state.weeklyCompletionRate)
                }

                // Heatmap Calendar Grid
                item(key = "heatmap_calendar") {
                    CompletionHeatmapCard(completionDensity = state.completionDensity)
                }

                // Individual Streak Breakdown Title
                item(key = "summaries_title") {
                    Text(
                        text = "INDIVIDUAL ROUTINE CONSISTENCY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                    )
                }

                // Individual Streak Cards
                items(state.summaries, key = { it.id }) { summary ->
                    IndividualStreakSummaryCard(summary = summary)
                }
            }
        }
    }
}

@Composable
private fun ConsistencyScoreCard(state: AnalyticsState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Overall Consistency",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = String.format("%.1f%%", state.overallConsistency),
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    color = PrimaryViolet
                )
                Text(
                    text = "${state.totalStreaks} Active Routines Tracked",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(PrimaryViolet.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FlashOn,
                    contentDescription = null,
                    tint = PrimaryViolet,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

@Composable
private fun StatsGrid(state: AnalyticsState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Total Logs", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${state.totalCompletions}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
        }

        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Icon(Icons.Default.Whatshot, contentDescription = null, tint = WarningAmber)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Best Streak", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${state.highestActiveStreak} Days", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun WeeklyRateChartCard(weeklyRates: List<Pair<String, Float>>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "WEEKLY RATE TRENDS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(start = 4.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                if (weeklyRates.isEmpty()) {
                    Text("No completion data available yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    val barBgColor = MaterialTheme.colorScheme.background

                    // Pre-allocate Paint objects outside Canvas draw scope for 120FPS performance
                    val valPaint = remember {
                        Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 28f
                            textAlign = Paint.Align.CENTER
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        }
                    }
                    val labelPaint = remember {
                        Paint().apply {
                            color = android.graphics.Color.parseColor("#94A3B8")
                            textSize = 28f
                            textAlign = Paint.Align.CENTER
                            typeface = Typeface.DEFAULT
                        }
                    }

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val barWidth = 40.dp.toPx()
                        val barSpacing = (canvasWidth - (barWidth * weeklyRates.size)) / (weeklyRates.size - 1)
                        val maxBarHeight = canvasHeight - 30.dp.toPx()

                        weeklyRates.forEachIndexed { index, (dayLabel, rate) ->
                            val x = index * (barWidth + barSpacing)
                            val barHeight = maxBarHeight * rate

                            // Draw empty background bar
                            drawRoundRect(
                                color = barBgColor,
                                topLeft = Offset(x, 0f),
                                size = Size(barWidth, maxBarHeight),
                                cornerRadius = CornerRadius(8.dp.toPx())
                            )

                            // Draw active gradient bar
                            if (barHeight > 0f) {
                                drawRoundRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(PrimaryViolet, SecondaryTeal)
                                    ),
                                    topLeft = Offset(x, maxBarHeight - barHeight),
                                    size = Size(barWidth, barHeight),
                                    cornerRadius = CornerRadius(8.dp.toPx())
                                )
                            }

                            // Render text values with zero allocation
                            drawIntoCanvas { canvas ->
                                canvas.nativeCanvas.drawText(
                                    String.format("%.0f%%", rate * 100),
                                    x + (barWidth / 2f),
                                    (maxBarHeight - barHeight - 8.dp.toPx()).coerceAtLeast(18f),
                                    valPaint
                                )
                                canvas.nativeCanvas.drawText(
                                    dayLabel,
                                    x + (barWidth / 2f),
                                    canvasHeight - 6.dp.toPx(),
                                    labelPaint
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompletionHeatmapCard(completionDensity: Map<String, Int>) {
    val currentMonth = YearMonth.from(AppClock.now())
    val monthLabel = currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
    val yearLabel = currentMonth.year.toString()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "COMPLETION HEATMAP ($monthLabel $yearLabel)".uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(start = 4.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                        Text(
                            text = day,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(36.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                val cellEmptyColor = MaterialTheme.colorScheme.background
                val outlineColor = MaterialTheme.colorScheme.outlineVariant

                // Pre-allocated Paint objects for zero-allocation performance inside draw scope
                val activeDayPaint = remember {
                    Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 28f
                        textAlign = Paint.Align.CENTER
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                }
                val emptyDayPaint = remember {
                    Paint().apply {
                        color = android.graphics.Color.parseColor("#475569")
                        textSize = 28f
                        textAlign = Paint.Align.CENTER
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                ) {
                    val canvasWidth = size.width
                    val boxSize = 36.dp.toPx()
                    val spacing = (canvasWidth - (boxSize * 7)) / 6

                    val firstDayOfMonth = currentMonth.atDay(1)
                    val totalDays = currentMonth.lengthOfMonth()
                    val offset = firstDayOfMonth.dayOfWeek.value - 1

                    var dayCounter = 1
                    for (week in 0..5) {
                        for (dayOfWeek in 0..6) {
                            val index = (week * 7) + dayOfWeek
                            if (index < offset || dayCounter > totalDays) {
                                continue
                            }
                            
                            val dateStr = String.format("%s-%02d", currentMonth.toString(), dayCounter)
                            val completionsCount = completionDensity[dateStr] ?: 0

                            val cellColor = when (completionsCount) {
                                0 -> cellEmptyColor
                                1 -> SuccessGreen.copy(alpha = 0.35f)
                                2 -> SuccessGreen.copy(alpha = 0.7f)
                                else -> SuccessGreen
                            }

                            val x = dayOfWeek * (boxSize + spacing)
                            val y = week * (boxSize + 8.dp.toPx())

                            drawRoundRect(
                                color = cellColor,
                                topLeft = Offset(x, y),
                                size = Size(boxSize, boxSize),
                                cornerRadius = CornerRadius(8.dp.toPx())
                            )

                            if (completionsCount == 0) {
                                drawRoundRect(
                                    color = outlineColor,
                                    topLeft = Offset(x, y),
                                    size = Size(boxSize, boxSize),
                                    cornerRadius = CornerRadius(8.dp.toPx()),
                                    style = Stroke(1.dp.toPx())
                                )
                            }

                            drawIntoCanvas { canvas ->
                                val paint = if (completionsCount > 0) activeDayPaint else emptyDayPaint
                                val fontMetrics = paint.fontMetrics
                                val textOffset = (fontMetrics.descent - fontMetrics.ascent) / 2 - fontMetrics.descent
                                
                                canvas.nativeCanvas.drawText(
                                    dayCounter.toString(),
                                    x + (boxSize / 2f),
                                    y + (boxSize / 2f) + textOffset,
                                    paint
                                )
                            }

                            dayCounter++
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IndividualStreakSummaryCard(summary: StreakAnalyticsSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(summary.color).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(summary.emoji, fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = summary.title,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Whatshot,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Current: ${summary.currentStreak}d | Best: ${summary.longestStreak}d",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = String.format("%.0f%%", summary.completionPercentage),
                    fontWeight = FontWeight.Black,
                    color = Color(summary.color),
                    fontSize = 18.sp
                )
                Text("Consistency", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
