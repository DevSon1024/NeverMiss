package com.devson.NeverMiss.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.devson.NeverMiss.domain.model.StreakCategory

fun getCategoryIcon(category: StreakCategory): ImageVector {
    return when (category) {
        StreakCategory.HEALTH -> Icons.Default.Favorite
        StreakCategory.FITNESS -> Icons.Default.FitnessCenter
        StreakCategory.ROUTINE -> Icons.Default.Sync
        StreakCategory.STUDY -> Icons.Default.Computer
        StreakCategory.DUOLINGO -> Icons.Default.Translate
        StreakCategory.SOCIAL -> Icons.Default.Forum
        StreakCategory.REWARDS -> Icons.Default.EmojiEvents
        StreakCategory.PLANTS -> Icons.Default.LocalFlorist
        StreakCategory.CUSTOM -> Icons.Default.Extension
    }
}
