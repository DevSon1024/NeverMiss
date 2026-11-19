package com.devson.NeverMiss.data.local.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. Create tracked_apps
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `tracked_apps` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `packageName` TEXT NOT NULL,
                    `appName` TEXT NOT NULL,
                    `isEnabled` INTEGER NOT NULL,
                    `trackingCreatedAt` INTEGER NOT NULL,
                    `currentStreak` INTEGER NOT NULL,
                    `longestStreak` INTEGER NOT NULL,
                    `lastCompletedDate` TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_tracked_apps_packageName` ON `tracked_apps` (`packageName`)"
            )

            // 2. Create tracked_accounts
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `tracked_accounts` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `trackedAppId` INTEGER NOT NULL,
                    `accountName` TEXT NOT NULL,
                    `isEnabled` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `currentStreak` INTEGER NOT NULL,
                    `longestStreak` INTEGER NOT NULL,
                    `lastCompletedDate` TEXT NOT NULL,
                    FOREIGN KEY(`trackedAppId`) REFERENCES `tracked_apps`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_tracked_accounts_trackedAppId` ON `tracked_accounts` (`trackedAppId`)"
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_tracked_accounts_trackedAppId_accountName` ON `tracked_accounts` (`trackedAppId`, `accountName`)"
            )

            // 3. Create app_streak_completions
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `app_streak_completions` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `trackedAppId` INTEGER NOT NULL,
                    `accountId` INTEGER,
                    `completionDate` TEXT NOT NULL,
                    `detectedAt` INTEGER NOT NULL,
                    `completionMethod` TEXT NOT NULL,
                    `confirmationStatus` TEXT NOT NULL,
                    FOREIGN KEY(`trackedAppId`) REFERENCES `tracked_apps`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`accountId`) REFERENCES `tracked_accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_app_streak_completions_trackedAppId` ON `app_streak_completions` (`trackedAppId`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_app_streak_completions_accountId` ON `app_streak_completions` (`accountId`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_app_streak_completions_completionDate` ON `app_streak_completions` (`completionDate`)"
            )

            // 4. Create usage_tracking_checkpoints
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `usage_tracking_checkpoints` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `sourceIdentifier` TEXT NOT NULL,
                    `lastProcessedTimestamp` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_usage_tracking_checkpoints_sourceIdentifier` ON `usage_tracking_checkpoints` (`sourceIdentifier`)"
            )

            // 5. Create pending_account_confirmations
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `pending_account_confirmations` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `trackedAppId` INTEGER NOT NULL,
                    `sessionTimestamp` INTEGER NOT NULL,
                    `sessionDate` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `expiresAt` INTEGER NOT NULL,
                    FOREIGN KEY(`trackedAppId`) REFERENCES `tracked_apps`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_pending_account_confirmations_trackedAppId` ON `pending_account_confirmations` (`trackedAppId`)"
            )
        }
    }
}

