package com.devson.NeverMiss.data.local.db

import androidx.sqlite.db.SupportSQLiteDatabase
import com.devson.NeverMiss.data.local.db.migrations.DatabaseMigrations
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class DatabaseMigrationTest {

    @Test
    fun testMigration1To2_executesAllRequiredTableCreations() {
        val mockDb = mockk<SupportSQLiteDatabase>(relaxed = true)

        DatabaseMigrations.MIGRATION_1_2.migrate(mockDb)

        assertEquals(1, DatabaseMigrations.MIGRATION_1_2.startVersion)
        assertEquals(2, DatabaseMigrations.MIGRATION_1_2.endVersion)

        // Verify tracked_apps table and index
        verify {
            mockDb.execSQL(match { it.contains("CREATE TABLE IF NOT EXISTS `tracked_apps`") })
            mockDb.execSQL(match { it.contains("CREATE UNIQUE INDEX IF NOT EXISTS `index_tracked_apps_packageName`") })
        }

        // Verify tracked_accounts table and indices
        verify {
            mockDb.execSQL(match { it.contains("CREATE TABLE IF NOT EXISTS `tracked_accounts`") })
            mockDb.execSQL(match { it.contains("CREATE INDEX IF NOT EXISTS `index_tracked_accounts_trackedAppId`") })
            mockDb.execSQL(match { it.contains("CREATE UNIQUE INDEX IF NOT EXISTS `index_tracked_accounts_trackedAppId_accountName`") })
        }

        // Verify app_streak_completions table and indices
        verify {
            mockDb.execSQL(match { it.contains("CREATE TABLE IF NOT EXISTS `app_streak_completions`") })
            mockDb.execSQL(match { it.contains("CREATE INDEX IF NOT EXISTS `index_app_streak_completions_trackedAppId`") })
            mockDb.execSQL(match { it.contains("CREATE INDEX IF NOT EXISTS `index_app_streak_completions_accountId`") })
            mockDb.execSQL(match { it.contains("CREATE INDEX IF NOT EXISTS `index_app_streak_completions_completionDate`") })
        }

        // Verify usage_tracking_checkpoints table and index
        verify {
            mockDb.execSQL(match { it.contains("CREATE TABLE IF NOT EXISTS `usage_tracking_checkpoints`") })
            mockDb.execSQL(match { it.contains("CREATE UNIQUE INDEX IF NOT EXISTS `index_usage_tracking_checkpoints_sourceIdentifier`") })
        }

        // Verify pending_account_confirmations table and index
        verify {
            mockDb.execSQL(match { it.contains("CREATE TABLE IF NOT EXISTS `pending_account_confirmations`") })
            mockDb.execSQL(match { it.contains("CREATE INDEX IF NOT EXISTS `index_pending_account_confirmations_trackedAppId`") })
        }
    }
}

