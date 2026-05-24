package com.devson.NeverMiss.core.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.devson.NeverMiss.R
import com.devson.NeverMiss.domain.model.StreakCalculator
import com.devson.NeverMiss.domain.repository.StreakRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class StreakWidgetProvider : AppWidgetProvider() {

    @Inject
    lateinit var repository: StreakRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val ACTION_UPDATE_WIDGETS = "com.devson.NeverMiss.ACTION_UPDATE_WIDGETS"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_UPDATE_WIDGETS) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, StreakWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(componentName)
            updateWidgets(context, appWidgetManager, ids)
        }
    }

    private fun updateWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        scope.launch {
            try {
                val activeStreaks = repository.getActiveStreaks()
                val today = com.devson.NeverMiss.core.util.AppClock.now()
                val todayStr = StreakCalculator.formatDate(today)

                val maxStreak = activeStreaks.maxOfOrNull { it.currentStreak } ?: 0
                
                var pendingCount = 0
                for (streak in activeStreaks) {
                    val isScheduled = StreakCalculator.isScheduled(streak, today)
                    val isCompleted = streak.lastCompletedDate == todayStr
                    if (isScheduled && !isCompleted && !streak.isPaused) {
                        pendingCount++
                    }
                }

                val pendingText = if (pendingCount == 0) {
                    "All caught up today! 🎉"
                } else {
                    "$pendingCount streaks pending today"
                }

                appWidgetIds.forEach { id ->
                    val views = RemoteViews(context.packageName, R.layout.widget_layout)
                    views.setTextViewText(R.id.widget_streak_count, maxStreak.toString())
                    views.setTextViewText(R.id.widget_pending_text, pendingText)
                    appWidgetManager.updateAppWidget(id, views)
                }
            } catch (e: Exception) {
                // Fail-safe
            }
        }
    }
}
