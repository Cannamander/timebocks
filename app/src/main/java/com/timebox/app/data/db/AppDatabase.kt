package com.timebox.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.timebox.app.data.model.AppLimit
import com.timebox.app.data.model.AchievementRecord
import com.timebox.app.data.model.BocksLedger
import com.timebox.app.data.model.ExtensionLog
import com.timebox.app.data.model.StreakRecord
import com.timebox.app.data.model.UsageLog

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE app_limits ADD COLUMN window_start_epoch_ms INTEGER NOT NULL DEFAULT 0"
        )
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS extension_log (
              packageName TEXT NOT NULL,
              date TEXT NOT NULL,
              extensionCount INTEGER NOT NULL,
              PRIMARY KEY(packageName, date)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS bocks_ledger (
              id INTEGER NOT NULL,
              balance INTEGER NOT NULL,
              lifetimeEarned INTEGER NOT NULL,
              PRIMARY KEY(id)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS streak_record (
              id INTEGER NOT NULL,
              currentStreak INTEGER NOT NULL,
              longestStreak INTEGER NOT NULL,
              lastCleanDate TEXT NOT NULL,
              PRIMARY KEY(id)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS achievement_record (
              achievementId TEXT NOT NULL,
              unlockedDate TEXT NOT NULL,
              seen INTEGER NOT NULL,
              PRIMARY KEY(achievementId)
            )
            """.trimIndent()
        )
    }
}

@Database(
    entities = [
        AppLimit::class,
        UsageLog::class,
        ExtensionLog::class,
        BocksLedger::class,
        StreakRecord::class,
        AchievementRecord::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appLimitDao(): AppLimitDao
    abstract fun usageLogDao(): UsageLogDao
    abstract fun extensionLogDao(): ExtensionLogDao
    abstract fun bocksLedgerDao(): BocksLedgerDao
    abstract fun streakRecordDao(): StreakRecordDao
    abstract fun achievementRecordDao(): AchievementRecordDao

    companion object {
        private const val DB_NAME = "timebox.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build().also { INSTANCE = it }
            }
        }
    }
}
