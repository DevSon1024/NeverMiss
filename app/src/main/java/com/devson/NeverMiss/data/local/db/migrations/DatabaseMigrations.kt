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

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. Update streaks table with new columns
            db.execSQL("ALTER TABLE `streaks` ADD COLUMN `icon` TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE `streaks` ADD COLUMN `startDate` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `streaks` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("UPDATE `streaks` SET `startDate` = `creationDate` WHERE `startDate` = ''")
            db.execSQL("UPDATE `streaks` SET `updatedAt` = `nextReminderTimeMillis` WHERE `updatedAt` = 0")

            // 2. Ensure unique constraint on completion_history (streakId, date)
            db.execSQL(
                """
                DELETE FROM `completion_history` WHERE `id` NOT IN (
                    SELECT MAX(`id`) FROM `completion_history` GROUP BY `streakId`, `date`
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_completion_history_streakId_date` ON `completion_history` (`streakId`, `date`)"
            )

            // 3. Update tracked_apps table with pausing, archiving, and updatedAt
            db.execSQL("ALTER TABLE `tracked_apps` ADD COLUMN `isPaused` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `tracked_apps` ADD COLUMN `isArchived` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `tracked_apps` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("UPDATE `tracked_apps` SET `updatedAt` = `trackingCreatedAt` WHERE `updatedAt` = 0")

            // 4. Update tracked_accounts table with pausing, archiving, and updatedAt
            db.execSQL("ALTER TABLE `tracked_accounts` ADD COLUMN `isPaused` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `tracked_accounts` ADD COLUMN `isArchived` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `tracked_accounts` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("UPDATE `tracked_accounts` SET `updatedAt` = `createdAt` WHERE `updatedAt` = 0")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Safely convert any historical FROZEN completion records to SKIPPED
            db.execSQL("UPDATE `completion_history` SET `status` = 'SKIPPED' WHERE `status` = 'FROZEN'")
        }
    }
}
