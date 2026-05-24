package com.devson.NeverMiss.presentation.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devson.NeverMiss.domain.model.StreakCalculator
import com.devson.NeverMiss.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics Dashboard", color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.height(72.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateToHome) {
                        Icon(Icons.Default.Home, contentDescription = "Home")
                    }
                    IconButton(onClick = { /* Already here */ }) {
                        Icon(Icons.Default.Analytics, contentDescription = "Analytics", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header consistency card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black,
                                color = PrimaryViolet
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(PrimaryViolet.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⚡", fontSize = 32.sp)
                        }
                    }
                }

                // Grid Stats Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Completions", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${state.totalCompletions}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Icon(Icons.Default.Whatshot, contentDescription = null, tint = WarningAmber)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Max Streak", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${state.highestActiveStreak} Days", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                // WEEKLY CHART CANVAS DRAWING
                Text(
                    text = "WEEKLY RATE TRENDS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 8.dp)
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.weeklyCompletionRate.isEmpty()) {
                            Text("Insufficient data", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            val rates = state.weeklyCompletionRate
                            val barBgColor = MaterialTheme.colorScheme.background
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val canvasWidth = size.width
                                val canvasHeight = size.height
                                
                                val barWidth = 40.dp.toPx()
                                val barSpacing = (canvasWidth - (barWidth * rates.size)) / (rates.size - 1)
                                val maxBarHeight = canvasHeight - 30.dp.toPx()

                                rates.forEachIndexed { index, (dayLabel, rate) ->
                                    val x = index * (barWidth + barSpacing)
                                    val barHeight = maxBarHeight * rate
                                    
                                    // Draw background empty bar path
                                    drawRoundRect(
                                        color = barBgColor,
                                        topLeft = Offset(x, 0f),
                                        size = Size(barWidth, maxBarHeight),
                                        cornerRadius = CornerRadius(8.dp.toPx())
                                    )

                                    // Draw completed active bar
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

                                    // Draw rate label text
                                    drawIntoCanvas { canvas ->
                                        val paint = android.graphics.Paint().apply {
                                            color = android.graphics.Color.WHITE
                                            textSize = 10.sp.toPx()
                                            textAlign = android.graphics.Paint.Align.CENTER
                                            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                                        }
                                        canvas.nativeCanvas.drawText(
                                            String.format("%.0f%%", rate * 100),
                                            x + (barWidth / 2f),
                                            maxBarHeight - barHeight - 6.dp.toPx(),
                                            paint
                                        )
                                        
                                        paint.color = android.graphics.Color.parseColor("#94A3B8")
                                        paint.typeface = android.graphics.Typeface.DEFAULT
                                        canvas.nativeCanvas.drawText(
                                            dayLabel,
                                            x + (barWidth / 2f),
                                            canvasHeight - 6.dp.toPx(),
                                            paint
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // HEATMAP CALENDAR CANVAS DRAWING
                val currentMonth = YearMonth.now()
                val monthLabel = currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                val yearLabel = currentMonth.year.toString()

                Text(
                    text = "COMPLETION HEATMAP ($monthLabel $yearLabel)".uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Days Header row: M T W T F S S
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

                        // Heatmap Grid drawing Canvas
                        val cellEmptyColor = MaterialTheme.colorScheme.background
                        val outlineColor = MaterialTheme.colorScheme.outlineVariant
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
                            // DayOfWeek: Monday=1, Sunday=7. Offset is first day's dayOfWeek value - 1
                            val offset = firstDayOfMonth.dayOfWeek.value - 1

                            var dayCounter = 1
                            for (week in 0..5) {
                                for (dayOfWeek in 0..6) {
                                    val index = (week * 7) + dayOfWeek
                                    if (index < offset || dayCounter > totalDays) {
                                        // Draw blank empty cells
                                        continue
                                    }
                                    
                                    val dateStr = String.format("%s-%02d", currentMonth.toString(), dayCounter)
                                    val completionsCount = state.completionDensity[dateStr] ?: 0

                                    // Heatmap Density Color mapping
                                    val cellColor = when (completionsCount) {
                                        0 -> cellEmptyColor
                                        1 -> SuccessGreen.copy(alpha = 0.3f)
                                        2 -> SuccessGreen.copy(alpha = 0.6f)
                                        else -> SuccessGreen // 3+ completions is full bright green
                                    }

                                    val x = dayOfWeek * (boxSize + spacing)
                                    val y = week * (boxSize + 8.dp.toPx())

                                    // Draw rounded cell rect
                                    drawRoundRect(
                                        color = cellColor,
                                        topLeft = Offset(x, y),
                                        size = Size(boxSize, boxSize),
                                        cornerRadius = CornerRadius(8.dp.toPx())
                                    )

                                    // Subtle border if empty
                                    if (completionsCount == 0) {
                                        drawRoundRect(
                                            color = outlineColor,
                                            topLeft = Offset(x, y),
                                            size = Size(boxSize, boxSize),
                                            cornerRadius = CornerRadius(8.dp.toPx()),
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx())
                                        )
                                    }

                                    // Draw day number text
                                    drawIntoCanvas { canvas ->
                                        val paint = android.graphics.Paint().apply {
                                            color = if (completionsCount > 0) android.graphics.Color.WHITE 
                                                    else android.graphics.Color.parseColor("#475569")
                                            textSize = 10.sp.toPx()
                                            textAlign = android.graphics.Paint.Align.CENTER
                                            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                                        }
                                        // Center text offset adjustments
                                        val fontMetrics = paint.fontMetrics
                                        val textHeight = fontMetrics.descent - fontMetrics.ascent
                                        val textOffset = textHeight / 2 - fontMetrics.descent
                                        
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

                // Individual Streak Percentages List
                Text(
                    text = "INDIVIDUAL STREAK SUCCESS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 8.dp)
                )

                state.summaries.forEach { summary ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(summary.color).copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(summary.emoji, fontSize = 20.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(summary.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                                    Text("🔥 Max streak: ${summary.longestStreak}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = String.format("%.0f%%", summary.completionPercentage),
                                    fontWeight = FontWeight.Black,
                                    color = Color(summary.color),
                                    fontSize = 16.sp
                                )
                                Text("Consistency", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}
