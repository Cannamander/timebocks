package com.timebox.app.di

import android.content.Context
import com.timebox.app.data.db.AppDatabase
import com.timebox.app.data.db.AppLimitDao
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
}
