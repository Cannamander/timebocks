package com.timebox.app.di

import android.content.Context
import com.timebox.app.data.db.AppDatabase
import com.timebox.app.data.db.AppLimitDao
import com.timebox.app.data.db.AchievementRecordDao
import com.timebox.app.data.db.BocksLedgerDao
import com.timebox.app.data.db.ExtensionLogDao
import com.timebox.app.data.db.StreakRecordDao
import com.timebox.app.data.db.UsageLogDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getInstance(context)

    @Provides
    @Singleton
    fun provideAppLimitDao(db: AppDatabase): AppLimitDao = db.appLimitDao()

    @Provides
    @Singleton
    fun provideUsageLogDao(db: AppDatabase): UsageLogDao = db.usageLogDao()

    @Provides
    @Singleton
    fun provideExtensionLogDao(db: AppDatabase): ExtensionLogDao = db.extensionLogDao()

    @Provides
    @Singleton
    fun provideBocksLedgerDao(db: AppDatabase): BocksLedgerDao = db.bocksLedgerDao()

    @Provides
    @Singleton
    fun provideStreakRecordDao(db: AppDatabase): StreakRecordDao = db.streakRecordDao()

    @Provides
    @Singleton
    fun provideAchievementRecordDao(db: AppDatabase): AchievementRecordDao = db.achievementRecordDao()
}
