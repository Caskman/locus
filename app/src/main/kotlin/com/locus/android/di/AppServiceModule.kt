package com.locus.android.di

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.locus.android.services.TrackerService
import com.locus.android.work.WatchdogWorker
import com.locus.core.domain.usecase.TrackingManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppServiceModule {

    @Provides
    @Singleton
    fun provideTrackingManager(@ApplicationContext context: Context): TrackingManager {
        return object : TrackingManager {
            override fun startTracking() {
                TrackerService.start(context)
            }

            override fun scheduleWatchdog() {
                val request = PeriodicWorkRequestBuilder<WatchdogWorker>(15, TimeUnit.MINUTES)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    "WatchdogWork",
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
            }
        }
    }
}
