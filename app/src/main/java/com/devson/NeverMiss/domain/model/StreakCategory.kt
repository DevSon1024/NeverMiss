package com.devson.NeverMiss.domain.model

enum class StreakCategory(val displayName: String, val defaultEmoji: String) {
    HEALTH("Health/Medicines", "💊"),
    FITNESS("Fitness/Gym", "💪"),
    ROUTINE("Routines/Backup", "🔄"),
    STUDY("Study/Coding", "💻"),
    DUOLINGO("Language/Duolingo", "🦉"),
    SOCIAL("Social/Reddit", "🤖"),
    REWARDS("Rewards/Edge", "🏆"),
    PLANTS("Plants/Gardening", "🪴"),
    CUSTOM("Custom/Other", "✨");

    companion object {
        fun fromName(name: String): StreakCategory {
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: CUSTOM
        }
    }
}
