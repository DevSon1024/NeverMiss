package com.devson.NeverMiss.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

data class StreakIconItem(
    val key: String,
    val label: String,
    val icon: ImageVector
)

object StreakIconHelper {
    val icons: List<StreakIconItem> = listOf(
        StreakIconItem("auto_awesome", "Habit", Icons.Default.AutoAwesome),
        StreakIconItem("star", "Star", Icons.Default.Star),
        StreakIconItem("fitness_center", "Fitness", Icons.Default.FitnessCenter),
        StreakIconItem("directions_run", "Running", Icons.AutoMirrored.Filled.DirectionsRun),
        StreakIconItem("directions_walk", "Walking", Icons.AutoMirrored.Filled.DirectionsWalk),
        StreakIconItem("self_improvement", "Meditation", Icons.Default.SelfImprovement),
        StreakIconItem("water_drop", "Water", Icons.Default.WaterDrop),
        StreakIconItem("menu_book", "Reading", Icons.AutoMirrored.Filled.MenuBook),
        StreakIconItem("code", "Coding", Icons.Default.Code),
        StreakIconItem("bedtime", "Sleep", Icons.Default.Bedtime),
        StreakIconItem("wb_sunny", "Morning", Icons.Default.WbSunny),
        StreakIconItem("restaurant", "Nutrition", Icons.Default.Restaurant),
        StreakIconItem("local_florist", "Plant", Icons.Default.LocalFlorist),
        StreakIconItem("favorite", "Health", Icons.Default.Favorite),
        StreakIconItem("timer", "Focus", Icons.Default.Timer),
        StreakIconItem("music_note", "Music", Icons.Default.MusicNote),
        StreakIconItem("attach_money", "Finance", Icons.Default.AttachMoney),
        StreakIconItem("school", "Study", Icons.Default.School),
        StreakIconItem("work", "Work", Icons.Default.Work),
        StreakIconItem("emoji_events", "Trophy", Icons.Default.EmojiEvents),
        StreakIconItem("lightbulb", "Idea", Icons.Default.Lightbulb),
        StreakIconItem("brush", "Art", Icons.Default.Brush),
        StreakIconItem("sports_esports", "Gaming", Icons.Default.SportsEsports),
        StreakIconItem("cleaning_services", "Chore", Icons.Default.CleaningServices)
    )

    fun getIcon(key: String?): ImageVector {
        if (key.isNullOrBlank()) return Icons.Default.AutoAwesome
        return icons.find { it.key.equals(key, ignoreCase = true) }?.icon ?: Icons.Default.AutoAwesome
    }
}
